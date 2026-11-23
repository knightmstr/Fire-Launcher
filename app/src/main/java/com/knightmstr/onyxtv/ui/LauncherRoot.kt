package com.knightmstr.onyxtv.ui

import com.knightmstr.onyxtv.data.HomeLayout
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.animation.core.animateFloatAsState
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.knightmstr.onyxtv.data.AppModel
import com.knightmstr.onyxtv.data.AppSortOrder
import com.knightmstr.onyxtv.data.CityLocationModel
import com.knightmstr.onyxtv.data.WallpaperModel
import com.knightmstr.onyxtv.data.WeatherData
import com.knightmstr.onyxtv.media.NowPlaying
import com.knightmstr.onyxtv.ui.components.AllAppsScreen
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextOverflow
import com.knightmstr.onyxtv.ui.components.CitySelectionDialog
import com.knightmstr.onyxtv.ui.components.HomeDock
import com.knightmstr.onyxtv.ui.components.HomeClock
import com.knightmstr.onyxtv.ui.components.GrantSetupDialog
import com.knightmstr.onyxtv.ui.components.AppContextMenuDialog
import com.knightmstr.onyxtv.ui.components.LocalCardShape
import com.knightmstr.onyxtv.ui.components.LocalCardWidth
import com.knightmstr.onyxtv.ui.components.LocalScreenEdge
import com.knightmstr.onyxtv.ui.components.rememberScreenEdge
import com.knightmstr.onyxtv.ui.components.CreatePinDialog
import com.knightmstr.onyxtv.ui.components.NowPlayingCard
import com.knightmstr.onyxtv.ui.components.NowPlayingHelpDialog
import com.knightmstr.onyxtv.ui.components.PinEntryDialog
import com.knightmstr.onyxtv.ui.components.SettingsStatus
import com.knightmstr.onyxtv.ui.components.UpdateChip
import com.knightmstr.onyxtv.ui.components.UpdateDialog
import com.knightmstr.onyxtv.update.UpdateState
import com.knightmstr.onyxtv.ui.components.LauncherSettingsDialog
import com.knightmstr.onyxtv.ui.components.PhotoUploadDialog
import com.knightmstr.onyxtv.ui.components.WallpaperUrlsDialog
import com.knightmstr.onyxtv.ui.components.toContentScale
import com.knightmstr.onyxtv.ui.components.RenameAppDialog
import com.knightmstr.onyxtv.ui.components.Screensaver
import com.knightmstr.onyxtv.ui.components.AppRow
import com.knightmstr.onyxtv.ui.components.SortSelectionDialog
import com.knightmstr.onyxtv.ui.components.TopBar
import com.knightmstr.onyxtv.ui.theme.DarkBackground
import com.knightmstr.onyxtv.ui.theme.FireOrange
import com.knightmstr.onyxtv.ui.theme.TextPrimary
import com.knightmstr.onyxtv.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Main stateless container collecting ViewModel state, managing D-pad focus,
 * animating dynamic background accent gradients, adaptive screen-edge margins,
 * auto-changing r/TVWallpapersSFW 30-minute wallpapers, and intercepting Back/Home events.
 */
