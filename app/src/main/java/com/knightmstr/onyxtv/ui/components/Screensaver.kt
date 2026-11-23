package com.knightmstr.onyxtv.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.knightmstr.onyxtv.data.LauncherSettings
import com.knightmstr.onyxtv.data.WallpaperModel
import com.knightmstr.onyxtv.data.WeatherData
import com.knightmstr.onyxtv.media.NowPlaying
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.random.Random

/** A new wallpaper every 5 minutes. */
private const val WALLPAPER_CHANGE_MS = 5 * 60 * 1000L

/** The clock block moves to the next spot every 30 minutes (burn-in protection). */
private const val SPOT_CHANGE_MS = 30 * 60 * 1000L

/** Spots across the lower part of the screen, as fractions of the free width. */
private val SPOTS = listOf(0.5f, 0.12f, 0.88f)

/**
 * The screensaver: the home screen with the apps faded away. The wallpaper shows properly (light
 * dim, slow zoom), with the home's clock, date and weather centred in one block and a soft shade
 * only behind it. The block sits low on screen, drifts a few pixels each minute and changes spot
 * every 30 minutes so nothing burns in. Darker at night if [nightDim] is on.
 *
 * Used on the home screen (any key dismisses it, see MainActivity) and as the Fire TV's
 * system screensaver (OnyxDream).
 */
@Composable
fun Screensaver(
    wallpaper: WallpaperModel?,
    weather: WeatherData,
    onNextWallpaper: () -> Unit,
    modifier: Modifier = Modifier,
    nowPlaying: NowPlaying? = null,
    nightDim: Boolean = true,
    contentScale: ContentScale = ContentScale.Crop
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
            now = System.currentTimeMillis()
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(WALLPAPER_CHANGE_MS)
            onNextWallpaper()
        }
    }

    // Night: darker picture and a dimmer clock
    val hour = remember(now) { Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY) }
    val night = nightDim && LauncherSettings.isNight(hour)
    val nightShade by animateFloatAsState(if (night) 0.55f else 0f, tween(4000), label = "nightShade")
    val textAlpha by animateFloatAsState(if (night) 0.6f else 1f, tween(4000), label = "nightText")

    // Spot (every 30 min) and a small drift (every minute)
    val spotIndex = ((now / SPOT_CHANGE_MS) % SPOTS.size).toInt()
    val spot by animateFloatAsState(SPOTS[spotIndex], tween(6000), label = "clockSpot")
    val minute = now / 60_000L
    val driftX by animateFloatAsState(Random(minute).nextInt(-12, 13).toFloat(), tween(3000), label = "driftX")
    val driftY by animateFloatAsState(Random(minute + 7).nextInt(-8, 9).toFloat(), tween(3000), label = "driftY")

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(targetState = wallpaper?.url, animationSpec = tween(2500), label = "ScreensaverFade") { url ->
            if (url != null) KenBurnsImage(url, contentScale)
        }

        // Light overall dim, and a soft shade rising from the bottom behind the clock
        Box(modifier = Modifier.fillMaxSize().background(Color(0x1F000000)))
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.45f to Color.Transparent,
                    1f to Color(0x8C000000)
                )
            )
        )
        if (nightShade > 0f) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = nightShade)))

        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 56.dp)) {
            var blockWidth by remember { mutableIntStateOf(0) }
            val freeX = (constraints.maxWidth - blockWidth).coerceAtLeast(0)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .onSizeChanged { blockWidth = it.width }
                    .graphicsLayer {
                        translationX = freeX * spot + driftX.dp.toPx()
                        translationY = driftY.dp.toPx()
                        alpha = textAlpha
                    }
            ) {
                HomeClock(weather = weather, onWeatherClick = {}, interactive = false)
                nowPlaying?.let { np ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${if (np.isPlaying) "▶" else "❚❚"}  ${np.title} · ${np.appLabel}",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 520.dp)
                    )
                }
            }
        }

        // Photo credit, small and faint
        wallpaper?.let { wp ->
            Text(
                text = if (wp.isLocal) "Your photo · ${wp.title}" else "${wp.title} · r/TVWallpapersSFW",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f * textAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 14.dp)
                    .widthIn(max = 320.dp)
            )
        }
    }
}

/** Wallpaper with a very slow zoom and pan, restarting with each new picture. */
@Composable
private fun KenBurnsImage(url: String, contentScale: ContentScale = ContentScale.Crop) {
    val progress = remember(url) { Animatable(0f) }
    val direction = remember(url) { if (Random.nextBoolean()) 1f else -1f }
    LaunchedEffect(url) {
        progress.animateTo(1f, tween(WALLPAPER_CHANGE_MS.toInt(), easing = LinearEasing))
    }
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = contentScale,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progress.value
                scaleX = 1.04f + 0.06f * p
                scaleY = scaleX
                translationX = direction * size.width * 0.02f * (p - 0.5f)
            }
    )
}
