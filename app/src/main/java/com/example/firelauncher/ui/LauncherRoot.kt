package com.example.firelauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
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
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.firelauncher.data.AppModel
import com.example.firelauncher.data.AppSortOrder
import com.example.firelauncher.data.CityLocationModel
import com.example.firelauncher.data.WallpaperModel
import com.example.firelauncher.data.WeatherData
import com.example.firelauncher.ui.components.AppContextMenuDialog
import com.example.firelauncher.ui.components.AppRow
import com.example.firelauncher.ui.components.HeroSection
import com.example.firelauncher.ui.components.SortSelectionDialog
import com.example.firelauncher.ui.components.TopBar
import com.example.firelauncher.ui.theme.DarkBackground
import com.example.firelauncher.ui.theme.FireAmber
import com.example.firelauncher.ui.theme.FireOrange
import com.example.firelauncher.ui.theme.TextPrimary
import com.example.firelauncher.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Main stateless container collecting ViewModel state, managing D-pad focus,
 * animating dynamic background accent gradients, handling TV overscan padding,
 * auto-changing r/TVWallpapersSFW 30-minute wallpapers, and intercepting Back/Home events.
 */
@Composable
fun LauncherRoot(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    var contextMenuApp by remember { mutableStateOf<AppModel?>(null) }

    // Intercept Back button: if context menu is open, close it; if search is active, close search; otherwise ignore
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    BackHandler {
        if (contextMenuApp != null) {
            contextMenuApp = null
        } else if (isSearchActive) {
            viewModel.setSearchActive(false)
        }
    }

    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val favoriteApps by viewModel.favoriteApps.collectAsStateWithLifecycle()
    val systemShortcuts by viewModel.systemShortcuts.collectAsStateWithLifecycle()
    val focusedApp by viewModel.focusedApp.collectAsStateWithLifecycle()
    val homeTick by viewModel.homeTick.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val savedCities by viewModel.savedCities.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearchingCities by viewModel.isSearchingCities.collectAsStateWithLifecycle()
    val currentWallpaper by viewModel.currentWallpaper.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()

    LauncherContent(
        apps = apps,
        favorites = favorites,
        favoriteApps = favoriteApps,
        systemShortcuts = systemShortcuts,
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
        onSearchCities = { viewModel.searchCities(it) },
        onClearCitySearch = { viewModel.clearCitySearch() },
        onSelectCity = { viewModel.selectCity(it) },
        onToggleUnit = { viewModel.toggleTemperatureUnit() },
        onNextWallpaper = { viewModel.nextWallpaper() },
        onSelectSort = { viewModel.setSortOrder(it) },
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onToggleSearch = { viewModel.setSearchActive(it) },
        onAppFocused = { viewModel.setFocusedApp(it) },
        onAppClicked = { viewModel.launchApp(it) },
        onAppLongClicked = { app ->
            if (!app.isSystemShortcut) {
                contextMenuApp = app
            } else {
                viewModel.toggleFavorite(app)
            }
        },
        modifier = modifier
    )

    // Context Menu for App Info / Force Stop / Favorite toggle
    contextMenuApp?.let { app ->
        AppContextMenuDialog(
            app = app,
            isFavorite = favorites.contains(app.packageName),
            onToggleFavorite = { viewModel.toggleFavorite(app) },
            onOpenAppDetails = { viewModel.openAppDetails(app) },
            onLaunchApp = { viewModel.launchApp(app) },
            onDismiss = { contextMenuApp = null }
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
    favorites: Set<String>,
    favoriteApps: List<AppModel>,
    systemShortcuts: List<AppModel>,
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
    val columnState = rememberTvLazyListState()
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

        // Layer 4: Main Content with 48dp Overscan Safe Margins
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp) // 48dp TV overscan safe padding
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
                sortOrder = sortOrder,
                onSelectSort = onSelectSort,
                searchQuery = searchQuery,
                isSearchActive = isSearchActive,
                onSearchQueryChange = onSearchQueryChange,
                onToggleSearch = onToggleSearch,
                onNextWallpaper = onNextWallpaper,
                wallpaperTitle = currentWallpaper?.title
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Hero section: Focused app title, dynamic hints, and active wallpaper badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    HeroSection(
                        focusedApp = focusedApp,
                        isFavorite = focusedApp != null && favorites.contains(focusedApp.packageName)
                    )
                }

                // Discreet Wallpaper Attribution Chip
                currentWallpaper?.let { wp ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x66181C28))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.Wallpaper,
                            contentDescription = "Wallpaper",
                            tint = FireAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${wp.title.take(28)}... • r/TVWallpapersSFW",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable rows of apps or filtered search results
            TvLazyColumn(
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
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Rounded.SearchOff,
                                    contentDescription = "No results",
                                    tint = TextSecondary,
                                    modifier = Modifier.height(48.dp)
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
                    // Row 1: Favorites (Only visible when non-empty)
                    if (favoriteApps.isNotEmpty()) {
                        item(key = "row_favorites") {
                            AppRow(
                                title = "Favorites",
                                apps = favoriteApps,
                                favorites = favorites,
                                firstItemFocusRequester = firstItemFocusRequester,
                                onAppFocused = onAppFocused,
                                onAppClicked = onAppClicked,
                                onAppLongClicked = onAppLongClicked
                            )
                        }
                    }

                    // Row 2: All Apps (with sort by menu trigger)
                    item(key = "row_all_apps") {
                        AppRow(
                            title = "All Apps",
                            apps = apps,
                            favorites = favorites,
                            sortOrder = sortOrder,
                            onOpenSortMenu = { showRowSortDialog = true },
                            firstItemFocusRequester = if (favoriteApps.isEmpty()) firstItemFocusRequester else null,
                            onAppFocused = onAppFocused,
                            onAppClicked = onAppClicked,
                            onAppLongClicked = onAppLongClicked
                        )
                    }

                    // Row 3: System Shortcuts
                    item(key = "row_system") {
                        AppRow(
                            title = "System",
                            apps = systemShortcuts,
                            favorites = favorites,
                            onAppFocused = onAppFocused,
                            onAppClicked = onAppClicked,
                            onAppLongClicked = onAppLongClicked
                        )
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
