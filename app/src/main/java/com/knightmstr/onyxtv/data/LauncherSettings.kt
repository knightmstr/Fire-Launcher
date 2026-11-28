package com.knightmstr.onyxtv.data

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
    val showHiddenApps: Boolean = false,
    /** Where wallpapers come from. Local photos are the default (no network, no rotation fetches). */
    val wallpaperSource: WallpaperSource = WallpaperSource.MY_PHOTOS,
    /** App tile width in dp, set with the size slider ([TILE_WIDTH_RANGE]). */
    val tileWidthDp: Int = DEFAULT_TILE_WIDTH,
    /** Corner style of app tiles. */
    val cardShape: CardShape = CardShape.ROUNDED,
    /** Show every app as a row on the home screen (off: use the All Apps tile instead). */
    val showAllAppsRow: Boolean = false,
    /** Darken the screensaver between [nightStartHour] and [nightEndHour]. */
    val nightDim: Boolean = true,
    /** Hour (0–23) night dimming starts, e.g. 22 = 10pm. */
    val nightStartHour: Int = NIGHT_START_HOUR,
    /** Hour (0–23) night dimming ends, e.g. 6 = 6am. */
    val nightEndHour: Int = NIGHT_END_HOUR,
    /** Dock: apps always at the bottom. Clean: only the clock until you press a button. */
    val homeLayout: HomeLayout = HomeLayout.DOCK,
    /** Which release channel app updates come from. Stable by default. */
    val updateChannel: UpdateChannel = UpdateChannel.STABLE,
    /** How a wallpaper photo is scaled to the screen. */
    val wallpaperFit: WallpaperFit = WallpaperFit.FILL
) {
    companion object {
        const val DEFAULT_WALLPAPER_INTERVAL = 30

        /** Default night dimming hours for the screensaver: from 22:00 until 06:00. */
        const val NIGHT_START_HOUR = 22
        const val NIGHT_END_HOUR = 6
        val HOUR_RANGE = 0..23

        fun clampHour(hour: Int): Int = hour.coerceIn(HOUR_RANGE.first, HOUR_RANGE.last)

        /**
         * True if [hourOfDay] falls in the window from [start] (inclusive) to [end] (exclusive).
         * The window may cross midnight (22 → 6). Equal start and end means no dimming.
         */
        fun isNight(hourOfDay: Int, start: Int = NIGHT_START_HOUR, end: Int = NIGHT_END_HOUR): Boolean = when {
            start == end -> false
            start < end -> hourOfDay in start until end
            else -> hourOfDay >= start || hourOfDay < end
        }

        /** "10 pm", "12 am", "12 pm", "6 am". */
        fun describeHour(hour: Int): String {
            val h = clampHour(hour)
            val twelve = if (h % 12 == 0) 12 else h % 12
            return "$twelve ${if (h < 12) "am" else "pm"}"
        }
        const val DEFAULT_SCREENSAVER_TIMEOUT = 10

        /** Tile size slider: 90dp (tiny) to 170dp, in 10dp steps; 140dp is the old "Small". */
        val TILE_WIDTH_RANGE = 90..170
        const val TILE_WIDTH_STEP = 10
        const val DEFAULT_TILE_WIDTH = 140

        fun clampTileWidth(dp: Int): Int = dp.coerceIn(TILE_WIDTH_RANGE.first, TILE_WIDTH_RANGE.last)

        /** Width saved by older versions as a size name (Small, Medium…), capped to the slider's range. */
        fun tileWidthFromLegacySize(name: String?): Int = when (name?.uppercase()) {
            null, "SMALL" -> DEFAULT_TILE_WIDTH
            "EXTRA_SMALL" -> 115
            else -> TILE_WIDTH_RANGE.last // Medium / Large / Extra large: the biggest the slider allows
        }

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

enum class CardShape(val title: String) {
    ROUNDED("Rounded"),
    SQUARE("Square"),
    SOFT("Extra rounded"),
    PILL("Pill");

    fun next(): CardShape = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(name: String?): CardShape =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ROUNDED
    }
}

enum class HomeLayout(val title: String, val description: String) {
    DOCK("Dock", "Your favourite apps always along the bottom"),
    CLEAN("Clean", "Just the clock and wallpaper; any button brings the apps back");

    fun next(): HomeLayout = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(name: String?): HomeLayout =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DOCK
    }
}

enum class WallpaperFit(val title: String, val description: String) {
    /** Cover the whole screen, cropping edges of off-ratio photos. */
    FILL("Fill screen", "Covers the screen, trims the edges"),
    /** Show the whole photo with bars, nothing cropped. */
    FIT("Fit whole photo", "Shows all of the photo, may leave bars"),
    /** Stretch to fill exactly, distorting off-ratio photos. */
    STRETCH("Stretch", "Fills exactly, may distort the photo");

    fun next(): WallpaperFit = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(name: String?): WallpaperFit =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: FILL
    }
}

enum class UpdateChannel(val title: String, val description: String) {
    /** Only full releases — the tested builds everyone gets. */
    STABLE("Stable", "Only tested releases"),
    /** The newest release including pre-releases — early test builds, may be rough. */
    BETA("Beta", "Early test builds, may be rough");

    fun next(): UpdateChannel = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(name: String?): UpdateChannel =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: STABLE
    }
}

enum class WallpaperSource(val title: String) {
    /** Photos in Pictures/Onyx TV (or the app's own folder); falls back to Reddit if none. */
    MY_PHOTOS("My photos"),
    REDDIT("r/TVWallpapersSFW"),
    /** Image URLs the user added; falls back to Reddit if none. */
    CUSTOM_URL("My links");

    fun next(): WallpaperSource = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(name: String?): WallpaperSource =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MY_PHOTOS
    }
}
