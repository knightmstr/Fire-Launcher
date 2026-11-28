package com.knightmstr.onyxtv.upload

import com.knightmstr.onyxtv.data.AppSortOrder
import com.knightmstr.onyxtv.data.BackupData
import com.knightmstr.onyxtv.data.BackupWeather
import com.knightmstr.onyxtv.data.LauncherSettings
import com.knightmstr.onyxtv.data.PrefsCodec
import org.json.JSONArray
import org.json.JSONObject

/**
 * Backup file format and strict parsing of a file coming back.
 *
 * Restoring is all-or-nothing: the file is fully checked first, and any bad part rejects the
 * whole file, so a damaged or tampered file can't leave the TV half-restored. Package names
 * only need to be well-formed (not installed), so a backup can be restored onto a fresh TV
 * before its apps are reinstalled.
 */
object BackupJson {

    const val FORMAT = "onyx-tv-backup"
    const val VERSION = 1

    /** Largest backup file accepted. */
    const val MAX_BYTES = 256 * 1024

    const val MAX_PACKAGES = 500
    const val MAX_WALLPAPER_URLS = 50
    private const val MAX_PLACE_LENGTH = 80

    sealed class Result {
        data class Ok(val data: BackupData) : Result()
        data class Invalid(val reason: String) : Result()
    }

    fun toJson(b: BackupData): JSONObject {
        val labels = JSONObject()
        b.labels.toSortedMap().forEach { (pkg, label) -> labels.put(pkg, label) }
        val json = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("settings", SettingsJson.toJson(b.settings))
            .put("favorites", JSONArray(b.favorites))
            .put("hidden", JSONArray(b.hidden.sorted()))
            .put("labels", labels)
            .put("appOrder", JSONArray(b.appOrder))
            .put("sortOrder", b.sortOrder.name)
            .put("celsius", b.celsius)
            .put("wallpaperUrls", JSONArray(b.wallpaperUrls))
        b.weather?.let {
            json.put(
                "weather",
                JSONObject().put("city", it.city).put("country", it.country)
                    .put("latitude", it.latitude).put("longitude", it.longitude)
            )
        }
        return json
    }

    /** Checks a whole backup against [current] settings (for keys the file leaves out). */
    fun parse(json: JSONObject, current: LauncherSettings): Result {
        if (json.opt("format") != FORMAT) return Result.Invalid("not_a_backup")
        val version = json.opt("version")
        if (version !is Int || version < 1) return Result.Invalid("bad_version")
        if (version > VERSION) return Result.Invalid("newer_version")

        val settingsJson = json.opt("settings") as? JSONObject ?: return Result.Invalid("settings")
        val settings = when (val r = SettingsJson.apply(current, settingsJson)) {
            is SettingsJson.Result.Ok -> r.settings
            is SettingsJson.Result.Invalid -> return Result.Invalid("settings")
        }

        val favorites = packageList(json.opt("favorites")) ?: return Result.Invalid("favorites")
        val hidden = packageList(json.opt("hidden"))?.toSet() ?: return Result.Invalid("hidden")
        val appOrder = packageList(json.opt("appOrder")) ?: return Result.Invalid("appOrder")
        val labels = labels(json.opt("labels")) ?: return Result.Invalid("labels")

        val sortName = json.opt("sortOrder") as? String ?: return Result.Invalid("sortOrder")
        val sortOrder = AppSortOrder.entries.firstOrNull { it.name == sortName } ?: return Result.Invalid("sortOrder")

        val celsius = json.opt("celsius") as? Boolean ?: return Result.Invalid("celsius")

        val weather = when (val w = json.opt("weather")) {
            null -> null
            is JSONObject -> weather(w) ?: return Result.Invalid("weather")
            else -> return Result.Invalid("weather")
        }

        val urls = wallpaperUrls(json.opt("wallpaperUrls")) ?: return Result.Invalid("wallpaperUrls")

        return Result.Ok(
            BackupData(settings, favorites, hidden, labels, appOrder, sortOrder, weather, celsius, urls)
        )
    }

    /** A list of well-formed, distinct package names, or null if anything in it is off. */
    private fun packageList(value: Any?): List<String>? {
        val array = value as? JSONArray ?: return null
        if (array.length() > MAX_PACKAGES) return null
        val out = ArrayList<String>(array.length())
        for (i in 0 until array.length()) {
            val p = array.opt(i) as? String ?: return null
            if (!SetupValidation.isPackageName(p)) return null
            if (p !in out) out += p
        }
        return out
    }

    private fun labels(value: Any?): Map<String, String>? {
        val obj = value as? JSONObject ?: return null
        if (obj.length() > MAX_PACKAGES) return null
        val out = LinkedHashMap<String, String>()
        for (pkg in obj.keys()) {
            if (!SetupValidation.isPackageName(pkg)) return null
            val raw = obj.opt(pkg) as? String ?: return null
            val clean = PrefsCodec.sanitizeLabel(raw)
            if (clean.isNotEmpty()) out[pkg] = clean
        }
        return out
    }

    private fun weather(w: JSONObject): BackupWeather? {
        val city = place(w.opt("city")) ?: return null
        val country = place(w.opt("country")) ?: return null
        val lat = (w.opt("latitude") as? Number)?.toDouble() ?: return null
        val lon = (w.opt("longitude") as? Number)?.toDouble() ?: return null
        if (!lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        if (city.isEmpty()) return null
        return BackupWeather(city, country, lat, lon)
    }

    /** Place names: plain text, no stored-data separators, bounded. */
    private fun place(value: Any?): String? {
        val s = value as? String ?: return null
        if (s.length > MAX_PLACE_LENGTH || s.any { it == '|' || it == '\n' || it == '\r' || it.isISOControl() }) return null
        return s.trim()
    }

    private fun wallpaperUrls(value: Any?): List<String>? {
        val array = value as? JSONArray ?: return null
        if (array.length() > MAX_WALLPAPER_URLS) return null
        val out = ArrayList<String>()
        for (i in 0 until array.length()) {
            val url = (array.opt(i) as? String)?.trim() ?: return null
            if (!isHttpsUrl(url)) return null
            if (url !in out) out += url
        }
        return out
    }

    fun isHttpsUrl(url: String): Boolean = SetupValidation.isHttpsUrl(url)
}
