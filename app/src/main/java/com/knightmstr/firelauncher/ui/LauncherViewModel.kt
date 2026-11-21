package com.knightmstr.firelauncher.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knightmstr.firelauncher.FireLauncherApp
import com.knightmstr.firelauncher.data.AppModel
import com.knightmstr.firelauncher.data.AppRepository
import com.knightmstr.firelauncher.data.AppSortOrder
import com.knightmstr.firelauncher.data.AppUsageStat
import com.knightmstr.firelauncher.data.CityLocationModel
import com.knightmstr.firelauncher.data.LauncherPrefsStore
import com.knightmstr.firelauncher.data.LauncherSettings
import com.knightmstr.firelauncher.data.LocalPhotoSource
import com.knightmstr.firelauncher.data.PinHasher
import com.knightmstr.firelauncher.data.WallpaperSource
import com.knightmstr.firelauncher.media.NowPlaying
import com.knightmstr.firelauncher.media.NowPlayingRepository
import com.knightmstr.firelauncher.update.UpdateInstaller
import com.knightmstr.firelauncher.update.UpdateState
import com.knightmstr.firelauncher.data.PrefsCodec
import com.knightmstr.firelauncher.data.WallpaperModel
import com.knightmstr.firelauncher.data.WallpaperRepository
import com.knightmstr.firelauncher.data.WeatherData
import com.knightmstr.firelauncher.data.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val localPhotos = LocalPhotoSource(application)

    private val wallpaperRepository = WallpaperRepository(
        scope = viewModelScope,
        rotationIntervalMinutes = settings.map { it.wallpaperIntervalMinutes }.distinctUntilChanged(),
        source = settings.map { it.wallpaperSource }.distinctUntilChanged(),
        localPhotos = localPhotos
    )

    private val nowPlayingRepository = NowPlayingRepository(application)
    private val updates = (application as FireLauncherApp).updateRepository

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

    private val customAppOrder: StateFlow<List<String>> = prefs.customAppOrderFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val kidsModeEnabled: StateFlow<Boolean> = prefs.kidsModeEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val kidsPackages: StateFlow<Set<String>> = prefs.kidsAppsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _rawApps = MutableStateFlow<List<AppModel>>(emptyList())

    /** Every installed app with usage stats, labels, hidden and kids flags applied (unsorted). */
    private val decoratedApps: StateFlow<List<AppModel>> =
        combine(_rawApps, appUsageStats, hiddenPackages, customLabels, kidsPackages) { raw, stats, hidden, labels, kids ->
            raw.map { app ->
                val stat = stats[app.packageName]
                app.copy(
                    label = labels[app.packageName] ?: app.originalLabel,
                    launchCount = stat?.launchCount ?: 0,
                    lastLaunchedTimeMs = stat?.lastLaunchedTimeMs ?: 0L,
                    isHidden = app.packageName in hidden,
                    isKidsAllowed = app.packageName in kids
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Apps that may appear in rows: not hidden, and kid-approved while Kids mode is on. */
    private val visibleApps: StateFlow<List<AppModel>> = combine(decoratedApps, kidsModeEnabled) { list, kids ->
        list.filter { !it.isHidden && (!kids || it.isKidsAllowed) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Visible apps sorted by the selected order (A–Z, Recent, Frequent or the user's own). */
    val apps: StateFlow<List<AppModel>> = combine(visibleApps, sortOrder, customAppOrder) { visible, order, custom ->
        when (order) {
            AppSortOrder.ALPHABETICAL -> visible.sortedBy { it.label.lowercase() }
            AppSortOrder.MOST_RECENT -> visible.sortedWith(
                compareByDescending<AppModel> { it.lastLaunchedTimeMs }.thenBy { it.label.lowercase() }
            )
            AppSortOrder.MOST_FREQUENT -> visible.sortedWith(
                compareByDescending<AppModel> { it.launchCount }.thenBy { it.label.lowercase() }
            )
            AppSortOrder.CUSTOM -> PrefsCodec.applyOrderWithRemainder(
                visible, custom, key = { it.packageName }, fallbackSort = { it.label.lowercase() }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Moves an app within All Apps. The first move switches the sort to "My Order",
     * starting from whatever order is on screen now.
     */
    fun moveInAllApps(app: AppModel, delta: Int) {
        val current = apps.value.map { it.packageName }
        viewModelScope.launch {
            if (sortOrder.value != AppSortOrder.CUSTOM) prefs.setSortOrder(AppSortOrder.CUSTOM)
            prefs.setCustomAppOrder(PrefsCodec.move(current, app.packageName, delta))
        }
    }

    val hiddenApps: StateFlow<List<AppModel>> = combine(decoratedApps, kidsModeEnabled) { list, kids ->
        if (kids) emptyList() else list.filter { it.isHidden }.sortedBy { it.label.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Total hidden apps, for the Settings summary (independent of Kids mode). */
    val hiddenAppCount: StateFlow<Int> = hiddenPackages.map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val allSystemShortcuts = repository.getSystemShortcuts()

    /** "All Apps" tile shown first in the home row. */
    val allAppsTile: AppModel = repository.getAllAppsTile()

    private val _isAllAppsOpen = MutableStateFlow(false)
    val isAllAppsOpen: StateFlow<Boolean> = _isAllAppsOpen.asStateFlow()

    fun openAllApps() { _isAllAppsOpen.value = true }
    fun closeAllApps() { _isAllAppsOpen.value = false }

    fun cycleSortOrder() {
        val next = AppSortOrder.entries[(sortOrder.value.ordinal + 1) % AppSortOrder.entries.size]
        setSortOrder(next)
    }

    /** In Kids mode only the (PIN-protected) settings tile remains, relabelled "Exit Kids Mode". */
    val systemShortcuts: StateFlow<List<AppModel>> = kidsModeEnabled.map { kids ->
        if (!kids) allSystemShortcuts
        else allSystemShortcuts
            .filter { it.packageName == AppRepository.LAUNCHER_SETTINGS_KEY }
            .map { it.copy(label = "Exit Kids Mode", originalLabel = "Exit Kids Mode") }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, allSystemShortcuts)

    // ------------------------------------------------------------------ Favourites

    private val favoriteOrder: StateFlow<List<String>> = prefs.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favorites: StateFlow<Set<String>> = favoriteOrder
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** Favourite apps in the user's chosen order (hidden / non-kids apps are left out). */
    val favoriteApps: StateFlow<List<AppModel>> = combine(visibleApps, favoriteOrder) { visible, order ->
        PrefsCodec.applyOrder(visible, order) { it.packageName }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ------------------------------------------------------------------ Quick buttons

    private val quickButtonOrder: StateFlow<List<String>> = prefs.quickButtonsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val quickButtonKeys: StateFlow<Set<String>> = quickButtonOrder.map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** One-press shortcuts shown in the header, max [LauncherPrefsStore.MAX_QUICK_BUTTONS]. */
    val quickButtonApps: StateFlow<List<AppModel>> = combine(visibleApps, quickButtonOrder) { visible, order ->
        PrefsCodec.applyOrder(visible, order) { it.packageName }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun toggleQuickButton(app: AppModel) {
        if (app.isSystemShortcut) return
        viewModelScope.launch { prefs.toggleQuickButton(app.packageName) }
    }

    // ------------------------------------------------------------------ Kids mode

    val hasKidsPin: StateFlow<Boolean> = prefs.hasKidsPinFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val kidsAppCount: StateFlow<Int> = kidsPackages.map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _pinDialog = MutableStateFlow<PinDialogState?>(null)
    val pinDialog: StateFlow<PinDialogState?> = _pinDialog.asStateFlow()

    private var failedPinAttempts = 0
    private var pinLockedUntilMs = 0L
    private var enableKidsAfterPinCreated = false

    fun setKidsAllowed(app: AppModel, allowed: Boolean) {
        if (app.isSystemShortcut) return
        viewModelScope.launch { prefs.setKidsAllowed(app.packageName, allowed) }
    }

    /** From Settings (parent side). Turning on asks for a new PIN the first time. */
    fun setKidsModeEnabled(enabled: Boolean) {
        if (enabled && !hasKidsPin.value) {
            enableKidsAfterPinCreated = true
            _pinDialog.value = PinDialogState(PinPurpose.CREATE)
            return
        }
        viewModelScope.launch {
            prefs.setKidsModeEnabled(enabled)
            if (enabled) {
                closeSettings()
                _homeTick.value += 1L
            }
        }
    }

    fun changeKidsPin() {
        enableKidsAfterPinCreated = false
        _pinDialog.value = PinDialogState(PinPurpose.CREATE)
    }

    fun onPinCreated(pin: String) {
        if (!PinHasher.isValidPin(pin)) return
        val enable = enableKidsAfterPinCreated
        enableKidsAfterPinCreated = false
        _pinDialog.value = null
        viewModelScope.launch {
            prefs.setKidsPin(pin)
            if (enable) setKidsModeEnabled(true)
        }
    }

    /** Checks the PIN to leave Kids mode; locks for 30 s after 5 wrong tries. */
    fun submitExitPin(pin: String) {
        val now = System.currentTimeMillis()
        if (now < pinLockedUntilMs) {
            _pinDialog.value = PinDialogState(PinPurpose.EXIT_KIDS_MODE, error = lockoutMessage(now))
            return
        }
        viewModelScope.launch {
            if (prefs.verifyKidsPin(pin)) {
                failedPinAttempts = 0
                _pinDialog.value = null
                prefs.setKidsModeEnabled(false)
                openSettings()
            } else {
                failedPinAttempts++
                if (failedPinAttempts >= 5) {
                    failedPinAttempts = 0
                    pinLockedUntilMs = System.currentTimeMillis() + 30_000L
                }
                val t = System.currentTimeMillis()
                _pinDialog.value = PinDialogState(
                    PinPurpose.EXIT_KIDS_MODE,
                    error = if (t < pinLockedUntilMs) lockoutMessage(t) else "Wrong PIN, try again"
                )
            }
        }
    }

    private fun lockoutMessage(now: Long) =
        "Too many tries. Wait ${((pinLockedUntilMs - now) / 1000L).coerceAtLeast(1)} s"

    fun closePinDialog() {
        enableKidsAfterPinCreated = false
        _pinDialog.value = null
    }

    // ------------------------------------------------------------------ App updates

    val updateState: StateFlow<UpdateState> = updates.state
    val installedVersion: String = updates.installedVersionName

    private val _isUpdateDialogOpen = MutableStateFlow(false)
    val isUpdateDialogOpen: StateFlow<Boolean> = _isUpdateDialogOpen.asStateFlow()

    private var updateJob: Job? = null

    /** Opens the update dialog, checking GitHub first unless an update is already known. */
    fun openUpdateDialog() {
        _isUpdateDialogOpen.value = true
        val s = updates.state.value
        if (s !is UpdateState.Available && s !is UpdateState.Downloading && s !is UpdateState.Installing) {
            checkForUpdates(quiet = false)
        }
    }

    fun closeUpdateDialog() {
        _isUpdateDialogOpen.value = false
        updates.dismissError()
    }

    fun checkForUpdates(quiet: Boolean) {
        viewModelScope.launch { updates.check(quiet) }
    }

    fun startUpdate() {
        val release = when (val s = updates.state.value) {
            is UpdateState.Available -> s.release
            is UpdateState.NeedsInstallPermission -> s.release
            is UpdateState.Failed -> s.release
            else -> null
        } ?: return
        if (updateJob?.isActive == true) return
        updateJob = viewModelScope.launch { updates.downloadAndInstall(release) }
    }

    fun openInstallPermissionSettings(): Boolean =
        UpdateInstaller.openInstallPermissionSettings(getApplication())

    // ------------------------------------------------------------------ Photos & now playing

    val localPhotoCount: StateFlow<Int> = wallpaperRepository.localPhotoCount

    private val _hasPhotoPermission = MutableStateFlow(localPhotos.hasPhotoPermission())
    val hasPhotoPermission: StateFlow<Boolean> = _hasPhotoPermission.asStateFlow()

    // One-shot event (SharedFlow, not StateFlow, so it doesn't replay when the Activity restarts)
    private val _photoPermissionRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val photoPermissionRequests: SharedFlow<Unit> = _photoPermissionRequests.asSharedFlow()

    fun requestPhotoPermission() { _photoPermissionRequests.tryEmit(Unit) }

    fun onPhotoPermissionResult() {
        _hasPhotoPermission.value = localPhotos.hasPhotoPermission()
        wallpaperRepository.reload()
    }

    val nowPlaying: StateFlow<NowPlaying?> = nowPlayingRepository.nowPlaying
    val hasNowPlayingAccess: StateFlow<Boolean> = nowPlayingRepository.hasAccess

    private val _showNowPlayingHelp = MutableStateFlow(false)
    val showNowPlayingHelp: StateFlow<Boolean> = _showNowPlayingHelp.asStateFlow()

    fun openNowPlayingHelp() { _showNowPlayingHelp.value = true }
    fun closeNowPlayingHelp() { _showNowPlayingHelp.value = false }

    /** Returns false when this Fire OS build has no notification-access screen. */
    fun openNotificationAccessSettings(): Boolean {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { getApplication<Application>().startActivity(intent) }.isSuccess
    }

    fun mediaPlayPause() = nowPlayingRepository.playPause()
    fun mediaNext() = nowPlayingRepository.skipNext()
    fun mediaPrevious() = nowPlayingRepository.skipPrevious()

    /** Opens the app that owns the current media session. */
    fun openNowPlayingApp() {
        val pkg = nowPlaying.value?.packageName ?: return
        val context = getApplication<Application>()
        context.packageManager.getLaunchIntentForPackage(pkg)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let { runCatching { context.startActivity(it) } }
    }

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

    private val allKnownApps = combine(decoratedApps, systemShortcuts) { a, b -> a + b + allAppsTile }

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
        if (app == null || app.isSystemShortcut || kidsModeEnabled.value) return
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


    fun onHomePressed() {
        _homeTick.value += 1L
        closeContextMenu()
        closeRename()
        closeSettings()
        closeNowPlayingHelp()
        closeAllApps()
        if (updates.state.value !is UpdateState.Downloading) closeUpdateDialog()
        onUserInteraction()
    }

    fun onVoiceResult(query: String) {
        _isSearchActive.value = true
        _searchQuery.value = query
    }

    // ------------------------------------------------------------------ Wallpapers

    val currentWallpaper: StateFlow<WallpaperModel?> = wallpaperRepository.currentWallpaper

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
        if (foreground) {
            scheduleIdle()
            nowPlayingRepository.start()
            updates.onInstallPermissionMaybeGranted()
            _hasPhotoPermission.value = localPhotos.hasPhotoPermission()
            // Pick up photos added while the launcher was in the background
            if (settings.value.wallpaperSource == WallpaperSource.MY_PHOTOS) wallpaperRepository.reload()
        } else {
            idleJob?.cancel()
            nowPlayingRepository.stop()
        }
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

        // Quiet update check shortly after start, then twice a day (well within GitHub's limits)
        viewModelScope.launch {
            delay(20_000L)
            while (isActive) {
                updates.check(quiet = true)
                delay(12 * 60 * 60 * 1000L)
            }
        }

        // Re-arm the screensaver timer when its timeout setting changes
        viewModelScope.launch {
            settings.map { it.screensaverTimeoutMinutes }.distinctUntilChanged().collect { scheduleIdle() }
        }
    }

    override fun onCleared() {
        nowPlayingRepository.stop()
        super.onCleared()
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
        if (app.packageName == AppRepository.ALL_APPS_KEY) {
            openAllApps()
            return true
        }
        if (app.packageName == AppRepository.LAUNCHER_SETTINGS_KEY) {
            if (kidsModeEnabled.value) _pinDialog.value = PinDialogState(PinPurpose.EXIT_KIDS_MODE)
            else openSettings()
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

enum class PinPurpose { CREATE, EXIT_KIDS_MODE }

data class PinDialogState(val purpose: PinPurpose, val error: String? = null)
