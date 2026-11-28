package com.knightmstr.onyxtv.data

import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone

/** One day of the forecast. Temperatures in °F like the rest of [WeatherData]. */
data class ForecastDay(
    /** ISO date (yyyy-MM-dd) in the city's own time zone. */
    val date: String,
    val weatherCode: Int,
    val highF: Int,
    val lowF: Int,
    /** Chance of rain, 0–100, or null if not given. */
    val rainChance: Int?,
    /** Rain/snow total in mm. */
    val precipitationMm: Double,
    val windGustMph: Int
) {
    val highC: Int get() = fToC(highF)
    val lowC: Int get() = fToC(lowF)

    companion object {
        fun fToC(f: Int): Int = Math.round((f - 32) * 5.0 / 9.0).toInt()
    }
}

/**
 * A short weather heads-up worked out from the forecast, e.g. "Thunderstorms tomorrow".
 * Not an official warning: Open-Meteo has none, so this only points out notable forecast days.
 */
data class WeatherHeadsUp(val message: String, val dayIndex: Int)

object WeatherForecast {

    /** Days shown in the forecast. */
    const val DAYS = 5

    // Thresholds for a heads-up. Gusts and heat are set where most weather services start warning.
    private const val HEAVY_RAIN_MM = 30.0
    private const val STRONG_GUST_MPH = 47 // ~75 km/h
    private const val EXTREME_HEAT_F = 100 // ~38 °C
    private val THUNDER = setOf(95, 96, 99)
    private val HEAVY_SNOW = setOf(75, 86)

    /** Parses Open-Meteo's `daily` block. Missing values make that day drop out, never crash. */
    fun parseDaily(daily: JSONObject?): List<ForecastDay> {
        if (daily == null) return emptyList()
        val dates = daily.optJSONArray("time") ?: return emptyList()
        val codes = daily.optJSONArray("weather_code")
        val highs = daily.optJSONArray("temperature_2m_max")
        val lows = daily.optJSONArray("temperature_2m_min")
        val rain = daily.optJSONArray("precipitation_probability_max")
        val precip = daily.optJSONArray("precipitation_sum")
        val gusts = daily.optJSONArray("wind_gusts_10m_max")
        val out = ArrayList<ForecastDay>()
        for (i in 0 until minOf(dates.length(), DAYS)) {
            val date = dates.optString(i).takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) } ?: continue
            val high = highs?.optDouble(i, Double.NaN) ?: Double.NaN
            val low = lows?.optDouble(i, Double.NaN) ?: Double.NaN
            if (high.isNaN() || low.isNaN()) continue
            out += ForecastDay(
                date = date,
                weatherCode = codes?.optInt(i, 2) ?: 2,
                highF = Math.round(high).toInt(),
                lowF = Math.round(low).toInt(),
                rainChance = rain?.optDouble(i, Double.NaN)?.takeIf { !it.isNaN() }?.let { Math.round(it).toInt().coerceIn(0, 100) },
                precipitationMm = precip?.optDouble(i, 0.0)?.takeIf { !it.isNaN() } ?: 0.0,
                windGustMph = Math.round(gusts?.optDouble(i, 0.0)?.takeIf { !it.isNaN() } ?: 0.0).toInt()
            )
        }
        return out
    }

    /**
     * Notable days, soonest first; at most one heads-up per kind. Open-Meteo's first day is
     * today in the city's own time zone, so the index says how far ahead a day is.
     */
    fun headsUps(days: List<ForecastDay>): List<WeatherHeadsUp> {
        val out = ArrayList<WeatherHeadsUp>()
        fun add(kind: String, test: (ForecastDay) -> Boolean) {
            val i = days.indexOfFirst(test)
            if (i >= 0) out += WeatherHeadsUp("$kind ${dayName(days[i].date, i)}", i)
        }
        add("Thunderstorms") { it.weatherCode in THUNDER }
        add("Heavy rain") { it.precipitationMm >= HEAVY_RAIN_MM }
        add("Strong winds") { it.windGustMph >= STRONG_GUST_MPH }
        add("Extreme heat") { it.highF >= EXTREME_HEAT_F }
        add("Heavy snow") { it.weatherCode in HEAVY_SNOW }
        return out.sortedBy { it.dayIndex }
    }

    /** "today", "tomorrow", or "on Friday". */
    fun dayName(date: String, index: Int): String = when (index) {
        0 -> "today"
        1 -> "tomorrow"
        else -> weekday(date)?.let { "on $it" } ?: "soon"
    }

    /** "Today", "Tue", … for the forecast strip. */
    fun shortDay(date: String, index: Int): String =
        if (index == 0) "Today" else weekday(date)?.take(3).orEmpty()

    /** Icon type for a WMO weather code, matching [WeatherData.iconType]. */
    fun iconType(code: Int): String = when (code) {
        0, 1 -> "clear"
        2 -> "partly_cloudy"
        3, 45, 48 -> "cloudy"
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> "rain"
        71, 73, 75, 77, 85, 86 -> "snow"
        95, 96, 99 -> "thunder"
        else -> "partly_cloudy"
    }

    private val WEEKDAYS = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    /** Weekday name of a yyyy-MM-dd date (Calendar, since java.time needs API 26). */
    fun weekday(date: String): String? {
        val parts = date.split('-').mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return null
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(parts[0], parts[1] - 1, parts[2])
        }
        return WEEKDAYS.getOrNull(cal.get(Calendar.DAY_OF_WEEK) - 1)
    }
}