@Composable
fun LauncherRoot(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val hiddenApps by viewModel.hiddenApps.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val favoriteApps by viewModel.favoriteApps.collectAsStateWithLifecycle()
    val focusedApp by viewModel.focusedApp.collectAsStateWithLifecycle()
    val homeTick by viewModel.homeTick.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val isCelsius by viewModel.isCelsius.collectAsStateWithLifecycle()
    val savedCities by viewModel.savedCities.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearchingCities by viewModel.isSearchingCities.collectAsStateWithLifecycle()
    val currentWallpaper by viewModel.currentWallpaper.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val contextMenuApp by viewModel.contextMenuApp.collectAsStateWithLifecycle()
    val renameTarget by viewModel.renameTarget.collectAsStateWithLifecycle()
    val screensaverVisible by viewModel.screensaverVisible.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val kidsMode by viewModel.kidsModeEnabled.collectAsStateWithLifecycle()
    val pinDialog by viewModel.pinDialog.collectAsStateWithLifecycle()
    val showNowPlayingHelp by viewModel.showNowPlayingHelp.collectAsStateWithLifecycle()
    val hiddenAppCount by viewModel.hiddenAppCount.collectAsStateWithLifecycle()
    val localPhotoCount by viewModel.localPhotoCount.collectAsStateWithLifecycle()
    val uploadedPhotoCount by viewModel.uploadedPhotoCount.collectAsStateWithLifecycle()
    val uploadUi by viewModel.uploadUi.collectAsStateWithLifecycle()
    val customWallpaperUrls by viewModel.customWallpaperUrls.collectAsStateWithLifecycle()
    val wallpaperUrlDialogOpen by viewModel.wallpaperUrlDialogOpen.collectAsStateWithLifecycle()
    val wallpaperUrlStatus by viewModel.wallpaperUrlStatus.collectAsStateWithLifecycle()
    val hasPhotoPermission by viewModel.hasPhotoPermission.collectAsStateWithLifecycle()
    val hasKidsPin by viewModel.hasKidsPin.collectAsStateWithLifecycle()
    val kidsAppCount by viewModel.kidsAppCount.collectAsStateWithLifecycle()
    val hasNowPlayingAccess by viewModel.hasNowPlayingAccess.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val isUpdateDialogOpen by viewModel.isUpdateDialogOpen.collectAsStateWithLifecycle()
    val isAllAppsOpen by viewModel.isAllAppsOpen.collectAsStateWithLifecycle()
    val homeWatchStatus by viewModel.homeWatchStatus.collectAsStateWithLifecycle()
    val grantSetupFor by viewModel.grantSetupFor.collectAsStateWithLifecycle()
    val screensaverTakeover by viewModel.screensaverTakeover.collectAsStateWithLifecycle()
    val keepAwake by viewModel.keepAwake.collectAsStateWithLifecycle()
    val needsRestart by viewModel.needsRestart.collectAsStateWithLifecycle()

    // Pause the screensaver timer while any dialog has the keys
    val anyDialogOpen = contextMenuApp != null || renameTarget != null || isSettingsOpen ||
        pinDialog != null || showNowPlayingHelp || isUpdateDialogOpen || isAllAppsOpen || grantSetupFor != null
    LaunchedEffect(anyDialogOpen) { viewModel.setDialogOpen(anyDialogOpen) }

    // Back closes an open search; otherwise it is absorbed so the launcher never exits
    BackHandler(enabled = isSearchActive) { viewModel.setSearchActive(false) }

    CompositionLocalProvider(
        LocalCardWidth provides settings.tileWidthDp.dp,
        LocalCardShape provides settings.cardShape,
        LocalScreenEdge provides rememberScreenEdge()
    ) {
        LauncherContent(
            apps = apps,
            hiddenApps = if (settings.showHiddenApps) hiddenApps else emptyList(),
            favorites = favorites,
            favoriteApps = favoriteApps,
            allAppsTile = viewModel.allAppsTile,
            showAllAppsRow = settings.showAllAppsRow,
            cleanLayout = settings.homeLayout == HomeLayout.CLEAN,
            onOpenWifi = { viewModel.openWifiSettings() },
            onOpenSystemSettings = { viewModel.openSystemSettings() },
            onOpenLauncherSettings = viewModel::onLauncherSettingsPressed,
            focusedApp = focusedApp,
            homeTick = homeTick,
            weather = weather,
            savedCities = savedCities,
            searchResults = searchResults,
            isSearchingCities = isSearchingCities,
            currentWallpaper = currentWallpaper,
            wallpaperContentScale = settings.wallpaperFit.toContentScale(),
            sortOrder = sortOrder,
            searchQuery = searchQuery,
            isSearchActive = isSearchActive,
            filteredApps = filteredApps,
            nowPlaying = nowPlaying,
            kidsMode = kidsMode,
            updateState = if (kidsMode) UpdateState.Idle else updateState,
            onOpenUpdates = viewModel::openUpdateDialog,
            onMediaPlayPause = viewModel::mediaPlayPause,
            onMediaNext = viewModel::mediaNext,
            onMediaPrevious = viewModel::mediaPrevious,
            onOpenNowPlayingApp = viewModel::openNowPlayingApp,
            onSearchCities = viewModel::searchCities,
            onClearCitySearch = viewModel::clearCitySearch,
            onSelectCity = viewModel::selectCity,
            onToggleUnit = viewModel::toggleTemperatureUnit,
            onNextWallpaper = viewModel::nextWallpaper,
            onSelectSort = viewModel::setSortOrder,
            onSearchQueryChange = viewModel::setSearchQuery,
            onToggleSearch = viewModel::setSearchActive,
            onAppFocused = viewModel::setFocusedApp,
            onAppClicked = { viewModel.launchApp(it) },
            onAppLongClicked = viewModel::openContextMenu,
            modifier = modifier
        )
    }

    contextMenuApp?.let { app ->
        AppContextMenuDialog(
            app = app,
            isFavorite = favorites.contains(app.packageName),
            favoriteIndex = favoriteApps.indexOfFirst { it.packageName == app.packageName },
            favoriteCount = favoriteApps.size,
            allAppsIndex = apps.indexOfFirst { it.packageName == app.packageName },
            allAppsCount = apps.size,
            onLaunchApp = { viewModel.launchApp(app) },
            onToggleFavorite = { viewModel.toggleFavorite(app) },
            onMoveFavorite = { delta -> viewModel.moveFavorite(app, delta) },
            onMoveInAllApps = { delta -> viewModel.moveInAllApps(app, delta) },
            onToggleKidsAllowed = { viewModel.setKidsAllowed(app, !app.isKidsAllowed) },
            onRename = { viewModel.startRename(app) },
            onToggleHidden = { viewModel.setHidden(app, !app.isHidden) },
            onOpenAppDetails = { viewModel.openAppDetails(app) },
            onDismiss = viewModel::closeContextMenu
        )
    }

    renameTarget?.let { app ->
        RenameAppDialog(
            app = app,
            onSave = { label -> viewModel.renameApp(app, label) },
            onDismiss = viewModel::closeRename
        )
    }

    if (isSettingsOpen) {
        LauncherSettingsDialog(
            settings = settings,
            status = SettingsStatus(
                isCelsius = isCelsius,
                hiddenAppCount = hiddenAppCount,
                localPhotoCount = localPhotoCount,
                uploadedPhotoCount = uploadedPhotoCount,
                customUrlCount = customWallpaperUrls.size,
                hasPhotoPermission = hasPhotoPermission,
                hasKidsPin = hasKidsPin,
                kidsAppCount = kidsAppCount,
                hasNowPlayingAccess = hasNowPlayingAccess,
                installedVersion = viewModel.installedVersion,
                updateSummary = when (val u = updateState) {
                    is UpdateState.Available -> "Version ${u.release.versionName} is available"
                    is UpdateState.Downloading -> "Downloading update…"
                    is UpdateState.UpToDate -> "Up to date"
                    is UpdateState.Failed -> "Last check failed"
                    else -> "Check GitHub for a new version"
                },
                homeWatch = homeWatchStatus,
                screensaverTakeover = screensaverTakeover,
                keepAwake = keepAwake,
                needsRestart = needsRestart
            ),
            onUpdate = viewModel::updateSettings,
            onToggleUnit = viewModel::toggleTemperatureUnit,
            onRequestPhotoPermission = viewModel::requestPhotoPermission,
            onUploadPhotos = viewModel::openPhotoUpload,
            onClearUploads = viewModel::clearUploadedPhotos,
            onManageWallpaperUrls = viewModel::openWallpaperUrls,
            wallpaperPreviewUrl = currentWallpaper?.url,
            onEnableKidsMode = { viewModel.setKidsModeEnabled(true) },
            onChangeKidsPin = viewModel::changeKidsPin,
            onOpenNowPlayingHelp = viewModel::openNowPlayingHelp,
            onOpenUpdates = viewModel::openUpdateDialog,
            onHomeWatch = viewModel::onHomeWatchPressed,
            onScreensaverTakeover = viewModel::onScreensaverTakeoverPressed,
            onKeepAwake = viewModel::onKeepAwakePressed,
            onDismiss = viewModel::closeSettings
        )
    }

    if (uploadUi.open) {
        PhotoUploadDialog(
            ui = uploadUi,
            onClearAll = viewModel::clearUploadedPhotos,
            onDismiss = viewModel::closePhotoUpload
        )
    }

    if (wallpaperUrlDialogOpen) {
        WallpaperUrlsDialog(
            urls = customWallpaperUrls,
            status = wallpaperUrlStatus,
            onAdd = viewModel::addWallpaperUrl,
            onRemove = viewModel::removeWallpaperUrl,
            onDismiss = viewModel::closeWallpaperUrls
        )
    }

    if (isAllAppsOpen) {
        CompositionLocalProvider(
        LocalCardWidth provides settings.tileWidthDp.dp,
        LocalCardShape provides settings.cardShape,
        LocalScreenEdge provides rememberScreenEdge()
    ) {
            AllAppsScreen(
                apps = apps,
                favorites = favorites,
                sortOrder = sortOrder,
                onCycleSort = viewModel::cycleSortOrder,
                onAppFocused = viewModel::setFocusedApp,
                onAppClicked = { if (viewModel.launchApp(it)) viewModel.closeAllApps() },
                onAppLongClicked = viewModel::openContextMenu,
                onDismiss = viewModel::closeAllApps
            )
        }
    }

    grantSetupFor?.let { feature ->
        GrantSetupDialog(
            title = feature.title,
            onEnable = viewModel::enableGrantedFeature,
            onDismiss = viewModel::closeGrantSetup
        )
    }

    if (isUpdateDialogOpen) {
        UpdateDialog(
            state = updateState,
            installedVersion = viewModel.installedVersion,
            onCheck = { viewModel.checkForUpdates(quiet = false) },
            onUpdate = viewModel::startUpdate,
            onOpenInstallPermission = viewModel::openInstallPermissionSettings,
            onDismiss = viewModel::closeUpdateDialog
        )
    }

    if (showNowPlayingHelp) {
        NowPlayingHelpDialog(
            onOpenSystemSettings = viewModel::openNotificationAccessSettings,
            onDismiss = viewModel::closeNowPlayingHelp
        )
    }

    pinDialog?.let { state ->
        when (state.purpose) {
            PinPurpose.CREATE -> CreatePinDialog(
                onCreated = viewModel::onPinCreated,
                onDismiss = viewModel::closePinDialog
            )
            PinPurpose.EXIT_KIDS_MODE -> PinEntryDialog(
                title = "Enter PIN to leave Kids mode",
                subtitle = null,
                error = state.error,
                onComplete = viewModel::submitExitPin,
                onDismiss = viewModel::closePinDialog
            )
        }
    }

    AnimatedVisibility(
        visible = screensaverVisible,
        enter = fadeIn(tween(1200)),
        exit = fadeOut(tween(300))
    ) {
        Screensaver(
            wallpaper = currentWallpaper,
            weather = weather,
            onNextWallpaper = viewModel::nextWallpaper,
            nowPlaying = nowPlaying,
            nightDim = settings.nightDim,
            contentScale = settings.wallpaperFit.toContentScale()
        )
    }
}

