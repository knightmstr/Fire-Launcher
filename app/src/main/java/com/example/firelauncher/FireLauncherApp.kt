package com.example.firelauncher

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Main Application class for FireLauncher.
 * Configures strict memory and disk cache limits for low-end Fire TV hardware
 * to prevent r/TVWallpapersSFW automatic downloads from bloating limited flash storage.
 */
class FireLauncherApp : Application(), ImageLoaderFactory {

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
