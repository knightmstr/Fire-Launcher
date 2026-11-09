package com.example.firelauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "firelauncher_prefs")

/**
 * Manages persisting favorite apps, weather location, and user preferences using AndroidX DataStore.
 */
class FavoritesStore(private val context: Context) {

    companion object {
        private val KEY_FAVORITES = stringSetPreferencesKey("favorite_packages_set")
        private val KEY_WEATHER_CITY = stringPreferencesKey("weather_selected_city")
        private val KEY_WEATHER_COUNTRY = stringPreferencesKey("weather_selected_country")
        private val KEY_WEATHER_LAT = doublePreferencesKey("weather_selected_lat")
        private val KEY_WEATHER_LON = doublePreferencesKey("weather_selected_lon")
        private val KEY_WEATHER_CELSIUS = booleanPreferencesKey("weather_is_celsius")
        private val KEY_SAVED_CITIES = stringSetPreferencesKey("weather_saved_cities_set")

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
    }

    val selectedCityFlow: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_WEATHER_CITY] ?: "Seattle" }

    val selectedCountryFlow: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_WEATHER_COUNTRY] ?: "WA, USA" }

    val selectedLatFlow: Flow<Double> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_WEATHER_LAT] ?: 47.6062 }

    val selectedLonFlow: Flow<Double> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_WEATHER_LON] ?: -122.3321 }

    val isCelsiusFlow: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_WEATHER_CELSIUS] ?: false }

    val savedCitiesFlow: Flow<List<CityLocationModel>> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val set = prefs[KEY_SAVED_CITIES] ?: DEFAULT_CITIES
            set.mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size >= 4) {
                    val name = parts[0]
                    val country = parts[1]
                    val lat = parts[2].toDoubleOrNull() ?: 0.0
                    val lon = parts[3].toDoubleOrNull() ?: 0.0
                    CityLocationModel(
                        id = "saved_${name}_$country",
                        name = name,
                        country = country,
                        latitude = lat,
                        longitude = lon
                    )
                } else null
            }
        }

    suspend fun setSelectedLocation(city: String, country: String, lat: Double, lon: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WEATHER_CITY] = city
            preferences[KEY_WEATHER_COUNTRY] = country
            preferences[KEY_WEATHER_LAT] = lat
            preferences[KEY_WEATHER_LON] = lon

            // Also add to saved cities set
            val currentSet = (preferences[KEY_SAVED_CITIES] ?: DEFAULT_CITIES).toMutableSet()
            currentSet.add("$city|$country|$lat|$lon")
            preferences[KEY_SAVED_CITIES] = currentSet
        }
    }

    suspend fun setSelectedCity(city: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WEATHER_CITY] = city
        }
    }

    suspend fun toggleTemperatureUnit() {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_WEATHER_CELSIUS] ?: false
            preferences[KEY_WEATHER_CELSIUS] = !current
        }
    }

    val favoritesFlow: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_FAVORITES] ?: emptySet()
        }

    suspend fun toggleFavorite(packageName: String) {
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[KEY_FAVORITES]?.toMutableSet() ?: mutableSetOf()
            if (currentFavorites.contains(packageName)) {
                currentFavorites.remove(packageName)
            } else {
                currentFavorites.add(packageName)
            }
            preferences[KEY_FAVORITES] = currentFavorites
        }
    }

    suspend fun setFavorites(packageNames: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FAVORITES] = packageNames
        }
    }
}
