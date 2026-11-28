package com.knightmstr.onyxtv.upload

import com.knightmstr.onyxtv.data.CardShape
import com.knightmstr.onyxtv.data.HomeLayout
import com.knightmstr.onyxtv.data.LauncherSettings
import com.knightmstr.onyxtv.data.WallpaperFit
import com.knightmstr.onyxtv.data.WallpaperSource
import org.json.JSONArray
import org.json.JSONObject

/**
 * Launcher settings as the phone page sees them, and strict parsing of changes coming back.
 *
 * Only settings that are safe to change remotely are exposed. The update channel stays TV-only,
 * and so does everything Kids mode or the Fire TV system features control.
 *
 * A change is all-or-nothing: an unknown key, a wrong type, or a value outside the TV's own
 * options rejects the whole request, so a bad request can't half-apply.
 */
object SettingsJson {

    const val HOME_LAYOUT = "homeLayout"
    const val CARD_SHAPE = "cardShape"
    const val TILE_WIDTH = "tileWidthDp"
    const val SHOW_ALL_APPS_ROW = "showAllAppsRow"
    const val SHOW_HIDDEN_APPS = "showHiddenApps"
    const val WALLPAPER_SOURCE = "wallpaperSource"
    const val WALLPAPER_FIT = "wallpaperFit"
    const val WALLPAPER_INTERVAL = "wallpaperIntervalMinutes"
    const val SCREENSAVER_TIMEOUT = "screensaverTimeoutMinutes"
    const val NIGHT_DIM = "nightDim"
    const val NIGHT_START = "nightStartHour"
    const val NIGHT_END = "nightEndHour"

    val KEYS = setOf(
        HOME_LAYOUT, CARD_SHAPE, TILE_WIDTH, SHOW_ALL_APPS_ROW, SHOW_HIDDEN_APPS,
        WALLPAPER_SOURCE, WALLPAPER_FIT, WALLPAPER_INTERVAL, SCREENSAVER_TIMEOUT, NIGHT_DIM,
        NIGHT_START, NIGHT_END
    )

    sealed class Result {
        data class Ok(val settings: LauncherSettings) : Result()
        data class Invalid(val key: String) : Result()
    }

    fun toJson(s: LauncherSettings): JSONObject = JSONObject()
        .put(HOME_LAYOUT, s.homeLayout.name)
        .put(CARD_SHAPE, s.cardShape.name)
        .put(TILE_WIDTH, s.tileWidthDp)
        .put(SHOW_ALL_APPS_ROW, s.showAllAppsRow)
        .put(SHOW_HIDDEN_APPS, s.showHiddenApps)
        .put(WALLPAPER_SOURCE, s.wallpaperSource.name)
        .put(WALLPAPER_FIT, s.wallpaperFit.name)
        .put(WALLPAPER_INTERVAL, s.wallpaperIntervalMinutes)
        .put(SCREENSAVER_TIMEOUT, s.screensaverTimeoutMinutes)
        .put(NIGHT_DIM, s.nightDim)
        .put(NIGHT_START, s.nightStartHour)
        .put(NIGHT_END, s.nightEndHour)

    /** The choices the TV offers, so the page draws the same options with the same names. */
    fun options(): JSONObject = JSONObject()
        .put(HOME_LAYOUT, enumOptions(HomeLayout.entries.map { Triple(it.name, it.title, it.description) }))
        .put(CARD_SHAPE, enumOptions(CardShape.entries.map { Triple(it.name, it.title, null) }))
        .put(WALLPAPER_SOURCE, enumOptions(WallpaperSource.entries.map { Triple(it.name, it.title, null) }))
        .put(WALLPAPER_FIT, enumOptions(WallpaperFit.entries.map { Triple(it.name, it.title, it.description) }))
        .put(WALLPAPER_INTERVAL, minuteOptions(LauncherSettings.WALLPAPER_INTERVAL_OPTIONS, "Never"))
        .put(SCREENSAVER_TIMEOUT, minuteOptions(LauncherSettings.SCREENSAVER_TIMEOUT_OPTIONS, "Off"))
        .put(NIGHT_START, hourOptions())
        .put(NIGHT_END, hourOptions())
        .put(
            TILE_WIDTH,
            JSONObject()
                .put("min", LauncherSettings.TILE_WIDTH_RANGE.first)
                .put("max", LauncherSettings.TILE_WIDTH_RANGE.last)
                .put("step", LauncherSettings.TILE_WIDTH_STEP)
        )

