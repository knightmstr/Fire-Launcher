package com.example.firelauncher.data

/**
 * User-adjustable launcher behaviour, persisted in DataStore.
 *
 * Option lists are fixed so every value can be cycled with a single D-pad OK press.
 */
data class LauncherSettings(
    /** Minutes between automatic wallpaper changes. 0 = never rotate. */
    val wallpaperIntervalMinutes: Int = DEFAULT_WALLPAPER_INTERVAL,
    /** Minutes of inactivity before the clock screensaver appears. 0 = disabled. */
    val screensaverTimeoutMinutes: Int = DEFAULT_SCREENSAVER_TIMEOUT,
    /** Show a "Hidden Apps" row at the bottom so hidden apps can be restored. */
    val showHiddenApps: Boolean = false
) {
    companion object {
        const val DEFAULT_WALLPAPER_INTERVAL = 30
        const val DEFAULT_SCREENSAVER_TIMEOUT = 10

        val WALLPAPER_INTERVAL_OPTIONS = listOf(15, 30, 60, 180, 0)
        val SCREENSAVER_TIMEOUT_OPTIONS = listOf(5, 10, 15, 30, 0)

        /** Returns the option after [current], wrapping around. Unknown values restart at the first option. */
        fun nextOption(options: List<Int>, current: Int): Int {
            val idx = options.indexOf(current)
            return options[(idx + 1) % options.size]
        }

        fun describeMinutes(minutes: Int, zeroLabel: String = "Off"): String = when {
            minutes <= 0 -> zeroLabel
            minutes % 60 == 0 -> "${minutes / 60} hr"
            else -> "$minutes min"
        }
    }
}
