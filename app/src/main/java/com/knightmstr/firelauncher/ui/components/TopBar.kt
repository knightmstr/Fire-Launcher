package com.knightmstr.firelauncher.ui.components

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.knightmstr.firelauncher.data.AppSortOrder
import com.knightmstr.firelauncher.data.CityLocationModel
import com.knightmstr.firelauncher.data.WeatherData
import com.knightmstr.firelauncher.ui.theme.DarkSurface
import com.knightmstr.firelauncher.ui.theme.DarkSurfaceVariant
import com.knightmstr.firelauncher.ui.theme.FireAmber
import com.knightmstr.firelauncher.ui.theme.FireOrange
import com.knightmstr.firelauncher.ui.theme.TextPrimary
import com.knightmstr.firelauncher.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Top bar displaying:
 * - Launcher brand on the left
 * - Focusable Live Weather widget in the center with Open-Meteo city search
 * - Wallpaper skip & Search icon buttons
 * - Dynamic clock (12h/24h) and formatted calendar date on the right.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopBar(
    weather: WeatherData,
    savedCities: List<CityLocationModel>,
    searchResults: List<CityLocationModel>,
    isSearchingCities: Boolean,
    onSearchCities: (String) -> Unit,
    onClearCitySearch: () -> Unit,
    onSelectCity: (CityLocationModel) -> Unit,
    onToggleUnit: () -> Unit,
    sortOrder: AppSortOrder = AppSortOrder.ALPHABETICAL,
    onSelectSort: (AppSortOrder) -> Unit = {},
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    onSearchQueryChange: (String) -> Unit = {},
    onToggleSearch: (Boolean) -> Unit = {},
    onRequestVoiceSearch: () -> Unit = {},
    onNextWallpaper: () -> Unit = {},
    wallpaperTitle: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTimeString by remember { mutableStateOf(getFormattedTime(context)) }
    var currentDateString by remember { mutableStateOf(getFormattedDate()) }
    var showCityDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = getFormattedTime(context)
            currentDateString = getFormattedDate()
            delay(1000L)
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand logo & title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(FireAmber, FireOrange))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "FireLauncher",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                    if (wallpaperTitle != null) {
                        Text(
                            text = "r/TVWallpapersSFW",
                            fontSize = 9.sp,
                            color = FireAmber,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Center: Weather Widget or Search Input
            if (isSearchActive) {
                // Expanded TV Search Bar
                GlobalSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onRequestVoiceSearch = onRequestVoiceSearch,
                    onClose = { onToggleSearch(false) },
                    modifier = Modifier.weight(1f, fill = false).padding(horizontal = 24.dp)
                )
            } else {
                // Top Weather Widget (Live weather for selected city)
                WeatherWidget(
                    weather = weather,
                    onClick = { showCityDialog = true },
                    onToggleUnit = onToggleUnit
                )
            }

            // Right: Sort Menu + Next Wallpaper + Search Trigger Button + Live Clock & Date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Dedicated Library Sort by Trigger Button
                SortTriggerButton(
                    currentSort = sortOrder,
                    onClick = { showSortDialog = true }
                )

                // Next Wallpaper Button
                WallpaperTriggerButton(
                    onClick = onNextWallpaper,
                    title = "Change Wallpaper (30m auto)"
                )

                // Dedicated Search Icon Trigger
                SearchTriggerButton(
                    isActive = isSearchActive,
                    onClick = { onToggleSearch(!isSearchActive) }
                )

                // Live clock and date
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = currentTimeString,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = currentDateString,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Live City Search & Selection Dialog
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

        // App Library Sort By Selection Dialog
        if (showSortDialog) {
            SortSelectionDialog(
                currentSort = sortOrder,
                onSelectSort = { order ->
                    onSelectSort(order)
                    showSortDialog = false
                },
                onDismiss = { showSortDialog = false }
            )
        }
    }
}

