package com.example.firelauncher.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.firelauncher.data.AppModel
import com.example.firelauncher.data.AppRepository
import com.example.firelauncher.data.AppSortOrder
import com.example.firelauncher.data.AppUsageStat
import com.example.firelauncher.data.CityLocationModel
import com.example.firelauncher.data.LauncherPrefsStore
import com.example.firelauncher.data.LauncherSettings
import com.example.firelauncher.data.PrefsCodec
import com.example.firelauncher.data.WallpaperModel
import com.example.firelauncher.data.WallpaperRepository
import com.example.firelauncher.data.WeatherData
import com.example.firelauncher.data.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Main MVVM coordinator. Exposes StateFlows for the app library (sorted, renamed, hidden),
 * ordered favourites, system shortcuts, wallpapers, weather, settings and the idle screensaver.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val prefs = LauncherPrefsStore(application)
    private val weatherRepository = WeatherRepository()

    // ------------------------------------------------------------------ Settings

    val settings: StateFlow<LauncherSettings> = prefs.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, LauncherSettings())

    private val wallpaperRepository = WallpaperRepository(
        scope = viewModelScope,
        rotationIntervalMinutes = settings.map { it.wallpaperIntervalMinutes }.distinctUntilChanged()
    )

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    fun openSettings() { _isSettingsOpen.value = true }
    fun closeSettings() { _isSettingsOpen.value = false }

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        viewModelScope.launch { prefs.updateSettings(transform(settings.value)) }
    }

    // ------------------------------------------------------------------ App library

    val sortOrder: StateFlow<AppSortOrder> = prefs.sortOrderFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSortOrder.ALPHABETICAL)

    private val appUsageStats: StateFlow<Map<String, AppUsageStat>> = prefs.appUsageStatsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val hiddenPackages: StateFlow<Set<String>> = prefs.hiddenAppsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val customLabels: StateFlow<Map<String, String>> = prefs.customLabelsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _rawApps = MutableStateFlow<List<AppModel>>(emptyList())

    /** Every installed app with usage stats, custom labels and hidden state applied (unsorted). */
    private val decoratedApps: StateFlow<List<AppModel>> =
        combine(_rawApps, appUsageStats, hiddenPackages, customLabels) { raw, stats, hidden, labels ->
            raw.map { app ->
                val stat = stats[app.packageName]
                app.copy(
                    label = labels[app.packageName] ?: app.originalLabel,
                    launchCount = stat?.launchCount ?: 0,
                    lastLaunchedTimeMs = stat?.lastLaunchedTimeMs ?: 0L,
                    isHidden = app.packageName in hidden
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Visible apps sorted by the selected order (A–Z, Most Recent, Most Frequent). */
    val apps: StateFlow<List<AppModel>> = combine(decoratedApps, sortOrder) { list, order ->
        val visible = list.filterNot { it.isHidden }
        when (order) {
            AppSortOrder.ALPHABETICAL -> visible.sortedBy { it.label.lowercase() }
            AppSortOrder.MOST_RECENT -> visible.sortedWith(
                compareByDescending<AppModel> { it.lastLaunchedTimeMs }.thenBy { it.label.lowercase() }
            )
            AppSortOrder.MOST_FREQUENT -> visible.sortedWith(
                compareByDescending<AppModel> { it.launchCount }.thenBy { it.label.lowercase() }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hiddenApps: StateFlow<List<AppModel>> = decoratedApps
        .map { list -> list.filter { it.isHidden }.sortedBy { it.label.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val systemShortcuts: StateFlow<List<AppModel>> = MutableStateFlow(repository.getSystemShortcuts())

    // ------------------------------------------------------------------ Favourites

    private val favoriteOrder: StateFlow<List<String>> = prefs.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favorites: StateFlow<Set<String>> = favoriteOrder
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** Favourite apps in the user's chosen order (hidden apps are left out). */
    val favoriteApps: StateFlow<List<AppModel>> = combine(decoratedApps, favoriteOrder) { all, order ->
        PrefsCodec.applyOrder(all.filterNot { it.isHidden }, order) { it.packageName }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun toggleFavorite(app: AppModel) {
        if (app.isSystemShortcut) return
        viewModelScope.launch { prefs.toggleFavorite(app.packageName) }
    }

    /** Moves a favourite left (negative) or right (positive) in the Favourites row. */
    fun moveFavorite(app: AppModel, delta: Int) {
        viewModelScope.launch { prefs.moveFavorite(app.packageName, delta) }
    }

    // ------------------------------------------------------------------ Hide / rename

    fun setHidden(app: AppModel, hidden: Boolean) {
        if (app.isSystemShortcut) return
        viewModelScope.launch { prefs.setHidden(app.packageName, hidden) }
    }

    fun renameApp(app: AppModel, newLabel: String?) {
        if (app.isSystemShortcut) return
        val label = newLabel?.trim()?.takeIf { it.isNotEmpty() && it != app.originalLabel }
        viewModelScope.launch { prefs.setCustomLabel(app.packageName, label) }
    }

    // ------------------------------------------------------------------ Focus, menus & dialogs

    private val allKnownApps = combine(decoratedApps, systemShortcuts) { a, b -> a + b }

    private fun resolve(keyFlow: StateFlow<String?>): StateFlow<AppModel?> =
        combine(keyFlow, allKnownApps) { key, list -> key?.let { k -> list.firstOrNull { it.uniqueKey == k } } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _focusedKey = MutableStateFlow<String?>(null)
    /** Resolved from live data so a rename shows immediately in the hero header. */
    val focusedApp: StateFlow<AppModel?> = resolve(_focusedKey)

    fun setFocusedApp(app: AppModel) { _focusedKey.value = app.uniqueKey }

    private val _contextMenuKey = MutableStateFlow<String?>(null)
    val contextMenuApp: StateFlow<AppModel?> = resolve(_contextMenuKey)

    fun openContextMenu(app: AppModel?) {
        if (app == null || app.isSystemShortcut) return
        _contextMenuKey.value = app.uniqueKey
    }

    /** Fire TV remote ☰ Menu key: open options for whichever card has focus. */
    fun openContextMenuForFocused() = openContextMenu(focusedApp.value)

    fun closeContextMenu() { _contextMenuKey.value = null }

    private val _renameKey = MutableStateFlow<String?>(null)
    val renameTarget: StateFlow<AppModel?> = resolve(_renameKey)

    fun startRename(app: AppModel) { _renameKey.value = app.uniqueKey }
    fun closeRename() { _renameKey.value = null }

    // ------------------------------------------------------------------ Home / voice

    private val _homeTick = MutableStateFlow(0L)
    val homeTick: StateFlow<Long> = _homeTick.asStateFlow()

    private val _voiceSearchTrigger = MutableStateFlow(0L)
    val voiceSearchTrigger: StateFlow<Long> = _voiceSearchTrigger.asStateFlow()

    fun onHomePressed() {
        _homeTick.value += 1L
        closeContextMenu()
        closeRename()
        closeSettings()
        onUserInteraction()
    }

    fun requestVoiceSearch() { _voiceSearchTrigger.value += 1L }

    fun onVoiceResult(query: String) {
        _isSearchActive.value = true
        _searchQuery.value = query
    }

    // ------------------------------------------------------------------ Wallpapers

    val currentWallpaper: StateFlow<WallpaperModel?> = wallpaperRepository.currentWallpaper
    val allWallpapers: StateFlow<List<WallpaperModel>> = wallpaperRepository.wallpapers

    fun nextWallpaper() = wallpaperRepository.nextWallpaper()
    fun setWallpaper(wallpaper: WallpaperModel) = wallpaperRepository.setWallpaper(wallpaper)

    // ------------------------------------------------------------------ Weather

    private val _weather = MutableStateFlow(WeatherData.defaultSeattle(LauncherPrefsStore.defaultIsCelsius()))
    val weather: StateFlow<WeatherData> = _weather.asStateFlow()

    val savedCities: StateFlow<List<CityLocationModel>> = prefs.savedCitiesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isCelsius: StateFlow<Boolean> = prefs.isCelsiusFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, LauncherPrefsStore.defaultIsCelsius())

    private val _searchResults = MutableStateFlow<List<CityLocationModel>>(emptyList())
    val searchResults: StateFlow<List<CityLocationModel>> = _searchResults.asStateFlow()

    private val _isSearchingCities = MutableStateFlow(false)
    val isSearchingCities: StateFlow<Boolean> = _isSearchingCities.asStateFlow()

    private var citySearchJob: Job? = null

    /** Searches cities globally via Open-Meteo Geocoding with a 300 ms debounce. */
    fun searchCities(query: String) {
        citySearchJob?.cancel()
        if (query.trim().length < 2) {
            clearCitySearch()
            return
        }
        citySearchJob = viewModelScope.launch {
            _isSearchingCities.value = true
            delay(300L)
            _searchResults.value = weatherRepository.searchCities(query)
            _isSearchingCities.value = false
        }
    }

    fun clearCitySearch() {
        citySearchJob?.cancel()
        _searchResults.value = emptyList()
        _isSearchingCities.value = false
    }

    fun selectCity(city: CityLocationModel) {
        viewModelScope.launch {
            prefs.setSelectedLocation(
                city = city.name,
                country = city.admin1?.let { "$it, ${city.countryCode ?: city.country}" } ?: city.country,
                lat = city.latitude,
                lon = city.longitude
            )
            clearCitySearch()
        }
    }

    fun toggleTemperatureUnit() {
        viewModelScope.launch { prefs.toggleTemperatureUnit() }
    }

    private suspend fun refreshWeather(lat: Double, lon: Double, city: String, country: String, celsius: Boolean) {
        _weather.value = weatherRepository.fetchLiveWeather(city, country, lat, lon, celsius)
    }

    // ------------------------------------------------------------------ Global search

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    val filteredApps: StateFlow<List<AppModel>> = combine(apps, _searchQuery) { allApps, query ->
        val q = query.trim().lowercase()
        if (q.isEmpty()) allApps
        else allApps.filter { app ->
            app.label.lowercase().contains(q) ||
                app.originalLabel.lowercase().contains(q) ||
                app.packageName.lowercase().contains(q)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    fun setSortOrder(order: AppSortOrder) {
        viewModelScope.launch { prefs.setSortOrder(order) }
    }

    // ------------------------------------------------------------------ Idle screensaver

    private val _screensaverVisible = MutableStateFlow(false)
    val screensaverVisible: StateFlow<Boolean> = _screensaverVisible.asStateFlow()

    private var idleJob: Job? = null
    private var isForeground = false
    private var isDialogOpen = false

    /** Called for every key press; dismisses the screensaver and restarts the idle timer. */
    fun onUserInteraction() {
        _screensaverVisible.value = false
        scheduleIdle()
    }

    fun onForegroundChanged(foreground: Boolean) {
        isForeground = foreground
        _screensaverVisible.value = false
        if (foreground) scheduleIdle() else idleJob?.cancel()
    }

    /** Compose dialogs receive keys in their own window, so the timer is paused while one is open. */
    fun setDialogOpen(open: Boolean) {
        if (isDialogOpen == open) return
        isDialogOpen = open
        scheduleIdle()
    }

    private fun scheduleIdle() {
        idleJob?.cancel()
        val minutes = settings.value.screensaverTimeoutMinutes
        if (!isForeground || isDialogOpen || minutes <= 0) return
        idleJob = viewModelScope.launch {
            delay(minutes * 60_000L)
            _screensaverVisible.value = true
        }
    }

    // ------------------------------------------------------------------ Lifecycle

    init {
        refreshApps()

        // Fetch weather whenever the location or unit changes
        viewModelScope.launch {
            combine(prefs.selectedLocationFlow, prefs.isCelsiusFlow) { loc, celsius -> loc to celsius }
                .collect { (loc, celsius) -> refreshWeather(loc.latitude, loc.longitude, loc.city, loc.country, celsius) }
        }

        // Periodic live weather refresh (every 15 minutes)
        viewModelScope.launch {
            while (isActive) {
                delay(15 * 60 * 1000L)
                val cur = _weather.value
                refreshWeather(cur.latitude, cur.longitude, cur.city, cur.country, cur.isCelsius)
            }
        }

        // Re-arm the screensaver timer when its timeout setting changes
        viewModelScope.launch {
            settings.map { it.screensaverTimeoutMinutes }.distinctUntilChanged().collect { scheduleIdle() }
        }
    }

    /** Re-scans installed apps from PackageManager. */
    fun refreshApps() {
        viewModelScope.launch {
            val list = repository.getInstalledApps()
            _rawApps.value = list
            if (_focusedKey.value == null) {
                list.firstOrNull()?.let { _focusedKey.value = it.uniqueKey }
            }
        }
    }

    fun launchApp(app: AppModel): Boolean {
        if (app.packageName == AppRepository.LAUNCHER_SETTINGS_KEY) {
            openSettings()
            return true
        }
        if (!app.isSystemShortcut) {
            viewModelScope.launch { prefs.recordAppLaunch(app.packageName) }
        }
        return repository.launchApp(app)
    }

    /** Opens Fire OS App Info (Force Stop, Clear Cache/Data, permissions) for [app]. */
    fun openAppDetails(app: AppModel) {
        if (app.isSystemShortcut) return
        val context = getApplication<Application>()
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val fallback = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(details) }
            .recoverCatching { context.startActivity(fallback) }
    }
}
