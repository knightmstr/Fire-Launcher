package com.knightmstr.firelauncher.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.knightmstr.firelauncher.data.WeatherData
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SoftShadow = Shadow(color = Color(0x99000000), blurRadius = 18f)

/**
 * Centre of the home: the date and weather (press it to change the city), with a big light clock
 * underneath. Laid out like couch-tv's Fire TV home, so the wallpaper stays the main thing.
 */
@Composable
fun HomeClock(
    weather: WeatherData,
    onWeatherClick: () -> Unit,
    onToggleUnit: () -> Unit,
    modifier: Modifier = Modifier
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
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = date,
                style = TextStyle(shadow = SoftShadow),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            WeatherWidget(weather = weather, onClick = onWeatherClick, onToggleUnit = onToggleUnit)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = time,
                style = TextStyle(shadow = SoftShadow),
                fontSize = 112.sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.92f),
                letterSpacing = (-2).sp
            )
            if (amPm != null) {
                Text(
                    text = amPm,
                    style = TextStyle(shadow = SoftShadow),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(start = 8.dp, bottom = 26.dp)
                )
            }
        }
    }
}
