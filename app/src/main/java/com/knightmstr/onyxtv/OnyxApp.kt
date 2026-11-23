package com.knightmstr.onyxtv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.knightmstr.onyxtv.data.LauncherPrefsStore
import com.knightmstr.onyxtv.data.LocalPhotoSource
import com.knightmstr.onyxtv.data.WallpaperRepository
import com.knightmstr.onyxtv.data.WeatherData
import com.knightmstr.onyxtv.update.UpdateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Main Application class for Onyx TV.
 * Configures strict memory and disk cache limits for low-end Fire TV hardware
 * to prevent r/TVWallpapersSFW automatic downloads from bloating limited flash storage.
 */
class OnyxApp : Application(), ImageLoaderFactory {

    /** App-wide so [com.knightmstr.onyxtv.update.InstallResultReceiver] can report results. */
    val updateRepository: UpdateRepository by lazy { UpdateRepository(this) }

    /** Lives as long as the app: shared by the home screen and the screensaver. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val prefs: LauncherPrefsStore by lazy { LauncherPrefsStore(this) }
    val localPhotos: LocalPhotoSource by lazy { LocalPhotoSource(this) }

    /** One wallpaper rotation for the whole app, so the screensaver shows the same picture as home. */
    val wallpapers: WallpaperRepository by lazy {
        WallpaperRepository(
            scope = appScope,
            rotationIntervalMinutes = prefs.settingsFlow.map { it.wallpaperIntervalMinutes }.distinctUntilChanged(),
            source = prefs.settingsFlow.map { it.wallpaperSource }.distinctUntilChanged(),
            localPhotos = localPhotos,
            customUrls = prefs.customWallpaperUrlsFlow
        )
    }

    /** Latest weather the home screen fetched; the screensaver shows the same. */
    val latestWeather = MutableStateFlow<WeatherData?>(null)

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Strict memory cache ceiling: exactly 10% of available application RAM
                    .maxSizePercent(0.10)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    // Hard disk cache ceiling: capped at exactly 15MB
                    .maxSizeBytes(15L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false) // Optimize for TV wallpaper rotation
            .crossfade(true)
            .build()
    }
}
