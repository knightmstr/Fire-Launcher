package com.knightmstr.firelauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
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
import com.knightmstr.firelauncher.data.AppModel
import com.knightmstr.firelauncher.data.AppSortOrder
import com.knightmstr.firelauncher.data.CityLocationModel
import com.knightmstr.firelauncher.data.WallpaperModel
import com.knightmstr.firelauncher.data.WeatherData
import com.knightmstr.firelauncher.data.LauncherPrefsStore
import com.knightmstr.firelauncher.media.NowPlaying
import com.knightmstr.firelauncher.ui.components.AllAppsScreen
import com.knightmstr.firelauncher.ui.components.AppContextMenuDialog
import com.knightmstr.firelauncher.ui.components.LocalCardWidth
import com.knightmstr.firelauncher.ui.components.LocalScreenEdge
import com.knightmstr.firelauncher.ui.components.rememberScreenEdge
import com.knightmstr.firelauncher.ui.components.CreatePinDialog
import com.knightmstr.firelauncher.ui.components.NowPlayingCard
import com.knightmstr.firelauncher.ui.components.NowPlayingHelpDialog
import com.knightmstr.firelauncher.ui.components.PinEntryDialog
import com.knightmstr.firelauncher.ui.components.QuickButtonsBar
import com.knightmstr.firelauncher.ui.components.SettingsStatus
import com.knightmstr.firelauncher.ui.components.UpdateChip
import com.knightmstr.firelauncher.ui.components.UpdateDialog
import com.knightmstr.firelauncher.update.UpdateState
import com.knightmstr.firelauncher.ui.components.LauncherSettingsDialog
import com.knightmstr.firelauncher.ui.components.RenameAppDialog
import com.knightmstr.firelauncher.ui.components.Screensaver
import com.knightmstr.firelauncher.ui.components.AppRow
import com.knightmstr.firelauncher.ui.components.HeroSection
import com.knightmstr.firelauncher.ui.components.SortSelectionDialog
import com.knightmstr.firelauncher.ui.components.TopBar
import com.knightmstr.firelauncher.ui.theme.DarkBackground
import com.knightmstr.firelauncher.ui.theme.FireAmber
import com.knightmstr.firelauncher.ui.theme.FireOrange
import com.knightmstr.firelauncher.ui.theme.TextPrimary
import com.knightmstr.firelauncher.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Main stateless container collecting ViewModel state, managing D-pad focus,
 * animating dynamic background accent gradients, adaptive screen-edge margins,
 * auto-changing r/TVWallpapersSFW 30-minute wallpapers, and intercepting Back/Home events.
 */
