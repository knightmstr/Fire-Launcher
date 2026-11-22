package com.knightmstr.onyxtv.ui.components

import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Popup
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import com.knightmstr.onyxtv.R
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.knightmstr.onyxtv.data.AppSortOrder
import com.knightmstr.onyxtv.data.CityLocationModel
import com.knightmstr.onyxtv.ui.theme.DarkSurface
import com.knightmstr.onyxtv.ui.theme.DarkSurfaceVariant
import com.knightmstr.onyxtv.ui.theme.FireAmber
import com.knightmstr.onyxtv.ui.theme.FireOrange
import com.knightmstr.onyxtv.ui.theme.TextPrimary
import com.knightmstr.onyxtv.ui.theme.TextSecondary

/**
 * Top bar:
 * - Left: logo, name and the current wallpaper (your photo or r/TVWallpapersSFW)
 * - Centre: the search field while searching (clock and weather are mid-screen, see HomeClock)
 * - Right: update chip, then icon buttons (next wallpaper, search, Wi-Fi, settings).
 *   The settings gear asks Launcher or Fire TV settings; in Kids mode Wi-Fi is hidden and the
 *   gear asks for the PIN.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopBar(
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    onSearchQueryChange: (String) -> Unit = {},
    onToggleSearch: (Boolean) -> Unit = {},
    onNextWallpaper: () -> Unit = {},
    kidsMode: Boolean = false,
    onOpenWifi: () -> Unit = {},
    onOpenSystemSettings: () -> Unit = {},
    onOpenLauncherSettings: () -> Unit = {},
    /** Extra items before the icon buttons (the update chip). */
    leadingActions: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSettingsChooser by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo and name; the wallpaper credit sits quietly at the bottom right instead
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_logo_mark),
                    contentDescription = null,
                    // The mark fills the middle 30/48 of its box, so this shows a ~22dp "O"
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "OnyxTV",
                    style = TextStyle(shadow = Shadow(Color(0x66000000), blurRadius = 12f)),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    color = Color.White.copy(alpha = 0.92f)
                )
            }

            // Centre: the search field while searching (the clock and weather live mid-screen)
            if (isSearchActive) {
                GlobalSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onClose = { onToggleSearch(false) },
                    modifier = Modifier.weight(1f, fill = false).padding(horizontal = 24.dp)
                )
            }

            // Right: update chip, then the round icon buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                leadingActions()
                TopIconButton(Icons.Rounded.Wallpaper, "Next wallpaper", onNextWallpaper)
                TopIconButton(
                    if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                    if (isSearchActive) "Close search" else "Search apps"
                ) { onToggleSearch(!isSearchActive) }
                if (!kidsMode) TopIconButton(Icons.Rounded.Wifi, "Wi-Fi", onOpenWifi)
                // One gear with a small drop-down: Launcher or Fire TV settings.
                // In Kids mode it goes straight to the PIN.
                Box {
                    TopIconButton(
                        Icons.Rounded.Settings,
                        if (kidsMode) "Leave Kids mode" else "Settings"
                    ) { if (kidsMode) onOpenLauncherSettings() else showSettingsChooser = true }
                    if (showSettingsChooser) {
                        SettingsDropDown(
                            onLauncherSettings = { showSettingsChooser = false; onOpenLauncherSettings() },
                            onFireTvSettings = { showSettingsChooser = false; onOpenSystemSettings() },
                            onDismiss = { showSettingsChooser = false }
                        )
                    }
                }
            }
        }

    }
}

/** Round icon-only button used across the top bar (wallpaper, search, Wi-Fi, settings). */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    val shape = CircleShape
    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.15f),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), shape = shape),
            focusedBorder = Border(border = BorderStroke(2.dp, Color.White), shape = shape)
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(elevationColor = FireOrange.copy(alpha = 0.7f), elevation = 14.dp)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DarkSurface.copy(alpha = 0.85f),
            focusedContainerColor = DarkSurfaceVariant
        )
    ) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = description, tint = FireAmber, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Search field shown while searching apps. It takes focus as soon as it opens and pops up the
 * on-screen keyboard; OK on it brings the keyboard back. Down moves to the results.
 * (Was a clickable Surface wrapping the field, which kept focus itself so OK did nothing.)
 */
@Composable
fun GlobalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { focus.requestFocus() }
        keyboard?.show()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .width(360.dp)
            .height(40.dp)
            .background(DarkSurfaceVariant, shape)
            .border(
                BorderStroke(if (focused) 2.dp else 1.5.dp, if (focused) Color.White else FireOrange.copy(alpha = 0.7f)),
                shape
            )
            .padding(horizontal = 14.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = FireOrange,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(text = "Search installed apps…", color = TextSecondary, fontSize = 13.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(FireOrange),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboard?.hide()
                    focusManager.moveFocus(FocusDirection.Down)
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .onFocusChanged { focused = it.isFocused }
                    .onPreviewKeyEvent { e ->
                        // OK on the field (keyboard closed): open the keyboard again
                        val ok = e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter
                        if (ok && e.type == KeyEventType.KeyUp) { keyboard?.show(); true } else ok
                    }
            )
        }
    }
}

