package com.knightmstr.onyxtv.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.knightmstr.onyxtv.data.WeatherData
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A faint, wide shadow: just enough to read on bright skies, without lifting the text off the photo. */
private val Soft = TextStyle(shadow = Shadow(color = Color(0x40000000), blurRadius = 16f))
/** Translucent white so the wallpaper shows through the text. */
private val Mist = Color.White.copy(alpha = 0.72f)

/**
 * Centre of the home: one quiet line with the date and weather (select the weather to change the
 * city), and a big light clock underneath. No boxes, just text over the wallpaper, like couch-tv.
 */
@Composable
fun HomeClock(
    weather: WeatherData,
    onWeatherClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** False in the screensaver: the weather is plain text, not a button. */
    interactive: Boolean = true
) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L - (System.currentTimeMillis() % 1000L))
            now = System.currentTimeMillis()
        }
    }
    val is24h = DateFormat.is24HourFormat(context)
    val locale = Locale.getDefault()
    val time = SimpleDateFormat(if (is24h) "HH:mm" else "h:mm", locale).format(Date(now))
    val amPm = if (is24h) null else SimpleDateFormat("a", locale).format(Date(now)).lowercase(locale)
    val date = SimpleDateFormat("EEEE d MMMM", locale).format(Date(now))

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = date, style = Soft, fontSize = 19.sp, fontWeight = FontWeight.Normal, color = Mist)
            Text(text = "·", style = Soft, fontSize = 19.sp, color = Mist.copy(alpha = 0.6f))
            if (interactive) WeatherText(weather = weather, onClick = onWeatherClick)
            else WeatherLine(weather = weather, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }
        // The soonest weather heads-up, e.g. "Thunderstorms tomorrow" (worked out from the forecast)
        weather.headsUps.firstOrNull()?.let { headsUp ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = HeadsUpAmber,
                    modifier = Modifier.size(18.dp)
                )
                Text(text = headsUp.message, style = Soft, fontSize = 16.sp, color = HeadsUpAmber)
            }
        }
        Row {
            Text(
                text = time,
                style = Soft,
                fontSize = 108.sp,
                fontWeight = FontWeight.Thin,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = (-2).sp,
                modifier = Modifier.alignByBaseline()
            )
            if (amPm != null) {
                Text(
                    text = amPm,
                    style = Soft,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Thin,
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.alignByBaseline().padding(start = 6.dp)
                )
            }
        }
    }
}

/** "☀ 31° Mainly sunny · Darwin": plain text, with a faint pill only while it has focus. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WeatherText(weather: WeatherData, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f)), shape = shape)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = 0.14f)
        )
    ) {
        WeatherLine(weather = weather, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Composable
private fun WeatherLine(weather: WeatherData, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            imageVector = weatherIcon(weather.iconType),
            contentDescription = weather.condition,
            tint = Mist,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${weather.displayTemperature} ${weather.condition} · ${weather.city}",
            style = Soft,
            fontSize = 19.sp,
            fontWeight = FontWeight.Normal,
            color = Mist
        )
    }
}

internal fun weatherIcon(type: String): ImageVector = when (type) {
    "partly_cloudy" -> Icons.Rounded.WbCloudy
    "cloudy" -> Icons.Rounded.Cloud
    "rain" -> Icons.Rounded.WaterDrop
    "thunder" -> Icons.Rounded.Thunderstorm
    else -> Icons.Rounded.WbSunny
}

private val HeadsUpAmber = Color(0xFFFBBF24)
