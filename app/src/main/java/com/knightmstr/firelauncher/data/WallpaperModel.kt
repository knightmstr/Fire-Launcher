package com.knightmstr.firelauncher.data

/**
 * Data model representing a 16:9 TV wallpaper from the user's own photos, r/TVWallpapersSFW, or the curated fallback.
 */
data class WallpaperModel(
    val id: String,
    val title: String,
    val url: String,
    val author: String = "r/TVWallpapersSFW",
    val permalink: String = "https://reddit.com/r/TVWallpapersSFW",
    val source: String = SOURCE_REDDIT
) {
    val isLocal: Boolean get() = source == SOURCE_LOCAL

    companion object {
        const val SOURCE_REDDIT = "reddit"
        const val SOURCE_LOCAL = "local"
    }
}
