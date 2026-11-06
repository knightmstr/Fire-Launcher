package com.example.firelauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.firelauncher.data.AppModel
import com.example.firelauncher.ui.components.AppRow
import com.example.firelauncher.ui.components.HeroSection
import com.example.firelauncher.ui.components.TopBar
import com.example.firelauncher.ui.theme.DarkBackground
import com.example.firelauncher.ui.theme.FireOrange
import com.example.firelauncher.ui.theme.TextPrimary
import com.example.firelauncher.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Main stateless container collecting ViewModel state, managing D-pad focus,
 * animating dynamic background accent gradients, handling TV overscan padding,
 * and intercepting Back/Home events.
 */
@Composable
fun LauncherRoot(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    // Intercept Back button: if search is active, close search; otherwise ignore
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    BackHandler {
        if (isSearchActive) {
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
        availableCities = viewModel.availableCities,
        searchQuery = searchQuery,
        isSearchActive = isSearchActive,
        filteredApps = filteredApps,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onToggleSearch = { viewModel.setSearchActive(it) },
        onSelectCity = { viewModel.selectCity(it) },
        onToggleUnit = { viewModel.toggleTemperatureUnit() },
        onAppFocused = { viewModel.setFocusedApp(it) },
        onAppClicked = { viewModel.launchApp(it) },
        onAppLongClicked = { viewModel.toggleFavorite(it) },
        modifier = modifier
    )
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
    weather: com.example.firelauncher.data.WeatherData,
    availableCities: List<String>,
    searchQuery: String,
    isSearchActive: Boolean,
    filteredApps: List<AppModel>,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onSelectCity: (String) -> Unit,
    onToggleUnit: () -> Unit,
    onAppFocused: (AppModel) -> Unit,
    onAppClicked: (AppModel) -> Unit,
    onAppLongClicked: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val columnState = rememberTvLazyListState()
    val firstItemFocusRequester = remember { FocusRequester() }

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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedAccentColor.copy(alpha = 0.32f),
                        animatedAccentColor.copy(alpha = 0.12f),
                        DarkBackground.copy(alpha = 0.95f),
                        DarkBackground
                    ),
                    startY = 0f,
                    endY = 900f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp) // 48dp TV overscan safe padding
                .padding(top = 28.dp, bottom = 20.dp)
        ) {
            // Top bar: App name, Weather widget, Global Search & live clock/date
            TopBar(
                weather = weather,
                availableCities = availableCities,
                searchQuery = searchQuery,
                isSearchActive = isSearchActive,
                onSearchQueryChange = onSearchQueryChange,
                onToggleSearch = onToggleSearch,
                onSelectCity = onSelectCity,
                onToggleUnit = onToggleUnit
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hero section: Focused app title and dynamic hints
            HeroSection(
                focusedApp = focusedApp,
                isFavorite = focusedApp != null && favorites.contains(focusedApp.packageName)
            )

            Spacer(modifier = Modifier.height(12.dp))

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

                    // Row 2: All Apps (alphabetical)
                    item(key = "row_all_apps") {
                        AppRow(
                            title = "All Apps",
                            apps = apps,
                            favorites = favorites,
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
    }
}
