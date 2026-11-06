package com.example.firelauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "firelauncher_prefs")

/**
 * Manages persisting favorite app package names using AndroidX DataStore Preferences.
 */
class FavoritesStore(private val context: Context) {

    companion object {
        private val KEY_FAVORITES = stringSetPreferencesKey("favorite_packages_set")
        private val KEY_WEATHER_CITY = androidx.datastore.preferences.core.stringPreferencesKey("weather_selected_city")
        private val KEY_WEATHER_CELSIUS = androidx.datastore.preferences.core.booleanPreferencesKey("weather_is_celsius")
    }

    val selectedCityFlow: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_WEATHER_CITY] ?: "Seattle" }

    val isCelsiusFlow: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_WEATHER_CELSIUS] ?: false }

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
