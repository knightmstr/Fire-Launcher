package com.example.firelauncher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.firelauncher.data.AppModel
import com.example.firelauncher.data.AppRepository
import com.example.firelauncher.data.FavoritesStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Main ViewModel for FireLauncher adhering to MVVM architecture.
 * Exposes StateFlows for apps, favorites, system shortcuts, and home-button tick events.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val favoritesStore = FavoritesStore(application)

    private val _apps = MutableStateFlow<List<AppModel>>(emptyList())
    val apps: StateFlow<List<AppModel>> = _apps.asStateFlow()

    val favorites: StateFlow<Set<String>> = favoritesStore.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val systemShortcuts: StateFlow<List<AppModel>> = MutableStateFlow(repository.getSystemShortcuts())

    // Combined favorites app list
    val favoriteApps: StateFlow<List<AppModel>> = combine(_apps, favorites) { allApps, favSet ->
        allApps.filter { favSet.contains(it.packageName) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _focusedApp = MutableStateFlow<AppModel?>(null)
    val focusedApp: StateFlow<AppModel?> = _focusedApp.asStateFlow()

    // Incremented whenever Home is pressed while launcher is active
    private val _homeTick = MutableStateFlow(0L)
    val homeTick: StateFlow<Long> = _homeTick.asStateFlow()

    // Weather StateFlow derived from DataStore selected city and unit
    val weather: StateFlow<com.example.firelauncher.data.WeatherData> = combine(
        favoritesStore.selectedCityFlow,
        favoritesStore.isCelsiusFlow
    ) { city, isCelsius ->
        com.example.firelauncher.data.WeatherData.forCity(city, isCelsius)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        com.example.firelauncher.data.WeatherData.forCity("Seattle", false)
    )

    val availableCities: List<String> = com.example.firelauncher.data.WeatherData.CITY_PRESETS.map { it.city }

    // Global Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    // Apps dynamically filtered by the global search query
    val filteredApps: StateFlow<List<AppModel>> = combine(_apps, _searchQuery) { allApps, query ->
        if (query.isBlank()) {
            allApps
        } else {
            val trimmed = query.trim().lowercase()
            allApps.filter { app ->
                app.label.lowercase().contains(trimmed) || app.packageName.lowercase().contains(trimmed)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshApps()
    }

    /**
     * Refreshes the list of installed applications from PackageManager.
     * Invoked initially and on every onResume of the Activity.
     */
    fun refreshApps() {
        viewModelScope.launch {
            val list = repository.getInstalledApps()
            _apps.value = list
            if (_focusedApp.value == null && list.isNotEmpty()) {
                _focusedApp.value = list.first()
            }
        }
    }

    /**
     * Called when the Home button is pressed while the launcher is open.
     * Triggers the homeTick counter to notify UI to scroll to top and refocus the first card.
     */
    fun onHomePressed() {
        _homeTick.value += 1L
    }

    /**
     * Updates the currently focused app (for hero text and dynamic background color).
     */
    fun setFocusedApp(app: AppModel) {
        _focusedApp.value = app
    }

    /**
     * Toggles the favorite status of the given app and saves to DataStore.
     */
    fun toggleFavorite(app: AppModel) {
        if (app.isSystemShortcut) return
        viewModelScope.launch {
            favoritesStore.toggleFavorite(app.packageName)
        }
    }

    /**
     * Updates the selected city for the top weather widget.
     */
    fun selectCity(city: String) {
        viewModelScope.launch {
            favoritesStore.setSelectedCity(city)
        }
    }

    /**
     * Toggles between Fahrenheit and Celsius.
     */
    fun toggleTemperatureUnit() {
        viewModelScope.launch {
            favoritesStore.toggleTemperatureUnit()
        }
    }

    /**
     * Updates the active global search query string.
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Activates or closes the global search bar.
     */
    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    /**
     * Clears the search query while keeping search active.
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * Launches the specified application or system settings shortcut.
     */
    fun launchApp(app: AppModel): Boolean {
        return repository.launchApp(app)
    }
}
