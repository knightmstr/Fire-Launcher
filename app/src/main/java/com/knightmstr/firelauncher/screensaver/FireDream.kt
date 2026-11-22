package com.knightmstr.firelauncher.screensaver

import android.service.dreams.DreamService
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.knightmstr.firelauncher.FireLauncherApp
import com.knightmstr.firelauncher.data.LauncherPrefsStore
import com.knightmstr.firelauncher.data.LauncherSettings
import com.knightmstr.firelauncher.data.WeatherData
import com.knightmstr.firelauncher.data.WeatherRepository
import com.knightmstr.firelauncher.media.NowPlayingRepository
import com.knightmstr.firelauncher.ui.components.Screensaver
import com.knightmstr.firelauncher.ui.theme.FireLauncherTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * FireLauncher's screensaver as a system screensaver (Android "dream"), so it can replace Amazon's
 * everywhere, not just on the home screen. Same wallpaper, clock and temperature as the home.
 * Made the Fire TV's screensaver by [ScreensaverTakeover]. Any remote button ends it.
 */
class FireDream : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private var nowPlaying: NowPlayingRepository? = null
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false   // any key wakes the TV
        isFullscreen = true
        isScreenBright = true
        // "Keep Fire TV awake": hold the screen on while the screensaver shows
        if (ScreensaverTakeover.keepAwake(this)) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val app = application as FireLauncherApp
        // Fetch weather once if the home screen hasn't yet (e.g. right after a restart)
        if (app.latestWeather.value == null) lifecycleScope.launch {
            runCatching {
                val loc = app.prefs.selectedLocationFlow.first()
                val celsius = app.prefs.isCelsiusFlow.first()
                app.latestWeather.value = WeatherRepository()
                    .fetchLiveWeather(loc.city, loc.country, loc.latitude, loc.longitude, celsius)
            }
        }

        // What's playing, if Now playing has been allowed (only while the screensaver shows)
        nowPlaying = NowPlayingRepository(this)

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FireDream)
            setViewTreeSavedStateRegistryOwner(this@FireDream)
            setContent {
                FireLauncherTheme {
                    val wallpaper by app.wallpapers.currentWallpaper.collectAsState()
                    val weather by app.latestWeather.collectAsState()
                    val settings by app.prefs.settingsFlow.collectAsState(initial = LauncherSettings())
                    val playing = nowPlaying?.nowPlaying?.collectAsState()
                    Screensaver(
                        wallpaper = wallpaper,
                        weather = weather ?: WeatherData.defaultSeattle(LauncherPrefsStore.defaultIsCelsius()),
                        onNextWallpaper = app.wallpapers::nextWallpaper,
                        nowPlaying = playing?.value,
                        nightDim = settings.nightDim
                    )
                }
            }
        }
        setContentView(view)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        nowPlaying?.start()
    }

    override fun onDreamingStopped() {
        nowPlaying?.stop()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDetachedFromWindow()
    }
}
