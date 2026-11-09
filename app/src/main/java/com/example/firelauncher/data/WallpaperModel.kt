package com.example.firelauncher.data

/**
 * Data model representing a 16:9 TV wallpaper fetched from r/TVWallpapersSFW or curated fallback.
 */
data class WallpaperModel(
    val id: String,
    val title: String,
    val url: String,
    val author: String = "r/TVWallpapersSFW",
    val permalink: String = "https://reddit.com/r/TVWallpapersSFW",
    val source: String = "reddit"
)
