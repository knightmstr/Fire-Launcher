package com.knightmstr.onyxtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.knightmstr.onyxtv.data.WeatherData
import com.knightmstr.onyxtv.data.WeatherForecast
import com.knightmstr.onyxtv.ui.theme.DarkSurface
import com.knightmstr.onyxtv.ui.theme.TextPrimary
import com.knightmstr.onyxtv.ui.theme.TextSecondary

/** Five-day forecast for the chosen city, any weather heads-ups, and a way to change city. */
@Composable
fun ForecastDialog(
    weather: WeatherData,
    onChangeCity: () -> Unit,
    onDismiss: () -> Unit
) {
    val first = rememberInitialFocus()
    TvDialogFrame(
        title = "Weather · ${weather.city}",
        subtitle = "${weather.displayTemperature} ${weather.condition} now",
        accentColor = Color(0xFF60A5FA),
        onDismiss = onDismiss,
        width = 640.dp
    ) {
        if (weather.forecast.isEmpty()) {
            Text("The forecast isn't available right now.", fontSize = 13.sp, color = TextSecondary)
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weather.forecast.forEachIndexed { i, day ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(DarkSurface, RoundedCornerShape(12.dp))
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(WeatherForecast.shortDay(day.date, i), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Icon(
                            imageVector = weatherIcon(WeatherForecast.iconType(day.weatherCode)),
                            contentDescription = null,
                            tint = Color(0xFFE5E7EB),
                            modifier = Modifier.size(26.dp)
                        )
                        val high = if (weather.isCelsius) day.highC else day.highF
                        val low = if (weather.isCelsius) day.lowC else day.lowF
                        Text("$high° / $low°", fontSize = 14.sp, color = TextPrimary)
                        day.rainChance?.let { Text("Rain $it%", fontSize = 11.sp, color = Color(0xFF93C5FD)) }
                    }
                }
            }
        }

        weather.headsUps.forEach { h ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                Text(h.message, fontSize = 14.sp, color = Color(0xFFFBBF24))
            }
        }
        if (weather.headsUps.isNotEmpty()) {
            Text(
                "Worked out from the forecast, not official warnings. Check your weather service for those.",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        TvMenuItem(
            title = "Change city",
            subtitle = "${weather.city}, ${weather.country}",
            icon = Icons.Rounded.LocationCity,
            iconTint = Color(0xFF60A5FA),
            modifier = Modifier.focusRequester(first).padding(top = 4.dp),
            onClick = onChangeCity
        )
    }
}
