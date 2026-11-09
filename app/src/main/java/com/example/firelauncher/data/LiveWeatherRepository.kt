package com.example.firelauncher.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Live Weather Repository that fetches real-time weather data from Open-Meteo.
 * Open-Meteo provides a free, open API with zero keys required.
 */
class LiveWeatherRepository {

    companion object {
        private const val TAG = "LiveWeatherRepo"
    }

    /**
     * Fetches live weather for [cityName].
     * First queries Open-Meteo geocoding, then fetches current weather and daily max/min temperatures.
     */
    suspend fun getLiveWeather(cityName: String, isCelsius: Boolean = false): WeatherData {
        return withContext(Dispatchers.IO) {
            try {
                val coords = geocodeCity(cityName)
                if (coords != null) {
                    val weather = fetchForecast(coords.first, coords.second, coords.third, isCelsius)
                    if (weather != null) {
                        return@withContext weather
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load live weather from Open-Meteo for $cityName: ${e.message}")
            }
            // Graceful offline fallback
            WeatherData.forCity(cityName, isCelsius)
        }
    }

    private fun geocodeCity(cityName: String): Triple<String, Double, Double>? {
        val encoded = URLEncoder.encode(cityName.trim(), "UTF-8")
        val endpoint = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json"
        val response = httpGet(endpoint) ?: return null

        val json = JSONObject(response)
        val results = json.optJSONArray("results") ?: return null
        if (results.length() == 0) return null

        val first = results.getJSONObject(0)
        val name = first.optString("name", cityName)
        val lat = first.optDouble("latitude")
        val lon = first.optDouble("longitude")
        return Triple(name, lat, lon)
    }

    private fun fetchForecast(name: String, lat: Double, lon: Double, isCelsius: Boolean): WeatherData? {
        val endpoint = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,is_day,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min&timezone=auto"
        val response = httpGet(endpoint) ?: return null

        val json = JSONObject(response)
        val current = json.optJSONObject("current") ?: return null
        val daily = json.optJSONObject("daily")

        val tempC = Math.round(current.optDouble("temperature_2m", 20.0)).toInt()
        val tempF = Math.round((tempC * 9.0) / 5.0 + 32.0).toInt()

        val maxArray = daily?.optJSONArray("temperature_2m_max")
        val minArray = daily?.optJSONArray("temperature_2m_min")

        val highC = if (maxArray != null && maxArray.length() > 0) Math.round(maxArray.optDouble(0, (tempC + 3).toDouble())).toInt() else tempC + 3
        val lowC = if (minArray != null && minArray.length() > 0) Math.round(minArray.optDouble(0, (tempC - 4).toDouble())).toInt() else tempC - 4

        val highF = Math.round((highC * 9.0) / 5.0 + 32.0).toInt()
        val lowF = Math.round((lowC * 9.0) / 5.0 + 32.0).toInt()

        val weatherCode = current.optInt("weather_code", 0)
        val isDay = current.optInt("is_day", 1) == 1

        val (condition, iconType) = mapWmoCode(weatherCode, isDay)

        return WeatherData(
            city = name,
            temperatureF = tempF,
            temperatureC = tempC,
            condition = condition,
            iconType = iconType,
            highF = highF,
            lowF = lowF,
            isCelsius = isCelsius
        )
    }

    private fun mapWmoCode(code: Int, isDay: Boolean): Pair<String, String> {
        return when (code) {
            0 -> (if (isDay) "Clear Sky" else "Clear Night") to "clear"
            1, 2 -> "Partly Cloudy" to "partly_cloudy"
            3 -> "Overcast" to "cloudy"
            45, 48 -> "Foggy" to "cloudy"
            51, 53, 55 -> "Drizzle" to "rain"
            61, 63, 65, 80, 81, 82 -> "Rain" to "rain"
            71, 73, 75, 77, 85, 86 -> "Snow" to "snow"
            95, 96, 99 -> "Thunderstorm" to "thunder"
            else -> "Fair" to "partly_cloudy"
        }
    }

    private fun httpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "FireLauncher/1.0 (Amazon Fire TV)")
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                sb.toString()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
