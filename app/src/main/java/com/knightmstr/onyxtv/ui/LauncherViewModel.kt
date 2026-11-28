package com.knightmstr.onyxtv.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knightmstr.onyxtv.OnyxApp
import com.knightmstr.onyxtv.data.AppModel
import com.knightmstr.onyxtv.home.HomeWatch
import com.knightmstr.onyxtv.screensaver.ScreensaverTakeover
import com.knightmstr.onyxtv.data.AppRepository
import com.knightmstr.onyxtv.data.AppSortOrder
import com.knightmstr.onyxtv.data.AppUsageStat
import com.knightmstr.onyxtv.data.BackupData
import com.knightmstr.onyxtv.data.AppFolder
import com.knightmstr.onyxtv.data.AppFolders
import com.knightmstr.onyxtv.data.CityLocationModel
import com.knightmstr.onyxtv.data.LauncherPrefsStore
import com.knightmstr.onyxtv.data.LauncherSettings
import com.knightmstr.onyxtv.data.PinHasher
import com.knightmstr.onyxtv.data.WallpaperSource
import com.knightmstr.onyxtv.media.NowPlaying
import com.knightmstr.onyxtv.media.NowPlayingRepository
import com.knightmstr.onyxtv.update.UpdateInstaller
import com.knightmstr.onyxtv.update.UpdateState
import com.knightmstr.onyxtv.data.PrefsCodec
import com.knightmstr.onyxtv.data.WallpaperModel
import com.knightmstr.onyxtv.data.WeatherData
import com.knightmstr.onyxtv.data.WeatherRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.knightmstr.onyxtv.upload.LanAddress
import com.knightmstr.onyxtv.upload.IconPng
import com.knightmstr.onyxtv.upload.PhoneSetupBridge
import com.knightmstr.onyxtv.upload.PhoneSetupServer
import com.knightmstr.onyxtv.upload.SetupApp
import com.knightmstr.onyxtv.upload.LinkResult
import com.knightmstr.onyxtv.upload.ApkStart
import com.knightmstr.onyxtv.upload.ApkStatus
import com.knightmstr.onyxtv.tools.AppSideloader
import com.knightmstr.onyxtv.data.WallpaperLinkResolver
import com.knightmstr.onyxtv.upload.SetupToken
import com.knightmstr.onyxtv.tools.SleepTimer
import com.knightmstr.onyxtv.kids.KidsTime
import com.knightmstr.onyxtv.kids.KidsTimeKeeper
import com.knightmstr.onyxtv.tools.StorageInfo
import com.knightmstr.onyxtv.home.SelfAdb
import kotlinx.coroutines.runBlocking