/** Clean layout: how long the top bar and dock stay after the last button press. */
private const val CLEAN_HIDE_MS = 6_000L

/**
 * Pure stateless Composable rendering the complete Fire TV launcher interface.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LauncherContent(
    apps: List<AppModel>,
    hiddenApps: List<AppModel>,
    favorites: Set<String>,
    favoriteApps: List<AppModel>,
    allAppsTile: AppModel,
    showAllAppsRow: Boolean,
    cleanLayout: Boolean,
    onOpenWifi: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onOpenLauncherSettings: () -> Unit,
    focusedApp: AppModel?,
    homeTick: Long,
    weather: WeatherData,
    savedCities: List<CityLocationModel>,
    searchResults: List<CityLocationModel>,
    isSearchingCities: Boolean,
    currentWallpaper: WallpaperModel?,
    wallpaperContentScale: ContentScale,
    sortOrder: AppSortOrder,
    searchQuery: String,
    isSearchActive: Boolean,
    filteredApps: List<AppModel>,
    nowPlaying: NowPlaying?,
    kidsMode: Boolean,
    updateState: UpdateState,
    onOpenUpdates: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrevious: () -> Unit,
    onOpenNowPlayingApp: () -> Unit,
    onSearchCities: (String) -> Unit,
    onClearCitySearch: () -> Unit,
    onSelectCity: (CityLocationModel) -> Unit,
    onToggleUnit: () -> Unit,
    onNextWallpaper: () -> Unit,
    onSelectSort: (AppSortOrder) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onAppFocused: (AppModel) -> Unit,
    onAppClicked: (AppModel) -> Unit,
    onAppLongClicked: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstItemFocusRequester = remember { FocusRequester() }
    var showRowSortDialog by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }

    // Smooth background gradient transition to the focused app's clamped accent color
    val targetAccentColor = focusedApp?.let { Color(it.accentColor) } ?: FireOrange
    val animatedAccentColor by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = tween(durationMillis = 450),
        label = "BackgroundAccentAnimation"
    )

    // Clean layout: the top bar and dock fade away after a few seconds without a button press, and
    // the first press only brings them back (so OK never opens an app you can't see)
    val clean = cleanLayout && !isSearchActive && searchQuery.isBlank()
    var chromeShown by remember { mutableStateOf(true) }
    var lastKeyAt by remember { mutableLongStateOf(0L) }
    var swallowKeyUp by remember { mutableStateOf<Key?>(null) }
    LaunchedEffect(clean, lastKeyAt) {
        if (!clean) {
            chromeShown = true
        } else {
            delay(if (lastKeyAt == 0L) 1500L else CLEAN_HIDE_MS)
            chromeShown = false
        }
    }
    val chromeAlpha by animateFloatAsState(if (chromeShown) 1f else 0f, tween(500), label = "chromeAlpha")
    val chromeModifier = Modifier.graphicsLayer { alpha = chromeAlpha }

    // Request initial focus on the first card once apps are loaded
    LaunchedEffect(apps.isNotEmpty()) {
        if (apps.isNotEmpty()) {
            delay(100)
            try {
                firstItemFocusRequester.requestFocus()
            } catch (_: Exception) {
                // Focus will settle on first user input if requester not yet attached
            }
        }
    }

    // When Home is pressed while launcher is open: refocus the first card
    LaunchedEffect(homeTick) {
        if (homeTick > 0L) {
            try {
                firstItemFocusRequester.requestFocus()
            } catch (_: Exception) {
                // Safe catch if layout is re-composing
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Layer 1: Fullscreen r/TVWallpapersSFW Wallpaper Background
        currentWallpaper?.let { wp ->
            Crossfade(
                targetState = wp.url,
                animationSpec = tween(700),
                label = "WallpaperFade"
            ) { imgUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imgUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = wp.title,
                    contentScale = wallpaperContentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Layer 2: TV Ambient Subtle Vignette & Scrim (Keeps 4K wallpaper highly visible while protecting text)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x550A0D14), // 33% top scrim for top bar text
                            Color(0x220A0D14), // 13% middle (wallpaper pops vibrantly)
                            Color(0x400A0D14), // 25% lower area
                            Color(0x770A0D14)  // 46% bottom scrim behind cards
                        ),
                        startY = 0f,
                        endY = 1000f
                    )
                )
        )

        // Layer 3: Dynamic Focused App Ambient Tint Accent
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedAccentColor.copy(alpha = 0.20f),
                            animatedAccentColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        )

        // Layer 4: Main content, laid out like couch-tv's Fire TV home: top bar, the date, weather and
        // a big clock mid-screen, and a dock of favourites along the bottom, so the wallpaper shows.
        val edge = LocalScreenEdge.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { e ->
                    if (!clean || e.key == Key.Back) return@onPreviewKeyEvent false
                    when (e.type) {
                        KeyEventType.KeyDown -> {
                            lastKeyAt = SystemClock.uptimeMillis()
                            if (!chromeShown) {
                                chromeShown = true
                                swallowKeyUp = e.key
                                true
                            } else swallowKeyUp == e.key // held key that woke it: keep swallowing
                        }
                        KeyEventType.KeyUp -> if (swallowKeyUp == e.key) { swallowKeyUp = null; true } else false
                        else -> false
                    }
                }
                .padding(top = 26.dp, bottom = 14.dp)
        ) {
            TopBar(
                searchQuery = searchQuery,
                isSearchActive = isSearchActive,
                onSearchQueryChange = onSearchQueryChange,
                onToggleSearch = onToggleSearch,
                onNextWallpaper = onNextWallpaper,
                kidsMode = kidsMode,
                onOpenWifi = onOpenWifi,
                onOpenSystemSettings = onOpenSystemSettings,
                onOpenLauncherSettings = onOpenLauncherSettings,
                leadingActions = { UpdateChip(state = updateState, onClick = onOpenUpdates) },
                modifier = chromeModifier.padding(horizontal = edge)
            )

            if (isSearchActive || searchQuery.isNotBlank()) {
                // Search: results replace the clock and the dock
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (filteredApps.isNotEmpty()) {
                        AppRow(
                            title = if (searchQuery.isBlank()) "All Apps (${filteredApps.size})" else "Search Results (${filteredApps.size})",
                            apps = filteredApps,
                            favorites = favorites,
                            firstItemFocusRequester = firstItemFocusRequester,
                            onAppFocused = onAppFocused,
                            onAppClicked = onAppClicked,
                            onAppLongClicked = onAppLongClicked
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = edge),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SearchOff,
                                contentDescription = "No results",
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No apps found matching \"$searchQuery\"",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Check the spelling or try searching for another app name",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                // Centre: date, weather and the big clock, plus what's playing
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.offset(y = (-20).dp)
                    ) {
                        HomeClock(
                            weather = weather,
                            onWeatherClick = { showCityDialog = true }
                        )
                        nowPlaying?.let { np ->
                            Spacer(modifier = Modifier.height(10.dp))
                            NowPlayingCard(
                                nowPlaying = np,
                                onPlayPause = onMediaPlayPause,
                                onNext = onMediaNext,
                                onPrevious = onMediaPrevious,
                                onOpenApp = onOpenNowPlayingApp
                            )
                        }
                    }
                }

                // Optional rows from Launcher Settings sit just above the dock
                if (showAllAppsRow) {
                    AppRow(
                        modifier = chromeModifier,
                        title = "All Apps",
                        apps = apps,
                        favorites = favorites,
                        sortOrder = sortOrder,
                        onOpenSortMenu = { showRowSortDialog = true },
                        onAppFocused = onAppFocused,
                        onAppClicked = onAppClicked,
                        onAppLongClicked = onAppLongClicked
                    )
                }
                if (hiddenApps.isNotEmpty()) {
                    AppRow(
                        modifier = chromeModifier,
                        title = "Hidden Apps (hold OK to unhide)",
                        apps = hiddenApps,
                        favorites = favorites,
                        onAppFocused = onAppFocused,
                        onAppClicked = onAppClicked,
                        onAppLongClicked = onAppLongClicked
                    )
                }

                // Bottom: the dock (favourites, then a round All Apps button at the right end)
                HomeDock(
                    apps = favoriteApps,
                    allAppsTile = allAppsTile,
                    favorites = favorites,
                    focusedApp = focusedApp,
                    hint = when {
                        kidsMode -> "Kids mode is on"
                        focusedApp?.isSystemShortcut == true -> "Press OK to see all your apps"
                        favoriteApps.isEmpty() -> "Open All Apps, hold OK on an app → Add to Favorites"
                        else -> "Hold OK or press ☰ for options"
                    },
                    firstItemFocusRequester = firstItemFocusRequester,
                    onAppFocused = onAppFocused,
                    onAppClicked = onAppClicked,
                    onAppLongClicked = onAppLongClicked,
                    modifier = chromeModifier
                )
            }
        }

        // Wallpaper credit: small and quiet in the bottom-right corner
        currentWallpaper?.let { wp ->
            Text(
                text = if (wp.isLocal) "Your photo · ${wp.title}" else "${wp.title} · r/TVWallpapersSFW",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = LocalScreenEdge.current, bottom = 12.dp)
                    .widthIn(max = 320.dp)
            )
        }

        if (showCityDialog) {
            CitySelectionDialog(
                currentCity = weather.city,
                savedCities = savedCities,
                searchResults = searchResults,
                isSearching = isSearchingCities,
                isCelsius = weather.isCelsius,
                onSearch = onSearchCities,
                onClearSearch = onClearCitySearch,
                onCitySelected = { city ->
                    onSelectCity(city)
                    showCityDialog = false
                },
                onToggleUnit = onToggleUnit,
                onDismiss = {
                    onClearCitySearch()
                    showCityDialog = false
                }
            )
        }

        // App Library Sort By Dialog when opened from the row header
        if (showRowSortDialog) {
            SortSelectionDialog(
                currentSort = sortOrder,
                onSelectSort = { order ->
                    onSelectSort(order)
                    showRowSortDialog = false
                },
                onDismiss = { showRowSortDialog = false }
            )
        }
    }
}
