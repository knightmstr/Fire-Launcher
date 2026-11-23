package com.knightmstr.onyxtv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "onyx_prefs")

/** Weather location as persisted in DataStore. */
data class SelectedLocation(
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Single DataStore-backed store for every persisted launcher preference:
 * favourites (ordered), hidden apps, custom labels, custom app order,
 * Kids mode, sort order, usage stats, weather location/units and [LauncherSettings].
 */
class LauncherPrefsStore(private val context: Context) {

    companion object {
        // Legacy unordered favourites set (pre-reordering). Read once for migration, then removed.
        private val KEY_FAVORITES_LEGACY = stringSetPreferencesKey("favorite_packages_set")
        private val KEY_FAVORITES_ORDER = stringPreferencesKey("favorite_packages_order")
        private val KEY_HIDDEN_APPS = stringSetPreferencesKey("hidden_packages_set")
        private val KEY_CUSTOM_LABELS = stringSetPreferencesKey("custom_labels_set")

        private val KEY_WEATHER_CITY = stringPreferencesKey("weather_selected_city")
        private val KEY_WEATHER_COUNTRY = stringPreferencesKey("weather_selected_country")
        private val KEY_WEATHER_LAT = doublePreferencesKey("weather_selected_lat")
        private val KEY_WEATHER_LON = doublePreferencesKey("weather_selected_lon")
        private val KEY_WEATHER_CELSIUS = booleanPreferencesKey("weather_is_celsius")
        private val KEY_SAVED_CITIES = stringSetPreferencesKey("weather_saved_cities_set")

        private val KEY_APP_SORT_ORDER = stringPreferencesKey("app_sort_order")
        private val KEY_APP_USAGE_STATS = stringSetPreferencesKey("app_usage_stats_set")

        private val KEY_WALLPAPER_INTERVAL = intPreferencesKey("settings_wallpaper_interval_min")
        private val KEY_SCREENSAVER_TIMEOUT = intPreferencesKey("settings_screensaver_timeout_min")
        private val KEY_SHOW_HIDDEN = booleanPreferencesKey("settings_show_hidden_apps")
        private val KEY_WALLPAPER_SOURCE = stringPreferencesKey("settings_wallpaper_source")
        private val KEY_CARD_SIZE = stringPreferencesKey("settings_card_size") // legacy name, read once
        private val KEY_TILE_WIDTH = intPreferencesKey("settings_tile_width_dp")
        private val KEY_CARD_SHAPE = stringPreferencesKey("settings_card_shape")
        private val KEY_HOME_LAYOUT = stringPreferencesKey("settings_home_layout")
        private val KEY_SHOW_ALL_APPS_ROW = booleanPreferencesKey("settings_show_all_apps_row")
        private val KEY_NIGHT_DIM = booleanPreferencesKey("settings_screensaver_night_dim")
        private val KEY_UPDATE_CHANNEL = stringPreferencesKey("settings_update_channel")
        private val KEY_WALLPAPER_FIT = stringPreferencesKey("settings_wallpaper_fit")

        private val KEY_CUSTOM_APP_ORDER = stringPreferencesKey("custom_app_order")
        private val KEY_CUSTOM_WALLPAPER_URLS = stringPreferencesKey("wallpaper_custom_urls")

        /** Most custom wallpaper URLs the user can store. */
        const val MAX_CUSTOM_WALLPAPER_URLS = 50

        private val KEY_KIDS_ENABLED = booleanPreferencesKey("kids_mode_enabled")
        private val KEY_KIDS_APPS = stringSetPreferencesKey("kids_allowed_packages")
        private val KEY_KIDS_PIN_HASH = stringPreferencesKey("kids_pin_hash")


        val DEFAULT_CITIES = setOf(
            "Seattle|WA, USA|47.6062|-122.3321",
            "New York|NY, USA|40.7128|-74.0060",
            "Los Angeles|CA, USA|34.0522|-118.2437",
            "London|UK|51.5074|-0.1278",
            "Tokyo|Japan|35.6762|139.6503",
            "Paris|France|48.8566|2.3522",
            "Sydney|Australia|-33.8688|151.2093",
            "Denver|CO, USA|39.7392|-104.9903"
        )

        /** Only the US, Liberia and Myanmar default to Fahrenheit. */
        private val FAHRENHEIT_COUNTRIES = setOf("US", "LR", "MM")

        fun defaultIsCelsius(locale: Locale = Locale.getDefault()): Boolean =
            locale.country.uppercase() !in FAHRENHEIT_COUNTRIES
    }

    /** All preferences, with read errors degraded to defaults instead of crashing the launcher. */
    private val prefs: Flow<Preferences> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    private fun <T> pref(transform: (Preferences) -> T): Flow<T> =
        prefs.map(transform).distinctUntilChanged()

    // ---------------------------------------------------------------- Favourites

    private fun Preferences.favoriteOrder(): List<String> =
        this[KEY_FAVORITES_ORDER]?.let { PrefsCodec.decodeList(it) }
            ?: this[KEY_FAVORITES_LEGACY].orEmpty().sorted()

    /** Favourite package names in the user's chosen order. */
    val favoritesFlow: Flow<List<String>> = pref { it.favoriteOrder() }

    suspend fun toggleFavorite(packageName: String) = editFavorites { PrefsCodec.toggle(it, packageName) }

    suspend fun moveFavorite(packageName: String, delta: Int) =
        editFavorites { PrefsCodec.move(it, packageName, delta) }

    private suspend fun editFavorites(block: (List<String>) -> List<String>) {
        context.dataStore.edit { p ->
            p[KEY_FAVORITES_ORDER] = PrefsCodec.encodeList(block(p.favoriteOrder()))
            p.remove(KEY_FAVORITES_LEGACY)
        }
    }

    // ---------------------------------------------------------------- Hidden apps & labels

    val hiddenAppsFlow: Flow<Set<String>> = pref { it[KEY_HIDDEN_APPS].orEmpty() }

    suspend fun setHidden(packageName: String, hidden: Boolean) {
        context.dataStore.edit { p ->
            val current = p[KEY_HIDDEN_APPS].orEmpty()
            p[KEY_HIDDEN_APPS] = if (hidden) current + packageName else current - packageName
        }
    }

    val customLabelsFlow: Flow<Map<String, String>> =
        pref { PrefsCodec.decodeLabels(it[KEY_CUSTOM_LABELS].orEmpty()) }

    /** Sets a custom label; a blank label restores the app's original name. */
    suspend fun setCustomLabel(packageName: String, label: String?) {
        context.dataStore.edit { p ->
            val remaining = PrefsCodec.withoutLabel(p[KEY_CUSTOM_LABELS].orEmpty(), packageName)
            val clean = label?.let { PrefsCodec.sanitizeLabel(it) }.orEmpty()
            p[KEY_CUSTOM_LABELS] = if (clean.isEmpty()) remaining else remaining + PrefsCodec.encodeLabel(packageName, clean)
        }
    }

    // ---------------------------------------------------------------- Settings

    val settingsFlow: Flow<LauncherSettings> = pref { p ->
        LauncherSettings(
            wallpaperIntervalMinutes = p[KEY_WALLPAPER_INTERVAL] ?: LauncherSettings.DEFAULT_WALLPAPER_INTERVAL,
            screensaverTimeoutMinutes = p[KEY_SCREENSAVER_TIMEOUT] ?: LauncherSettings.DEFAULT_SCREENSAVER_TIMEOUT,
            showHiddenApps = p[KEY_SHOW_HIDDEN] ?: false,
            wallpaperSource = WallpaperSource.fromName(p[KEY_WALLPAPER_SOURCE]),
            tileWidthDp = p[KEY_TILE_WIDTH]?.let { LauncherSettings.clampTileWidth(it) }
                ?: LauncherSettings.tileWidthFromLegacySize(p[KEY_CARD_SIZE]),
            cardShape = CardShape.fromName(p[KEY_CARD_SHAPE]),
            showAllAppsRow = p[KEY_SHOW_ALL_APPS_ROW] ?: false,
            nightDim = p[KEY_NIGHT_DIM] ?: true,
            homeLayout = HomeLayout.fromName(p[KEY_HOME_LAYOUT]),
            updateChannel = UpdateChannel.fromName(p[KEY_UPDATE_CHANNEL]),
            wallpaperFit = WallpaperFit.fromName(p[KEY_WALLPAPER_FIT])
        )
    }

    suspend fun updateSettings(settings: LauncherSettings) {
        context.dataStore.edit { p ->
            p[KEY_WALLPAPER_INTERVAL] = settings.wallpaperIntervalMinutes.coerceAtLeast(0)
            p[KEY_SCREENSAVER_TIMEOUT] = settings.screensaverTimeoutMinutes.coerceAtLeast(0)
            p[KEY_SHOW_HIDDEN] = settings.showHiddenApps
            p[KEY_WALLPAPER_SOURCE] = settings.wallpaperSource.name
            p[KEY_TILE_WIDTH] = LauncherSettings.clampTileWidth(settings.tileWidthDp)
            p.remove(KEY_CARD_SIZE)
            p[KEY_CARD_SHAPE] = settings.cardShape.name
            p[KEY_HOME_LAYOUT] = settings.homeLayout.name
            p[KEY_SHOW_ALL_APPS_ROW] = settings.showAllAppsRow
            p[KEY_NIGHT_DIM] = settings.nightDim
            p[KEY_UPDATE_CHANNEL] = settings.updateChannel.name
            p[KEY_WALLPAPER_FIT] = settings.wallpaperFit.name
        }
    }

    // ---------------------------------------------------------------- Custom wallpaper URLs

    /** Image URLs the user added as a wallpaper source, in the order added. */
    val customWallpaperUrlsFlow: Flow<List<String>> =
        pref { PrefsCodec.decodeList(it[KEY_CUSTOM_WALLPAPER_URLS]) }

    /** Appends a (already-validated) URL if not present and under the cap. */
    suspend fun addCustomWallpaperUrl(url: String) {
        val clean = url.trim()
        if (clean.isEmpty()) return
        context.dataStore.edit { p ->
            val current = PrefsCodec.decodeList(p[KEY_CUSTOM_WALLPAPER_URLS])
            if (clean in current || current.size >= MAX_CUSTOM_WALLPAPER_URLS) return@edit
            p[KEY_CUSTOM_WALLPAPER_URLS] = PrefsCodec.encodeList(current + clean)
        }
    }

    suspend fun removeCustomWallpaperUrl(url: String) {
        context.dataStore.edit { p ->
            val current = PrefsCodec.decodeList(p[KEY_CUSTOM_WALLPAPER_URLS])
            p[KEY_CUSTOM_WALLPAPER_URLS] = PrefsCodec.encodeList(current - url)
        }
    }

    // ---------------------------------------------------------------- Custom app order

    val customAppOrderFlow: Flow<List<String>> = pref { PrefsCodec.decodeList(it[KEY_CUSTOM_APP_ORDER]) }

    suspend fun setCustomAppOrder(order: List<String>) {
        context.dataStore.edit { it[KEY_CUSTOM_APP_ORDER] = PrefsCodec.encodeList(order) }
    }

    // ---------------------------------------------------------------- Kids mode

    val kidsModeEnabledFlow: Flow<Boolean> = pref { it[KEY_KIDS_ENABLED] ?: false }
    val kidsAppsFlow: Flow<Set<String>> = pref { it[KEY_KIDS_APPS].orEmpty() }
    val hasKidsPinFlow: Flow<Boolean> = pref { !it[KEY_KIDS_PIN_HASH].isNullOrBlank() }

    suspend fun setKidsModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_KIDS_ENABLED] = enabled }
    }

    suspend fun setKidsAllowed(packageName: String, allowed: Boolean) {
        context.dataStore.edit { p ->
            val current = p[KEY_KIDS_APPS].orEmpty()
            p[KEY_KIDS_APPS] = if (allowed) current + packageName else current - packageName
        }
    }

    suspend fun setKidsPin(pin: String) {
        require(PinHasher.isValidPin(pin))
        context.dataStore.edit { it[KEY_KIDS_PIN_HASH] = PinHasher.hash(pin) }
    }

    suspend fun verifyKidsPin(pin: String): Boolean =
        PinHasher.verify(pin, prefs.first()[KEY_KIDS_PIN_HASH])

    // ---------------------------------------------------------------- Weather

    val selectedLocationFlow: Flow<SelectedLocation> = pref { p ->
        SelectedLocation(
            city = p[KEY_WEATHER_CITY] ?: "Seattle",
            country = p[KEY_WEATHER_COUNTRY] ?: "WA, USA",
            latitude = p[KEY_WEATHER_LAT] ?: 47.6062,
            longitude = p[KEY_WEATHER_LON] ?: -122.3321
        )
    }

    val isCelsiusFlow: Flow<Boolean> = pref { it[KEY_WEATHER_CELSIUS] ?: defaultIsCelsius() }

    val savedCitiesFlow: Flow<List<CityLocationModel>> = pref { p ->
        (p[KEY_SAVED_CITIES] ?: DEFAULT_CITIES).mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size < 4) return@mapNotNull null
            val name = parts[0].trim()
            val country = parts[1].trim()
            val lat = parts[2].toDoubleOrNull() ?: return@mapNotNull null
            val lon = parts[3].toDoubleOrNull() ?: return@mapNotNull null
            if (name.isEmpty() || lat !in -90.0..90.0 || lon !in -180.0..180.0) return@mapNotNull null
            CityLocationModel(
                id = "saved_${name}_$country",
                name = name,
                country = country,
                latitude = lat,
                longitude = lon
            )
        }
    }

    suspend fun setSelectedLocation(city: String, country: String, lat: Double, lon: Double) {
        val safeCity = city.replace("|", "-").trim()
        val safeCountry = country.replace("|", "-").trim()
        val safeLat = if (lat.isFinite() && lat in -90.0..90.0) lat else 0.0
        val safeLon = if (lon.isFinite() && lon in -180.0..180.0) lon else 0.0

        context.dataStore.edit { p ->
            p[KEY_WEATHER_CITY] = safeCity
            p[KEY_WEATHER_COUNTRY] = safeCountry
            p[KEY_WEATHER_LAT] = safeLat
            p[KEY_WEATHER_LON] = safeLon
            p[KEY_SAVED_CITIES] = (p[KEY_SAVED_CITIES] ?: DEFAULT_CITIES) + "$safeCity|$safeCountry|$safeLat|$safeLon"
        }
    }

    suspend fun toggleTemperatureUnit() {
        context.dataStore.edit { p ->
            p[KEY_WEATHER_CELSIUS] = !(p[KEY_WEATHER_CELSIUS] ?: defaultIsCelsius())
        }
    }

    // ---------------------------------------------------------------- Sorting & usage

    val sortOrderFlow: Flow<AppSortOrder> = pref { AppSortOrder.fromName(it[KEY_APP_SORT_ORDER]) }

    suspend fun setSortOrder(order: AppSortOrder) {
        context.dataStore.edit { it[KEY_APP_SORT_ORDER] = order.name }
    }

    val appUsageStatsFlow: Flow<Map<String, AppUsageStat>> = pref { p ->
        p[KEY_APP_USAGE_STATS].orEmpty().mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size < 3) return@mapNotNull null
            AppUsageStat(
                packageName = parts[0],
                launchCount = parts[1].toIntOrNull() ?: 0,
                lastLaunchedTimeMs = parts[2].toLongOrNull() ?: 0L
            )
        }.associateBy { it.packageName }
    }

    suspend fun recordAppLaunch(packageName: String) {
        val safePkg = packageName.replace("|", "_").trim()
        if (safePkg.isBlank()) return

        context.dataStore.edit { p ->
            val current = p[KEY_APP_USAGE_STATS].orEmpty()
            val existing = current.firstOrNull { it.startsWith("$safePkg|") }
            val count = existing?.split("|")?.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            p[KEY_APP_USAGE_STATS] = (current - setOfNotNull(existing)) +
                "$safePkg|${count + 1}|${System.currentTimeMillis()}"
        }
    }
}