/**
 * Main MVVM coordinator. Exposes StateFlows for the app library (sorted, renamed, hidden),
 * ordered favourites, system shortcuts, wallpapers, weather, settings and the idle screensaver.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val app = application as OnyxApp
    private val prefs = app.prefs
    private val weatherRepository = WeatherRepository()

    // ------------------------------------------------------------------ Settings

    val settings: StateFlow<LauncherSettings> = prefs.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, LauncherSettings())

    private val localPhotos = app.localPhotos

    // App-wide, so the screensaver shows the same wallpaper (see OnyxApp)
    private val wallpaperRepository = app.wallpapers

    private val nowPlayingRepository = NowPlayingRepository(application)
    private val updates = app.updateRepository

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    fun openSettings() {
        _isSettingsOpen.value = true
        refreshStorage()
    }
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

    // ------------------------------------------------------------------ Top-bar buttons

    /** Wi-Fi button: Fire OS's Network screen. */
    fun openWifiSettings(): Boolean = repository.openNetworkSettings()

    /** Fire TV settings button: Fire OS's Settings menu on its own, not via Amazon's home. */
    fun openSystemSettings(): Boolean = repository.openFireTvSettings()

    /** Launcher Settings button; in Kids mode it asks for the PIN to leave Kids mode instead. */
    fun onLauncherSettingsPressed() {
        if (kidsModeEnabled.value) _pinDialog.value = PinDialogState(PinPurpose.EXIT_KIDS_MODE)
        else openSettings()
    }

    // ------------------------------------------------------------------ Favourites

    private val favoriteOrder: StateFlow<List<String>> = prefs.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favorites: StateFlow<Set<String>> = favoriteOrder
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** Dock folders (each holds apps; it sits in the dock as a `folder:<id>` favourite). */
    val folders: StateFlow<List<AppFolder>> = prefs.foldersFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * The dock in the user's order: favourite apps and folder tiles. Hidden / non-kids apps are
     * left out, also from inside folders; a folder with nothing left to show is skipped.
     */
    val favoriteApps: StateFlow<List<AppModel>> = combine(visibleApps, favoriteOrder, folders) { visible, order, folderList ->
        val byPkg = visible.associateBy { it.packageName }
        val byFolder = folderList.associateBy { AppFolders.keyOf(it.id) }
        order.mapNotNull { key ->
            val folder = byFolder[key]
            if (folder != null) folderTile(key, folder, folder.packages.mapNotNull { byPkg[it] })
            else byPkg[key]
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun folderTile(key: String, folder: AppFolder, inside: List<AppModel>): AppModel? {
        val first = inside.firstOrNull() ?: return null
        return AppModel(
            packageName = key,
            activityName = "",
            label = folder.name,
            originalLabel = folder.name,
            icon = first.icon,
            accentColor = first.accentColor,
            folderApps = inside
        )
    }

    // ------------------------------------------------------------------ Folders

    private val _openFolderKey = MutableStateFlow<String?>(null)
    /** The folder whose apps are showing, resolved live so changes inside it show at once. */
    val openFolder: StateFlow<AppModel?> = combine(_openFolderKey, favoriteApps) { key, dock ->
        key?.let { k -> dock.firstOrNull { it.packageName == k } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun closeFolder() { _openFolderKey.value = null }

    private val _folderPickerFor = MutableStateFlow<String?>(null)
    /** The app being moved into a folder (the "Move to folder" chooser), if open. */
    val folderPickerFor: StateFlow<String?> = _folderPickerFor.asStateFlow()

    fun openFolderPicker(app: AppModel) { if (!app.isSystemShortcut && !app.isFolder) _folderPickerFor.value = app.packageName }
    fun closeFolderPicker() { _folderPickerFor.value = null }

    fun moveToNewFolder(pkg: String) {
        _folderPickerFor.value = null
        viewModelScope.launch { prefs.editFolders { AppFolders.create(it, pkg) } }
    }

    fun moveToFolder(pkg: String, folderId: String) {
        _folderPickerFor.value = null
        viewModelScope.launch { prefs.editFolders { AppFolders.moveInto(it, pkg, folderId) } }
    }

    fun takeOutOfFolder(app: AppModel) {
        viewModelScope.launch { prefs.editFolders { AppFolders.takeOut(it, app.packageName) } }
    }

    fun renameFolder(folder: AppModel, name: String?) {
        val id = AppFolders.idOf(folder.packageName) ?: return
        if (name.isNullOrBlank()) return
        viewModelScope.launch { prefs.editFolders { AppFolders.rename(it, id, name) } }
    }

    fun deleteFolder(folder: AppModel) {
        val id = AppFolders.idOf(folder.packageName) ?: return
        if (_openFolderKey.value == folder.packageName) _openFolderKey.value = null
        viewModelScope.launch { prefs.editFolders { AppFolders.delete(it, id) } }
    }

    /** Name of the folder [app] is in, if any (for "Take out of …"). */
    fun folderNameOf(app: AppModel): String? = AppFolders.folderOf(folders.value, app.packageName)?.name

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
    fun submitExitPin(pin: String) = submitPin(pin, PinPurpose.EXIT_KIDS_MODE) {
        prefs.setKidsModeEnabled(false)
        openSettings()
    }

    /** The PIN on the "Time's up" screen: then a parent picks extra time or leaves Kids mode. */
    fun submitUnlockPin(pin: String) = submitPin(pin, PinPurpose.UNLOCK_KIDS_TIME) {
        _kidsExtendOpen.value = true
    }

    private fun submitPin(pin: String, purpose: PinPurpose, onRight: suspend () -> Unit) {
        val now = System.currentTimeMillis()
        if (now < pinLockedUntilMs) {
            _pinDialog.value = PinDialogState(purpose, error = lockoutMessage(now))
            return
        }
        viewModelScope.launch {
            if (prefs.verifyKidsPin(pin)) {
                failedPinAttempts = 0
                _pinDialog.value = null
                onRight()
            } else {
                failedPinAttempts++
                if (failedPinAttempts >= 5) {
                    failedPinAttempts = 0
                    pinLockedUntilMs = System.currentTimeMillis() + 30_000L
                }
                val t = System.currentTimeMillis()
                _pinDialog.value = PinDialogState(
                    purpose,
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

    // ------------------------------------------------------------------ Fire TV system features
    // Home button watcher, screensaver takeover and keep-awake all need the same one-time ADB
    // grant (WRITE_SECURE_SETTINGS); without it, pressing the row opens the setup dialog.

    private val _homeWatchStatus = MutableStateFlow(HomeWatch.status(application))
    val homeWatchStatus: StateFlow<HomeWatch.Status> = _homeWatchStatus.asStateFlow()

    private val _screensaverTakeover = MutableStateFlow(ScreensaverTakeover.status(application))
    val screensaverTakeover: StateFlow<ScreensaverTakeover.Status> = _screensaverTakeover.asStateFlow()

    private val _keepAwake = MutableStateFlow(ScreensaverTakeover.keepAwake(application))
    val keepAwake: StateFlow<Boolean> = _keepAwake.asStateFlow()

    private val _needsRestart = MutableStateFlow(ScreensaverTakeover.needsRestart(application))
    val needsRestart: StateFlow<Boolean> = _needsRestart.asStateFlow()

    /** Which feature the one-time ADB setup dialog is for; null = closed. */
    private val _grantSetupFor = MutableStateFlow<GrantFeature?>(null)
    val grantSetupFor: StateFlow<GrantFeature?> = _grantSetupFor.asStateFlow()

    fun refreshSystemFeatures() {
        val ctx = getApplication<Application>()
        _homeWatchStatus.value = HomeWatch.status(ctx)
        _screensaverTakeover.value = ScreensaverTakeover.status(ctx)
        _keepAwake.value = ScreensaverTakeover.keepAwake(ctx)
        _needsRestart.value = ScreensaverTakeover.needsRestart(ctx)
    }

    fun onHomeWatchPressed() = toggleOrSetUp(GrantFeature.HOME_BUTTON)
    fun onScreensaverTakeoverPressed() = toggleOrSetUp(GrantFeature.SCREENSAVER)
    fun onKeepAwakePressed() = toggleOrSetUp(GrantFeature.KEEP_AWAKE)

    private fun toggleOrSetUp(feature: GrantFeature) {
        val ctx = getApplication<Application>()
        if (!HomeWatch.canManage(ctx)) {
            _grantSetupFor.value = feature
            return
        }
        when (feature) {
            GrantFeature.HOME_BUTTON -> HomeWatch.setWanted(ctx, !HomeWatch.isEnabled(ctx))
            GrantFeature.SCREENSAVER -> ScreensaverTakeover.setOn(ctx, !ScreensaverTakeover.isActive(ctx))
            GrantFeature.KEEP_AWAKE -> ScreensaverTakeover.setKeepAwake(ctx, !ScreensaverTakeover.keepAwake(ctx))
        }
        refreshSystemFeatures()
    }

    /** From the setup dialog, after the ADB grant: turn the feature on. False if not granted yet. */
    fun enableGrantedFeature(): Boolean {
        val ctx = getApplication<Application>()
        val ok = when (_grantSetupFor.value) {
            GrantFeature.HOME_BUTTON -> HomeWatch.setWanted(ctx, true)
            GrantFeature.SCREENSAVER -> ScreensaverTakeover.setOn(ctx, true)
            GrantFeature.KEEP_AWAKE -> ScreensaverTakeover.setKeepAwake(ctx, true)
            null -> false
        }
        refreshSystemFeatures()
        if (ok) _grantSetupFor.value = null
        return ok
    }

    fun closeGrantSetup() { _grantSetupFor.value = null }

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
        viewModelScope.launch { updates.check(quiet, settings.value.updateChannel) }
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

    // ------------------------------------------------------------------ Kids mode screen time

    /** Today's Kids mode usage, the rules, and whether it's locked right now. */
    val kidsTime: StateFlow<KidsTimeKeeper.Status?> = KidsTimeKeeper.status

    init {
        val app = getApplication<Application>()
        // Ticking follows Kids mode on/off
        viewModelScope.launch {
            kidsModeEnabled.collect { KidsTimeKeeper.onKidsModeChanged(app, it) }
        }
        // Keep the lock and the settings summary current while Onyx TV is showing
        viewModelScope.launch {
            while (isActive) {
                KidsTimeKeeper.refresh(app)
                delay(30_000)
            }
        }
    }

    init {
        // A phone-installed app shows up at once (and can be added to the dock from the phone)
        viewModelScope.launch {
            AppSideloader.status.map { it.state to it.serial }.distinctUntilChanged().collect { (state, _) ->
                if (state == AppSideloader.State.INSTALLED) refreshApps()
            }
        }
    }

    fun setKidsDailyLimit(minutes: Int) {
        val app = getApplication<Application>()
        val r = KidsTimeKeeper.rules(app)
        KidsTimeKeeper.setRules(app, r.copy(dailyLimitMinutes = minutes.takeIf { it in KidsTime.LIMIT_OPTIONS } ?: 0))
    }

    fun setKidsBedtime(on: Boolean? = null, startHour: Int? = null, endHour: Int? = null) {
        val app = getApplication<Application>()
        val r = KidsTimeKeeper.rules(app)
        KidsTimeKeeper.setRules(
            app,
            r.copy(
                bedtimeOn = on ?: r.bedtimeOn,
                bedtimeStartHour = startHour ?: r.bedtimeStartHour,
                bedtimeEndHour = endHour ?: r.bedtimeEndHour
            )
        )
    }

    private val _kidsExtendOpen = MutableStateFlow(false)
    /** After the parent's PIN on the "Time's up" screen: +30 min, +1 hr or leave Kids mode. */
    val kidsExtendOpen: StateFlow<Boolean> = _kidsExtendOpen.asStateFlow()

    fun askParentPin() { _pinDialog.value = PinDialogState(PinPurpose.UNLOCK_KIDS_TIME) }

    fun extendKidsTime(minutes: Int) {
        _kidsExtendOpen.value = false
        KidsTimeKeeper.extend(getApplication(), minutes)
    }

    fun leaveKidsModeFromLock() {
        _kidsExtendOpen.value = false
        viewModelScope.launch { prefs.setKidsModeEnabled(false) }
    }

    fun closeKidsExtend() { _kidsExtendOpen.value = false }

    // ------------------------------------------------------------------ Sleep timer & storage

    /** When the sleep timer goes off (wall clock), or null when it's off. */
    val sleepTimerEndsAt: StateFlow<Long?> = SleepTimer.endsAt

    private val _toolsUi = MutableStateFlow(ToolsUi(space = runCatching { StorageInfo.read() }.getOrNull()))
    val toolsUi: StateFlow<ToolsUi> = _toolsUi.asStateFlow()

    init {
        SleepTimer.load(getApplication())
    }

    /** Sets the sleep timer (0 = off). Needs ADB debugging on to work. */
    fun setSleepTimer(minutes: Int) {
        val app = getApplication<Application>()
        val next = minutes.coerceAtLeast(0)
        if (next > 0 && !SelfAdb.isAdbOn(app)) {
            _toolsUi.update { it.copy(sleepMessage = "Turn on ADB debugging first (Fire TV Settings → My Fire TV → Developer options).") }
            return
        }
        SleepTimer.set(app, next)
        _toolsUi.update { it.copy(sleepMessage = null) }
    }

    fun refreshStorage() {
        _toolsUi.update { it.copy(space = runCatching { StorageInfo.read() }.getOrNull()) }
    }

    /** Clears every app's cache through the TV's own ADB, then reports how much was freed. */
    fun freeUpSpace() {
        if (_toolsUi.value.cleaning) return
        _toolsUi.update { it.copy(cleaning = true, storageMessage = "Clearing app caches…") }
        viewModelScope.launch {
            val message = when (val r = StorageInfo.freeUpSpace(getApplication())) {
                is StorageInfo.CleanResult.Freed -> {
                    _toolsUi.update { it.copy(space = r.space) }
                    if (r.bytes > 0) "Freed ${StorageInfo.describe(r.bytes)}" else "Caches were already small"
                }
                StorageInfo.CleanResult.AdbOff -> "Turn on ADB debugging first (Developer options)"
                StorageInfo.CleanResult.NotAllowed -> "Press Allow on the TV's ADB box (tick Always allow), then try again"
                is StorageInfo.CleanResult.Failed -> "Couldn't clear caches: ${r.reason.take(60)}"
            }
            _toolsUi.update { it.copy(cleaning = false, storageMessage = message) }
        }
    }

    // ------------------------------------------------------------------ Set up from phone (Wi-Fi)

    /** Number of photos in the app's own upload folder (what adb push / the uploader write to). */
    private val _uploadedPhotoCount = MutableStateFlow(0)
    val uploadedPhotoCount: StateFlow<Int> = _uploadedPhotoCount.asStateFlow()

    private val _phoneSetupUi = MutableStateFlow(PhoneSetupUi())
    val phoneSetupUi: StateFlow<PhoneSetupUi> = _phoneSetupUi.asStateFlow()

    private var setupServer: PhoneSetupServer? = null
    private var setupIdleWatch: Job? = null

    /** Icons rendered for the phone page, cached for as long as the server runs. */
    private val iconCache = java.util.concurrent.ConcurrentHashMap<String, ByteArray>()

    init {
        viewModelScope.launch { refreshUploadedCount() }
    }

    private suspend fun refreshUploadedCount() {
        val count = withContext(Dispatchers.IO) { localPhotos.listUploads().size }
        _uploadedPhotoCount.value = count
        _phoneSetupUi.update { it.copy(count = count) }
    }

    /**
     * What the phone page may read and change. Writes go through the same prefs calls as the TV's
     * own menus, so the home screen updates live. Runs on the server's worker threads.
     */
    private val setupBridge = object : PhoneSetupBridge {
        override fun listPhotos() = localPhotos.listUploads()
        override fun savePhoto(bytes: ByteArray, originalName: String?) = localPhotos.saveUpload(bytes, originalName)
        override fun deletePhoto(name: String) = localPhotos.deleteUpload(name)

        private fun installed() = decoratedApps.value.filter { !it.isSystemShortcut }

        override fun apps() = installed().map {
            SetupApp(packageName = it.packageName, label = it.label, originalLabel = it.originalLabel, hidden = it.isHidden)
        }

        // Read straight from DataStore, not the StateFlows: those update a moment later, and two
        // quick changes from the phone would otherwise build on stale values and undo each other.
        override fun favoriteOrder() = runBlocking { prefs.favoritesFlow.first() }

        override fun iconPng(packageName: String): ByteArray? {
            iconCache[packageName]?.let { return it }
            val app = installed().firstOrNull { it.packageName == packageName } ?: return null
            return IconPng.render(app.icon)?.also { iconCache[packageName] = it }
        }

        override fun setFavoriteOrder(order: List<String>) = runBlocking { prefs.setFavoriteOrder(order) }

        override fun setHidden(packageName: String, hidden: Boolean) =
            runBlocking { prefs.setHidden(packageName, hidden) }

        override fun rename(packageName: String, label: String?) {
            val original = installed().firstOrNull { it.packageName == packageName }?.originalLabel ?: return
            val clean = label?.trim()?.takeIf { it.isNotEmpty() && it != original }
            runBlocking { prefs.setCustomLabel(packageName, clean) }
        }

        override fun settings() = runBlocking { prefs.settingsFlow.first() }

        override fun updateSettings(settings: LauncherSettings) = runBlocking { prefs.updateSettings(settings) }

        override fun wallpaperUrls() = runBlocking { prefs.customWallpaperUrlsFlow.first() }

        override fun addWallpaperUrl(url: String): LinkResult = runBlocking {
            when (val result = WallpaperLinkResolver.resolve(url)) {
                is WallpaperLinkResolver.Result.Ok -> {
                    prefs.addCustomWallpaperUrls(result.urls)
                    LinkResult.Added(result.what)
                }
                is WallpaperLinkResolver.Result.Error -> LinkResult.Failed(result.message)
            }
        }

        override fun removeWallpaperUrl(url: String) = runBlocking { prefs.removeCustomWallpaperUrl(url) }

        override fun freeSpaceBytes(): Long = runCatching {
            android.os.StatFs(AppSideloader.workDir(getApplication()).path).availableBytes
        }.getOrDefault(0L)

        override fun apkWorkDir(): java.io.File = AppSideloader.workDir(getApplication())

        override fun startApkInstall(apk: java.io.File): ApkStart =
            when (val r = AppSideloader.start(getApplication(), apk)) {
                is AppSideloader.Start.Started -> ApkStart.Started(r.status.toApkStatus())
                is AppSideloader.Start.Failed -> ApkStart.Failed(r.message)
            }

        override fun apkStatus(): ApkStatus = AppSideloader.status.value.toApkStatus()

        private fun AppSideloader.Status.toApkStatus() = ApkStatus(
            state = state.name.lowercase(),
            label = label,
            packageName = packageName,
            versionName = versionName,
            isUpdate = isUpdate,
            message = message,
            serial = serial
        )

        override fun exportBackup() = runBlocking { prefs.backupSnapshot() }

        // Weather follows on its own: it watches the stored city and units.
        override fun restoreBackup(backup: BackupData) = runBlocking { prefs.restoreBackup(backup) }

        override fun isLocked() = this@LauncherViewModel.kidsModeEnabled.value

        override fun onChanged() {
            viewModelScope.launch {
                wallpaperRepository.reload()
                refreshUploadedCount()
            }
        }
    }

    /** Opens "Set up from phone" and starts the Wi-Fi server. [tab] picks the page's first tab. */
    fun openPhoneSetup(tab: SetupTab = SetupTab.APPS) {
        if (kidsModeEnabled.value) return
        val existing = setupServer
        if (existing != null) {
            _phoneSetupUi.update { it.copy(open = true, url = it.url?.let { u -> withTab(u, tab) }) }
            return
        }
        val app = getApplication<Application>()
        val ip = LanAddress.wifiIpv4(app)
        if (ip == null) {
            _phoneSetupUi.value = PhoneSetupUi(open = true, running = false, error = "Connect the TV to Wi-Fi first.")
            viewModelScope.launch { refreshUploadedCount() }
            return
        }
        val token = SetupToken.generate()
        try {
            val page = app.assets.open(SETUP_PAGE).bufferedReader().use { it.readText() }
            val server = PhoneSetupServer(SETUP_PORT, token, page, setupBridge)
            server.startServer()
            setupServer = server
            _phoneSetupUi.value = PhoneSetupUi(
                open = true,
                running = true,
                url = withTab("http://$ip:$SETUP_PORT/?t=$token", tab)
            )
            viewModelScope.launch { refreshUploadedCount() }
            watchSetupIdle(server)
        } catch (e: Exception) {
            _phoneSetupUi.value = PhoneSetupUi(open = true, running = false, error = "Couldn't start phone setup.")
        }
    }

    /** Stops the server once the phone has made no request for [SETUP_IDLE_LIMIT_MS]. */
    private fun watchSetupIdle(server: PhoneSetupServer) {
        setupIdleWatch?.cancel()
        setupIdleWatch = viewModelScope.launch {
            while (isActive && setupServer === server) {
                delay(30_000)
                if (System.currentTimeMillis() - server.lastActivityMs > SETUP_IDLE_LIMIT_MS) {
                    _phoneSetupUi.update {
                        it.copy(
                            running = false,
                            url = null,
                            error = "Phone setup stopped after 10 minutes without use. Close this and open it again for a new code."
                        )
                    }
                    stopSetupServer() // cancels this loop too
                }
            }
        }
    }

    private fun withTab(url: String, tab: SetupTab) = url.substringBefore('#') + "#" + tab.anchor

    /** Closes "Set up from phone" and stops the server. */
    fun closePhoneSetup() {
        stopSetupServer()
        _phoneSetupUi.value = PhoneSetupUi(open = false, count = _uploadedPhotoCount.value)
        wallpaperRepository.reload()
    }

    private fun stopSetupServer() {
        setupIdleWatch?.cancel()
        setupIdleWatch = null
        setupServer?.let { runCatching { it.stop() } }
        setupServer = null
        iconCache.clear()
    }

    /** Deletes every uploaded photo (the app's own folder only; never the gallery album). */
    fun clearUploadedPhotos() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { localPhotos.clearUploads() }
            wallpaperRepository.reload()
            refreshUploadedCount()
        }
    }


    // ------------------------------------------------------------------ Custom wallpaper URLs

    val customWallpaperUrls: StateFlow<List<String>> =
        prefs.customWallpaperUrlsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _wallpaperUrlDialogOpen = MutableStateFlow(false)
    val wallpaperUrlDialogOpen: StateFlow<Boolean> = _wallpaperUrlDialogOpen.asStateFlow()

    private val _wallpaperUrlStatus = MutableStateFlow<WallpaperUrlStatus>(WallpaperUrlStatus.Idle)
    val wallpaperUrlStatus: StateFlow<WallpaperUrlStatus> = _wallpaperUrlStatus.asStateFlow()

    fun openWallpaperUrls() {
        _wallpaperUrlStatus.value = WallpaperUrlStatus.Idle
        _wallpaperUrlDialogOpen.value = true
    }

    fun closeWallpaperUrls() {
        _wallpaperUrlDialogOpen.value = false
    }

    /**
     * Checks a pasted picture link, cloud share link, subreddit or Reddit post online, then saves
     * what it resolves to.
     */
    fun addWallpaperUrl(raw: String) {
        val url = raw.trim()
        if (url.isEmpty()) return
        viewModelScope.launch {
            _wallpaperUrlStatus.value = WallpaperUrlStatus.Checking
            _wallpaperUrlStatus.value = when (val result = WallpaperLinkResolver.resolve(url)) {
                is WallpaperLinkResolver.Result.Ok -> {
                    prefs.addCustomWallpaperUrls(result.urls) // repo observes the flow, so wallpapers refresh
                    WallpaperUrlStatus.Added(result.what)
                }
                is WallpaperLinkResolver.Result.Error -> WallpaperUrlStatus.Failed(result.message)
            }
        }
    }

    fun removeWallpaperUrl(url: String) {
        viewModelScope.launch { prefs.removeCustomWallpaperUrl(url) }
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
        if (app.isSystemShortcut || app.isFolder) return
        viewModelScope.launch { prefs.toggleFavorite(app.packageName) }
    }

    /** Moves a favourite left (negative) or right (positive) in the Favourites row. */
    fun moveFavorite(app: AppModel, delta: Int) {
        viewModelScope.launch { prefs.moveFavorite(app.packageName, delta) }
    }

    // ------------------------------------------------------------------ Hide / rename

    fun setHidden(app: AppModel, hidden: Boolean) {
        if (app.isSystemShortcut || app.isFolder) return
        viewModelScope.launch { prefs.setHidden(app.packageName, hidden) }
    }

    fun renameApp(app: AppModel, newLabel: String?) {
        if (app.isFolder) return renameFolder(app, newLabel)
        if (app.isSystemShortcut) return
        val label = newLabel?.trim()?.takeIf { it.isNotEmpty() && it != app.originalLabel }
        viewModelScope.launch { prefs.setCustomLabel(app.packageName, label) }
    }

    // ------------------------------------------------------------------ Focus, menus & dialogs

    private val allKnownApps = combine(decoratedApps, favoriteApps) { apps, dock -> apps + allAppsTile + dock.filter { it.isFolder } }

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
        closeGrantSetup()
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
        app.latestWeather.value = _weather.value
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
            refreshSystemFeatures()
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
                updates.check(quiet = true, channel = settings.value.updateChannel)
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
        stopSetupServer()
        super.onCleared()
    }

    private companion object {
        const val SETUP_PORT = 8723
        const val SETUP_PAGE = "phone-setup.html"
        const val SETUP_IDLE_LIMIT_MS = 10 * 60 * 1000L
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
        if (app.isFolder) {
            _openFolderKey.value = app.packageName
            return true
        }
        // Kids mode time's up / bedtime: apps stay closed until a parent allows more time
        if (kidsModeEnabled.value && kidsTime.value?.lock != null) return false
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

/** Result of validating/adding a custom wallpaper URL. */
sealed interface WallpaperUrlStatus {
    data object Idle : WallpaperUrlStatus
    data object Checking : WallpaperUrlStatus
    data class Added(val what: String) : WallpaperUrlStatus
    data class Failed(val message: String) : WallpaperUrlStatus
}

/** State for the Wi-Fi photo upload screen. */
data class ToolsUi(
    val space: StorageInfo.Space? = null,
    val cleaning: Boolean = false,
    val storageMessage: String? = null,
    val sleepMessage: String? = null
)

/** Which tab the phone page opens on. */
enum class SetupTab(val anchor: String) { APPS("apps"), PHOTOS("photos") }

data class PhoneSetupUi(
    val open: Boolean = false,
    val running: Boolean = false,
    val url: String? = null,
    val count: Int = 0,
    val error: String? = null
)

enum class PinPurpose { CREATE, EXIT_KIDS_MODE, UNLOCK_KIDS_TIME }

data class PinDialogState(val purpose: PinPurpose, val error: String? = null)

/** Fire TV features that need the one-time ADB grant (WRITE_SECURE_SETTINGS). */
enum class GrantFeature(val title: String) {
    HOME_BUTTON("Make Home open Onyx TV"),
    SCREENSAVER("Use as Fire TV screensaver"),
    KEEP_AWAKE("Keep Fire TV awake")
}
