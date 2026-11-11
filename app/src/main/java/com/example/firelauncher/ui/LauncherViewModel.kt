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
import com.example.firelauncher.data.FavoritesStore
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Main ViewModel for FireLauncher adhering to MVVM architecture.
 * Exposes StateFlows for apps, sort order, favorites, system shortcuts, home-button tick events,
 * periodic r/TVWallpapersSFW wallpapers (changing every 30 mins), and accurate live Open-Meteo weather with city search.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val favoritesStore = FavoritesStore(application)
    private val wallpaperRepository = WallpaperRepository(application, viewModelScope)
    private val weatherRepository = WeatherRepository()

    // App Library Sorting Order & Usage Tracking
    val sortOrder: StateFlow<AppSortOrder> = favoritesStore.sortOrderFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSortOrder.ALPHABETICAL)

    val appUsageStats: StateFlow<Map<String, AppUsageStat>> = favoritesStore.appUsageStatsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _rawApps = MutableStateFlow<List<AppModel>>(emptyList())

    // Dynamically sorted app library based on selected sort order (A-Z, Most Recent, Most Frequent)
    val apps: StateFlow<List<AppModel>> = combine(_rawApps, sortOrder, appUsageStats) { rawList, order, statsMap ->
        val enriched = rawList.map { app ->
            val stat = statsMap[app.packageName]
            if (stat != null) {
                app.copy(launchCount = stat.launchCount, lastLaunchedTimeMs = stat.lastLaunchedTimeMs)
            } else {
                app
            }
        }
        when (order) {
            AppSortOrder.ALPHABETICAL -> enriched.sortedBy { it.label.lowercase() }
            AppSortOrder.MOST_RECENT -> enriched.sortedWith(
                compareByDescending<AppModel> { it.lastLaunchedTimeMs }
                    .thenBy { it.label.lowercase() }
            )
            AppSortOrder.MOST_FREQUENT -> enriched.sortedWith(
                compareByDescending<AppModel> { it.launchCount }
                    .thenBy { it.label.lowercase() }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    // r/TVWallpapersSFW 30-minute auto-changing wallpaper
    val currentWallpaper: StateFlow<WallpaperModel?> = wallpaperRepository.currentWallpaper
    val allWallpapers: StateFlow<List<WallpaperModel>> = wallpaperRepository.wallpapers

    // Live Weather State
    private val _weather = MutableStateFlow(WeatherData.defaultSeattle())
    val weather: StateFlow<WeatherData> = _weather.asStateFlow()

    val savedCities: StateFlow<List<CityLocationModel>> = favoritesStore.savedCitiesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isCelsius: StateFlow<Boolean> = favoritesStore.isCelsiusFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Live City Geocoding Search State
    private val _searchResults = MutableStateFlow<List<CityLocationModel>>(emptyList())
    val searchResults: StateFlow<List<CityLocationModel>> = _searchResults.asStateFlow()

    private val _isSearchingCities = MutableStateFlow(false)
    val isSearchingCities: StateFlow<Boolean> = _isSearchingCities.asStateFlow()

    private var citySearchJob: Job? = null

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

        // Observe location & unit changes to fetch live weather
        viewModelScope.launch {
            combine(
                favoritesStore.selectedCityFlow,
                favoritesStore.selectedCountryFlow,
                favoritesStore.selectedLatFlow,
                favoritesStore.selectedLonFlow,
                favoritesStore.isCelsiusFlow
            ) { city, country, lat, lon, celsius ->
                fetchCurrentWeather(city, country, lat, lon, celsius)
            }.collect {}
        }

        // Periodic live weather refresh (every 15 minutes)
        viewModelScope.launch {
            while (isActive) {
                delay(15 * 60 * 1000L)
                val cur = _weather.value
                fetchCurrentWeather(cur.city, cur.country, cur.latitude, cur.longitude, cur.isCelsius)
            }
        }
    }

    private suspend fun fetchCurrentWeather(city: String, country: String, lat: Double, lon: Double, celsius: Boolean) {
        val result = weatherRepository.fetchLiveWeather(city, country, lat, lon, celsius)
        _weather.value = result
    }

    /**
     * Refreshes the list of installed applications from PackageManager.
     */
    fun refreshApps() {
        viewModelScope.launch {
            val list = repository.getInstalledApps()
            _rawApps.value = list
            if (_focusedApp.value == null && list.isNotEmpty()) {
                _focusedApp.value = list.first()
            }
        }
    }

    /**
     * Called when the Home button is pressed while the launcher is open.
     */
    fun onHomePressed() {
        _homeTick.value += 1L
    }

    fun setFocusedApp(app: AppModel) {
        _focusedApp.value = app
    }

    fun toggleFavorite(app: AppModel) {
        if (app.isSystemShortcut) return
        viewModelScope.launch {
            favoritesStore.toggleFavorite(app.packageName)
        }
    }

    /**
     * Searches cities globally via Open-Meteo Geocoding API with debounce.
     */
    fun searchCities(query: String) {
        citySearchJob?.cancel()
        if (query.trim().length < 2) {
            _searchResults.value = emptyList()
            _isSearchingCities.value = false
            return
        }

        citySearchJob = viewModelScope.launch {
            _isSearchingCities.value = true
            delay(300L) // Debounce
            val results = weatherRepository.searchCities(query)
            _searchResults.value = results
            _isSearchingCities.value = false
        }
    }

    fun clearCitySearch() {
        citySearchJob?.cancel()
        _searchResults.value = emptyList()
        _isSearchingCities.value = false
    }

    /**
     * Selects and adds a city to the user's saved cities list, and triggers live weather fetch.
     */
    fun selectCity(city: CityLocationModel) {
        viewModelScope.launch {
            favoritesStore.setSelectedLocation(
                city = city.name,
                country = city.admin1?.let { "$it, ${city.countryCode ?: city.country}" } ?: city.country,
                lat = city.latitude,
                lon = city.longitude
            )
            clearCitySearch()
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
     * Cycles to the next wallpaper immediately.
     */
    fun nextWallpaper() {
        wallpaperRepository.nextWallpaper()
    }

    /**
     * Sets a specific wallpaper.
     */
    fun setWallpaper(wallpaper: WallpaperModel) {
        wallpaperRepository.setWallpaper(wallpaper)
    }

    // Global Search Actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * Changes the application library sort order and persists it in DataStore.
     */
    fun setSortOrder(order: AppSortOrder) {
        viewModelScope.launch {
            favoritesStore.setSortOrder(order)
        }
    }

    fun launchApp(app: AppModel): Boolean {
        if (!app.isSystemShortcut) {
            viewModelScope.launch {
                favoritesStore.recordAppLaunch(app.packageName)
            }
        }
        return repository.launchApp(app)
    }

    /**
     * Opens Fire OS system settings directly to the target app's management window
     * for Force Stop, Clear Cache, Clear Data, or permissions management.
     */
    fun openAppDetails(app: AppModel) {
        if (app.isSystemShortcut) return
        val context = getApplication<Application>()
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                // Ignore if device restricts direct settings access
            }
        }
    }
}
