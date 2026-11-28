package com.knightmstr.onyxtv.data

/** Everything a backup holds. Kids mode, its PIN and usage history are never part of it. */
data class BackupData(
    val settings: LauncherSettings,
    val favorites: List<String>,
    val hidden: Set<String>,
    val labels: Map<String, String>,
    val appOrder: List<String>,
    val sortOrder: AppSortOrder,
    val weather: BackupWeather?,
    val celsius: Boolean,
    val wallpaperUrls: List<String>,
    /** Dock folders; older backups have none. */
    val folders: List<AppFolder> = emptyList()
)

data class BackupWeather(val city: String, val country: String, val latitude: Double, val longitude: Double)
