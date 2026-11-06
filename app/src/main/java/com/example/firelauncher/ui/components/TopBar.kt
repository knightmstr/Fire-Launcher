package com.example.firelauncher.ui.components

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Thunderstorm
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
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.firelauncher.data.WeatherData
import com.example.firelauncher.ui.theme.DarkSurface
import com.example.firelauncher.ui.theme.DarkSurfaceVariant
import com.example.firelauncher.ui.theme.FireAmber
import com.example.firelauncher.ui.theme.FireOrange
import com.example.firelauncher.ui.theme.TextPrimary
import com.example.firelauncher.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Top bar displaying:
 * - Launcher brand on the left
 * - Focusable Weather widget in the center
 * - Global Search icon button / expandable search input
 * - Dynamic clock (12h/24h) and formatted calendar date on the right.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopBar(
    weather: WeatherData,
    availableCities: List<String> = WeatherData.CITY_PRESETS.map { it.city },
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    onSearchQueryChange: (String) -> Unit = {},
    onToggleSearch: (Boolean) -> Unit = {},
    onSelectCity: (String) -> Unit = {},
    onToggleUnit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTimeString by remember { mutableStateOf(getFormattedTime(context)) }
    var currentDateString by remember { mutableStateOf(getFormattedDate()) }
    var showCityDialog by remember { mutableStateOf(false) }

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
                Text(
                    text = "FireLauncher",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )
            }

            // Center: Weather Widget or Search Input
            if (isSearchActive) {
                // Expanded TV Search Bar
                GlobalSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onClose = { onToggleSearch(false) },
                    modifier = Modifier.weight(1f, fill = false).padding(horizontal = 24.dp)
                )
            } else {
                // Top Weather Widget (Current weather for selected city)
                WeatherWidget(
                    weather = weather,
                    onClick = { showCityDialog = true },
                    onToggleUnit = onToggleUnit
                )
            }

            // Right: Search Trigger Button + Live Clock & Date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = currentDateString,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // City Selector Overlay Dialog
        if (showCityDialog) {
            CitySelectionDialog(
                currentCity = weather.city,
                cities = availableCities,
                onCitySelected = { city ->
                    onSelectCity(city)
                    showCityDialog = false
                },
                onDismiss = { showCityDialog = false }
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
        modifier = modifier.size(42.dp),
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
            modifier = Modifier.size(42.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = if (isActive) Icons.Rounded.Close else Icons.Rounded.Search,
                contentDescription = if (isActive) "Close Search" else "Search Apps",
                tint = if (isActive) FireOrange else TextPrimary,
                modifier = Modifier.size(20.dp)
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
        modifier = modifier.width(380.dp).height(42.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            androidx.compose.material3.Icon(
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(FireOrange),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                androidx.compose.material3.Icon(
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
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

            androidx.compose.material3.Icon(
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
                }

                // City badge with pin
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    androidx.compose.material3.Icon(
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
 * TV dialog modal allowing user to change the selected city with D-pad navigation.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CitySelectionDialog(
    currentCity: String,
    cities: List<String>,
    onCitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            onClick = {},
            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(20.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color(0xFF141824)
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(
                    border = BorderStroke(1.5.dp, FireOrange.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp)
                )
            ),
            modifier = Modifier.width(360.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Weather Location",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Close",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TvLazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(cities) { city ->
                        val isSelected = city.equals(currentCity, ignoreCase = true)
                        Surface(
                            onClick = { onCitySelected(city) },
                            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (isSelected) FireOrange.copy(alpha = 0.2f) else Color.Transparent,
                                focusedContainerColor = DarkSurfaceVariant
                            ),
                            border = ClickableSurfaceDefaults.border(
                                focusedBorder = Border(
                                    border = BorderStroke(1.5.dp, Color.White),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = city,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) FireAmber else TextPrimary
                                )
                                if (isSelected) {
                                    Text(
                                        text = "SELECTED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FireAmber
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