    /** Applies the keys present in [changes] to [current]. */
    fun apply(current: LauncherSettings, changes: JSONObject): Result {
        var s = current
        val keys = changes.keys().asSequence().toList()
        if (keys.isEmpty()) return Result.Invalid("")
        for (key in keys) {
            val v = changes.opt(key)
            s = when (key) {
                HOME_LAYOUT -> s.copy(homeLayout = enumValue<HomeLayout>(v) ?: return Result.Invalid(key))
                CARD_SHAPE -> s.copy(cardShape = enumValue<CardShape>(v) ?: return Result.Invalid(key))
                WALLPAPER_SOURCE -> s.copy(wallpaperSource = enumValue<WallpaperSource>(v) ?: return Result.Invalid(key))
                WALLPAPER_FIT -> s.copy(wallpaperFit = enumValue<WallpaperFit>(v) ?: return Result.Invalid(key))
                TILE_WIDTH -> {
                    val w = intValue(v) ?: return Result.Invalid(key)
                    val r = LauncherSettings.TILE_WIDTH_RANGE
                    if (w !in r || (w - r.first) % LauncherSettings.TILE_WIDTH_STEP != 0) return Result.Invalid(key)
                    s.copy(tileWidthDp = w)
                }
                WALLPAPER_INTERVAL -> {
                    val m = intValue(v)?.takeIf { it in LauncherSettings.WALLPAPER_INTERVAL_OPTIONS }
                        ?: return Result.Invalid(key)
                    s.copy(wallpaperIntervalMinutes = m)
                }
                SCREENSAVER_TIMEOUT -> {
                    val m = intValue(v)?.takeIf { it in LauncherSettings.SCREENSAVER_TIMEOUT_OPTIONS }
                        ?: return Result.Invalid(key)
                    s.copy(screensaverTimeoutMinutes = m)
                }
                SHOW_ALL_APPS_ROW -> s.copy(showAllAppsRow = v as? Boolean ?: return Result.Invalid(key))
                SHOW_HIDDEN_APPS -> s.copy(showHiddenApps = v as? Boolean ?: return Result.Invalid(key))
                NIGHT_DIM -> s.copy(nightDim = v as? Boolean ?: return Result.Invalid(key))
                NIGHT_START -> s.copy(nightStartHour = hourValue(v) ?: return Result.Invalid(key))
                NIGHT_END -> s.copy(nightEndHour = hourValue(v) ?: return Result.Invalid(key))
                else -> return Result.Invalid(key)
            }
        }
        return Result.Ok(s)
    }

    /** Exact enum name only (no case folding, no fallback to a default). */
    private inline fun <reified E : Enum<E>> enumValue(v: Any?): E? =
        (v as? String)?.let { name -> enumValues<E>().firstOrNull { it.name == name } }

    /** Whole numbers only: 30 and 30.0 are fine, 30.5, "30" and true are not. */
    private fun intValue(v: Any?): Int? = when (v) {
        is Int -> v
        is Long -> v.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
        is Double -> v.takeIf { it % 1.0 == 0.0 && it in -1e9..1e9 }?.toInt()
        is Number -> v.toDouble().takeIf { it % 1.0 == 0.0 && it in -1e9..1e9 }?.toInt()
        else -> null
    }

    private fun hourValue(v: Any?): Int? = intValue(v)?.takeIf { it in LauncherSettings.HOUR_RANGE }

    private fun hourOptions(): JSONArray {
        val array = JSONArray()
        LauncherSettings.HOUR_RANGE.forEach {
            array.put(JSONObject().put("value", it).put("title", LauncherSettings.describeHour(it)))
        }
        return array
    }

    private fun enumOptions(items: List<Triple<String, String, String?>>): JSONArray {
        val array = JSONArray()
        items.forEach { (value, title, description) ->
            val o = JSONObject().put("value", value).put("title", title)
            if (description != null) o.put("description", description)
            array.put(o)
        }
        return array
    }

    private fun minuteOptions(values: List<Int>, zeroLabel: String): JSONArray {
        val array = JSONArray()
        values.forEach {
            array.put(JSONObject().put("value", it).put("title", LauncherSettings.describeMinutes(it, zeroLabel)))
        }
        return array
    }
}