/**
 * Weather location picker: search any city (Open-Meteo geocoding) or pick a saved one.
 * Built on [TvDialogFrame] / [TvMenuItem] so every row is reachable with the D-pad.
 */
@Composable
fun CitySelectionDialog(
    currentCity: String,
    savedCities: List<CityLocationModel>,
    searchResults: List<CityLocationModel>,
    isSearching: Boolean,
    isCelsius: Boolean,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onCitySelected: (CityLocationModel) -> Unit,
    onToggleUnit: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var fieldFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val firstCity = rememberInitialFocus()
    val searching = searchQuery.trim().length >= 2
    val displayList = if (searching) searchResults else savedCities
    val fieldShape = RoundedCornerShape(12.dp)

    TvDialogFrame(
        title = "Weather location",
        subtitle = "Search any city, or pick a saved one",
        accentColor = FireOrange,
        onDismiss = onDismiss,
        width = 520.dp
    ) {
        // Search box: press OK to type with the on-screen keyboard, then Down to the results
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceVariant, fieldShape)
                .border(
                    BorderStroke(if (fieldFocused) 2.dp else 1.dp, if (fieldFocused) Color.White else FireOrange.copy(alpha = 0.5f)),
                    fieldShape
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = FireOrange, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (searchQuery.isEmpty()) {
                    Text("Search a city (e.g. Darwin, Tokyo)…", color = TextSecondary, fontSize = 13.sp)
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.isBlank()) onClearSearch() else onSearch(it)
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    cursorBrush = SolidColor(FireOrange),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { fieldFocused = it.isFocused }
                )
            }
        }

        if (searchQuery.isNotEmpty()) {
            TvMenuItem(
                title = "Clear search",
                icon = Icons.Rounded.Close,
                iconTint = TextSecondary,
                onClick = {
                    searchQuery = ""
                    onClearSearch()
                }
            )
        }

        Text(
            text = if (searching) "Search results" else "Saved locations",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = FireAmber
        )

        when {
            searching && isSearching -> Text("Searching…", fontSize = 12.sp, color = TextSecondary)
            searching && displayList.isEmpty() ->
                Text("No cities found for \"$searchQuery\"", fontSize = 12.sp, color = TextSecondary)
            else -> displayList.forEachIndexed { index, city ->
                val isSelected = city.name.equals(currentCity, ignoreCase = true)
                TvMenuItem(
                    title = city.name,
                    subtitle = if (!city.admin1.isNullOrBlank()) "${city.admin1}, ${city.country}" else city.country,
                    icon = if (isSelected) Icons.Rounded.Check else Icons.Rounded.Public,
                    iconTint = if (isSelected) FireAmber else TextSecondary,
                    trailing = if (isSelected) "Active" else null,
                    modifier = if (index == 0 && !searching) Modifier.focusRequester(firstCity) else Modifier,
                    onClick = { onCitySelected(city) }
                )
            }
        }

        TvMenuItem(
            title = "Temperature unit",
            icon = Icons.Rounded.Thermostat,
            iconTint = FireOrange,
            trailing = if (isCelsius) "°C" else "°F",
            onClick = onToggleUnit
        )
    }
}

/**
 * Small menu that drops down from the settings gear: Onyx TV's own settings or the Fire TV's.
 * A focusable popup, so the remote moves inside it and Back closes it.
 */
@Composable
private fun SettingsDropDown(
    onLauncherSettings: () -> Unit,
    onFireTvSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val first = rememberInitialFocus()
    val gap = with(LocalDensity.current) { 50.dp.roundToPx() }
    val shape = RoundedCornerShape(16.dp)
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, gap),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = Modifier
                .width(290.dp)
                .background(Color(0xF2141824), shape)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), shape)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TvMenuItem(
                title = "Launcher settings",
                subtitle = "Wallpapers, tiles, Kids mode, updates",
                icon = Icons.Rounded.Tune,
                iconTint = FireAmber,
                modifier = Modifier.focusRequester(first),
                onClick = onLauncherSettings
            )
            TvMenuItem(
                title = "Fire TV settings",
                subtitle = "Network, display, sound, apps",
                icon = Icons.Rounded.Tv,
                iconTint = FireOrange,
                onClick = onFireTvSettings
            )
        }
    }
}

/** Sort order picker for the app library (A–Z, Recent, Frequent, My Order). */
@Composable
fun SortSelectionDialog(
    currentSort: AppSortOrder,
    onSelectSort: (AppSortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    val firstItem = rememberInitialFocus()
    TvDialogFrame(
        title = "Sort apps by",
        subtitle = null,
        accentColor = FireAmber,
        onDismiss = onDismiss
    ) {
        AppSortOrder.entries.forEachIndexed { index, order ->
            val selected = order == currentSort
            TvMenuItem(
                title = order.title,
                subtitle = order.subtitle,
                icon = if (selected) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.Sort,
                iconTint = if (selected) FireAmber else TextSecondary,
                trailing = if (selected) "Current" else null,
                modifier = if (index == 0) Modifier.focusRequester(firstItem) else Modifier,
                onClick = { onSelectSort(order) }
            )
        }
    }
}