@Composable
fun LauncherRoot(
    viewModel: LauncherViewModel,
    onRequestVoiceSearch: () -> Unit = {},
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
    val quickButtonApps by viewModel.quickButtonApps.collectAsStateWithLifecycle()
    val quickButtonKeys by viewModel.quickButtonKeys.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val kidsMode by viewModel.kidsModeEnabled.collectAsStateWithLifecycle()
    val pinDialog by viewModel.pinDialog.collectAsStateWithLifecycle()
    val showNowPlayingHelp by viewModel.showNowPlayingHelp.collectAsStateWithLifecycle()
    val hiddenAppCount by viewModel.hiddenAppCount.collectAsStateWithLifecycle()
    val localPhotoCount by viewModel.localPhotoCount.collectAsStateWithLifecycle()
    val hasPhotoPermission by viewModel.hasPhotoPermission.collectAsStateWithLifecycle()
    val hasKidsPin by viewModel.hasKidsPin.collectAsStateWithLifecycle()
    val kidsAppCount by viewModel.kidsAppCount.collectAsStateWithLifecycle()
    val hasNowPlayingAccess by viewModel.hasNowPlayingAccess.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val isUpdateDialogOpen by viewModel.isUpdateDialogOpen.collectAsStateWithLifecycle()
    val isAllAppsOpen by viewModel.isAllAppsOpen.collectAsStateWithLifecycle()

    // Pause the screensaver timer while any dialog has the keys
    val anyDialogOpen = contextMenuApp != null || renameTarget != null || isSettingsOpen ||
        pinDialog != null || showNowPlayingHelp || isUpdateDialogOpen || isAllAppsOpen
    LaunchedEffect(anyDialogOpen) { viewModel.setDialogOpen(anyDialogOpen) }

    // Back closes an open search; otherwise it is absorbed so the launcher never exits
    BackHandler(enabled = isSearchActive) { viewModel.setSearchActive(false) }

    CompositionLocalProvider(
        LocalCardWidth provides settings.cardSize.widthDp.dp,
        LocalScreenEdge provides rememberScreenEdge()
    ) {
        LauncherContent(
            apps = apps,
            hiddenApps = if (settings.showHiddenApps) hiddenApps else emptyList(),
            favorites = favorites,
            favoriteApps = favoriteApps,
            allAppsTile = viewModel.allAppsTile,
            showAllAppsRow = settings.showAllAppsRow,
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
            sortOrder = sortOrder,
            searchQuery = searchQuery,
            isSearchActive = isSearchActive,
            filteredApps = filteredApps,
            quickButtonApps = quickButtonApps,
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
            onRequestVoiceSearch = onRequestVoiceSearch,
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
            isQuickButton = app.packageName in quickButtonKeys,
            quickButtonsFull = quickButtonKeys.size >= LauncherPrefsStore.MAX_QUICK_BUTTONS,
            onLaunchApp = { viewModel.launchApp(app) },
            onToggleFavorite = { viewModel.toggleFavorite(app) },
            onMoveFavorite = { delta -> viewModel.moveFavorite(app, delta) },
            onMoveInAllApps = { delta -> viewModel.moveInAllApps(app, delta) },
            onToggleQuickButton = { viewModel.toggleQuickButton(app) },
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
                }
            ),
            onUpdate = viewModel::updateSettings,
            onToggleUnit = viewModel::toggleTemperatureUnit,
            onRequestPhotoPermission = viewModel::requestPhotoPermission,
            onEnableKidsMode = { viewModel.setKidsModeEnabled(true) },
            onChangeKidsPin = viewModel::changeKidsPin,
            onOpenNowPlayingHelp = viewModel::openNowPlayingHelp,
            onOpenUpdates = viewModel::openUpdateDialog,
            onDismiss = viewModel::closeSettings
        )
    }

    if (isAllAppsOpen) {
        CompositionLocalProvider(
        LocalCardWidth provides settings.cardSize.widthDp.dp,
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
            onNextWallpaper = viewModel::nextWallpaper
        )
    }
}

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
    sortOrder: AppSortOrder,
    searchQuery: String,
    isSearchActive: Boolean,
    filteredApps: List<AppModel>,
    quickButtonApps: List<AppModel>,
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
    onRequestVoiceSearch: () -> Unit,
    onAppFocused: (AppModel) -> Unit,
    onAppClicked: (AppModel) -> Unit,
    onAppLongClicked: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val columnState = rememberLazyListState()
    val firstItemFocusRequester = remember { FocusRequester() }
    var showRowSortDialog by remember { mutableStateOf(false) }

    // Smooth background gradient transition to the focused app's clamped accent color
    val targetAccentColor = focusedApp?.let { Color(it.accentColor) } ?: FireOrange
    val animatedAccentColor by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = tween(durationMillis = 450),
        label = "BackgroundAccentAnimation"
    )

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

    // When Home is pressed while launcher is open: scroll to top and refocus first card
    LaunchedEffect(homeTick) {
        if (homeTick > 0L) {
            columnState.scrollToItem(0)
            try {
                firstItemFocusRequester.requestFocus()
            } catch (_: Exception) {
                // Safe catch if layout is re-composing
            }
        }
    }

    // Overscan-safe layout with 48dp horizontal margins
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
                    contentScale = ContentScale.Crop,
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

        // Layer 4: Main content. Full width so the app rows run edge to edge; the top bar, header
        // and the start of each row use an adaptive side margin (LocalScreenEdge, ~3.5% of width).
        val edge = LocalScreenEdge.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 26.dp, bottom = 20.dp)
        ) {
            // Top bar: App name, Live Open-Meteo Weather with Search, Sort by Menu, Next Wallpaper & live clock/date
            TopBar(
                weather = weather,
                savedCities = savedCities,
                searchResults = searchResults,
                isSearchingCities = isSearchingCities,
                onSearchCities = onSearchCities,
                onClearCitySearch = onClearCitySearch,
                onSelectCity = onSelectCity,
                onToggleUnit = onToggleUnit,
                searchQuery = searchQuery,
                isSearchActive = isSearchActive,
                onSearchQueryChange = onSearchQueryChange,
                onToggleSearch = onToggleSearch,
                onRequestVoiceSearch = onRequestVoiceSearch,
                onNextWallpaper = onNextWallpaper,
                wallpaperInfo = currentWallpaper?.let { wp ->
                    if (wp.isLocal) "Your photo • ${wp.title}" else "${wp.title} • r/TVWallpapersSFW"
                },
                kidsMode = kidsMode,
                onOpenWifi = onOpenWifi,
                onOpenSystemSettings = onOpenSystemSettings,
                onOpenLauncherSettings = onOpenLauncherSettings,
                modifier = Modifier.padding(horizontal = edge)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Hero section: focused app title and hints, now playing, quick buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = edge),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    HeroSection(
                        focusedApp = focusedApp,
                        isFavorite = focusedApp != null && favorites.contains(focusedApp.packageName),
                        kidsMode = kidsMode
                    )
                }

                nowPlaying?.let { np ->
                    NowPlayingCard(
                        nowPlaying = np,
                        onPlayPause = onMediaPlayPause,
                        onNext = onMediaNext,
                        onPrevious = onMediaPrevious,
                        onOpenApp = onOpenNowPlayingApp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                UpdateChip(state = updateState, onClick = onOpenUpdates)
                if (updateState is UpdateState.Available || updateState is UpdateState.Downloading ||
                    updateState is UpdateState.NeedsInstallPermission
                ) {
                    Spacer(modifier = Modifier.width(12.dp))
                }

                QuickButtonsBar(
                    apps = quickButtonApps,
                    onClick = onAppClicked,
                    onLongClick = onAppLongClicked
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable rows of apps or filtered search results
            LazyColumn(
                state = columnState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isSearchActive || searchQuery.isNotBlank()) {
                    // Global Search Mode
                    if (filteredApps.isNotEmpty()) {
                        item(key = "search_results") {
                            AppRow(
                                title = if (searchQuery.isBlank()) "All Apps (${filteredApps.size})" else "Search Results (${filteredApps.size})",
                                apps = filteredApps,
                                favorites = favorites,
                                firstItemFocusRequester = firstItemFocusRequester,
                                onAppFocused = onAppFocused,
                                onAppClicked = onAppClicked,
                                onAppLongClicked = onAppLongClicked
                            )
                        }
                    } else {
                        // Empty search results state
                        item(key = "empty_search") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = edge, vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
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
                    // Standard Home Launcher Rows
                    // Row 1: "All Apps" tile followed by favourites (always shown)
                    item(key = "row_favorites") {
                        AppRow(
                            title = if (favoriteApps.isEmpty()) "Home (open All Apps, hold OK on an app → Add to Favorites)"
                            else "Favorites",
                            apps = listOf(allAppsTile) + favoriteApps,
                            favorites = favorites,
                            firstItemFocusRequester = firstItemFocusRequester,
                            onAppFocused = onAppFocused,
                            onAppClicked = onAppClicked,
                            onAppLongClicked = onAppLongClicked
                        )
                    }

                    // Row 2: All Apps (optional; off by default, the tile opens a full grid instead)
                    if (showAllAppsRow) {
                        item(key = "row_all_apps") {
                            AppRow(
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
                    }

                    // Row 3: Hidden apps (only when enabled in Launcher Settings)
                    if (hiddenApps.isNotEmpty()) {
                        item(key = "row_hidden") {
                            AppRow(
                                title = "Hidden Apps (hold OK to unhide)",
                                apps = hiddenApps,
                                favorites = favorites,
                                onAppFocused = onAppFocused,
                                onAppClicked = onAppClicked,
                                onAppLongClicked = onAppLongClicked
                            )
                        }
                    }
                }

                // Bottom safety spacer for overscan visibility
                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
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
