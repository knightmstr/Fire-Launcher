package com.knightmstr.firelauncher.ui.components

import android.text.format.DateFormat
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.knightmstr.firelauncher.data.WallpaperModel
import com.knightmstr.firelauncher.data.WeatherData
import com.knightmstr.firelauncher.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

private const val WALLPAPER_CHANGE_MS = 2 * 60 * 1000L

/**
 * Idle screensaver: dimmed wallpaper slideshow with a large clock, date and temperature.
 * The clock drifts to a new spot every minute to avoid burn-in on OLED/plasma TVs.
 * Any remote key dismisses it (handled in MainActivity).
 */
@Composable
fun Screensaver(
    wallpaper: WallpaperModel?,
    weather: WeatherData,
    onNextWallpaper: () -> Unit,
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
    LaunchedEffect(Unit) {
        while (true) {
            delay(WALLPAPER_CHANGE_MS)
            onNextWallpaper()
        }
    }

    // New random position once per minute (fractions of the free space)
    val minute = now / 60_000L
    val position = remember(minute) { Random(minute).nextFloat() to Random(minute + 7).nextFloat() }
    val fx by animateFloatAsState(position.first, tween(2500), label = "clockX")
    val fy by animateFloatAsState(position.second, tween(2500), label = "clockY")

    val timePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    val time = SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(now))
    val date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now))

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(targetState = wallpaper?.url, animationSpec = tween(1500), label = "ScreensaverFade") { url ->
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize().background(Color(0x99000000)))

        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(64.dp)) {
            val clockWidth = 360.dp
            val clockHeight = 170.dp
            val freeX = (maxWidth - clockWidth).coerceAtLeast(0.dp)
            val freeY = (maxHeight - clockHeight).coerceAtLeast(0.dp)

            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.offset(x = freeX * fx, y = freeY * fy)
            ) {
                Text(
                    text = time,
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )
                Text(
                    text = "$date  •  ${weather.displayTemperature} ${weather.city}",
                    fontSize = 18.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