/**
 * Dedicated TV-Material focusable Wallpaper trigger button in the top bar.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WallpaperTriggerButton(
    onClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier
) {
    val shape = CircleShape

    Surface(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.15f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = shape
            )
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevationColor = FireOrange.copy(alpha = 0.7f),
                elevation = 14.dp
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DarkSurface.copy(alpha = 0.85f),
            focusedContainerColor = DarkSurfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Wallpaper,
                contentDescription = title,
                tint = FireAmber,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Dedicated TV-Material focusable Search Icon button in the top bar.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchTriggerButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = CircleShape

    Surface(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.15f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, if (isActive) FireOrange else Color.White.copy(alpha = 0.15f)),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = shape
            )
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevationColor = FireOrange.copy(alpha = 0.8f),
                elevation = 16.dp
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isActive) FireOrange.copy(alpha = 0.25f) else DarkSurface.copy(alpha = 0.85f),
            focusedContainerColor = DarkSurfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActive) Icons.Rounded.Close else Icons.Rounded.Search,
                contentDescription = if (isActive) "Close Search" else "Search Apps",
                tint = if (isActive) FireOrange else TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Expanded Search Bar with real-time text input and clear button.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GlobalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onRequestVoiceSearch: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)

    Surface(
        onClick = {},
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DarkSurfaceVariant
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.5.dp, FireOrange.copy(alpha = 0.7f)),
                shape = shape
            )
        ),
        modifier = modifier.width(360.dp).height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = FireOrange,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search installed apps...",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(FireOrange),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Rounded.Mic,
                contentDescription = "Voice search",
                tint = FireAmber,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onRequestVoiceSearch() }
            )

            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear search",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onQueryChange("") }
                )
            }
        }
    }
}

/**
 * TV-Material focusable weather surface displaying weather icon, temp, condition, and selected city.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WeatherWidget(
    weather: WeatherData,
    onClick: () -> Unit,
    onToggleUnit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)

    Surface(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = shape
            )
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevationColor = FireOrange.copy(alpha = 0.7f),
                elevation = 14.dp
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DarkSurface.copy(alpha = 0.85f),
            focusedContainerColor = DarkSurfaceVariant
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            // Weather icon
            val weatherIcon: ImageVector = when (weather.iconType) {
                "clear" -> Icons.Rounded.WbSunny
                "partly_cloudy" -> Icons.Rounded.WbCloudy
                "cloudy" -> Icons.Rounded.Cloud
                "rain" -> Icons.Rounded.WaterDrop
                "thunder" -> Icons.Rounded.Thunderstorm
                else -> Icons.Rounded.WbSunny
            }
            val iconTint = when (weather.iconType) {
                "clear" -> FireAmber
                "partly_cloudy" -> Color(0xFF90E0EF)
                "rain" -> Color(0xFF00B4D8)
                else -> Color(0xFFE0E0E0)
            }

            Icon(
                imageVector = weatherIcon,
                contentDescription = weather.condition,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Temperature and condition
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = weather.displayTemperature,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = weather.condition,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    if (weather.isLive) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                    }
                }

                // City badge with pin
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = "Location",
                        tint = FireOrange,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = weather.city,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = FireAmber
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = weather.displayHighLow,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
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
 * Focusable TV-Material button in the top bar to toggle/select library sort order.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SortTriggerButton(
    currentSort: AppSortOrder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)

    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.10f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = shape
            )
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevationColor = FireAmber.copy(alpha = 0.7f),
                elevation = 14.dp
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DarkSurface.copy(alpha = 0.85f),
            focusedContainerColor = DarkSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Sort,
                contentDescription = "Sort library",
                tint = FireAmber,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = when (currentSort) {
                    AppSortOrder.ALPHABETICAL -> "Sort: A–Z"
                    AppSortOrder.MOST_RECENT -> "Sort: Recent"
                    AppSortOrder.MOST_FREQUENT -> "Sort: Frequent"
                    AppSortOrder.CUSTOM -> "Sort: My Order"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
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

private fun getFormattedTime(context: Context): String {
    val is24Hour = DateFormat.is24HourFormat(context)
    val pattern = if (is24Hour) "HH:mm" else "h:mm a"
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(Date())
}

private fun getFormattedDate(): String {
    val formatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    return formatter.format(Date())
}
