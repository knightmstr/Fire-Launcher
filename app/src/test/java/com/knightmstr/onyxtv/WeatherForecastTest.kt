package com.knightmstr.onyxtv

import com.knightmstr.onyxtv.data.ForecastDay
import com.knightmstr.onyxtv.data.WeatherForecast
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherForecastTest {

    private fun daily(vararg days: List<Any?>): JSONObject {
        val keys = listOf("time", "weather_code", "temperature_2m_max", "temperature_2m_min",
            "precipitation_probability_max", "precipitation_sum", "wind_gusts_10m_max")
        val o = JSONObject()
        keys.forEachIndexed { k, key -> o.put(key, JSONArray(days.map { it[k] ?: JSONObject.NULL })) }
        return o
    }

    private fun day(date: String, code: Int = 1, high: Double? = 90.0, low: Double = 75.0,
                    rain: Int? = 10, mm: Double = 0.0, gust: Double = 15.0) = listOf(date, code, high, low, rain, mm, gust)

    @Test
    fun `parses up to five days`() {
        val d = WeatherForecast.parseDaily(daily(
            day("2026-11-28"), day("2026-11-29"), day("2026-11-30"), day("2026-12-01"), day("2026-12-02"), day("2026-12-03")))
        assertEquals(5, d.size)
        assertEquals(90, d[0].highF)
        assertEquals(32, d[0].highC)
        assertEquals(10, d[0].rainChance)
    }

    @Test
    fun `bad days drop out instead of crashing`() {
        val d = WeatherForecast.parseDaily(daily(day("not-a-date"), day("2026-11-29", high = null), day("2026-11-30", rain = null)))
        assertEquals(1, d.size)
        assertEquals(null, d[0].rainChance)
        assertTrue(WeatherForecast.parseDaily(null).isEmpty())
        assertTrue(WeatherForecast.parseDaily(JSONObject()).isEmpty())
    }

    @Test
    fun `heads-ups name the soonest day of each kind`() {
        val days = listOf(
            ForecastDay("2026-11-28", 2, 93, 77, 20, 1.0, 20),
            ForecastDay("2026-11-29", 95, 91, 76, 80, 12.0, 30),
            ForecastDay("2026-11-30", 95, 90, 76, 90, 45.0, 55),
            ForecastDay("2026-12-01", 1, 101, 80, 0, 0.0, 10),
            ForecastDay("2026-12-02", 1, 95, 80, 0, 0.0, 10)
        )
        val msgs = WeatherForecast.headsUps(days).map { it.message }
        assertEquals(listOf("Thunderstorms tomorrow", "Heavy rain on Monday", "Strong winds on Monday", "Extreme heat on Tuesday"), msgs)
    }

    @Test
    fun `ordinary weather gives no heads-up`() {
        val days = (0 until 5).map { ForecastDay("2026-11-${28 + it}", 2, 92, 77, 40, 5.0, 25) }
        assertTrue(WeatherForecast.headsUps(days).isEmpty())
    }

    @Test
    fun `day names`() {
        assertEquals("today", WeatherForecast.dayName("2026-11-28", 0))
        assertEquals("tomorrow", WeatherForecast.dayName("2026-11-29", 1))
        assertEquals("on Monday", WeatherForecast.dayName("2026-11-30", 2))
        assertEquals("Today", WeatherForecast.shortDay("2026-11-28", 0))
        assertEquals("Mon", WeatherForecast.shortDay("2026-11-30", 2))
        assertEquals("Saturday", WeatherForecast.weekday("2026-11-28"))
        assertEquals(null, WeatherForecast.weekday("junk"))
    }

    @Test
    fun `icons for codes`() {
        assertEquals("thunder", WeatherForecast.iconType(95))
        assertEquals("rain", WeatherForecast.iconType(61))
        assertEquals("clear", WeatherForecast.iconType(0))
    }
}
