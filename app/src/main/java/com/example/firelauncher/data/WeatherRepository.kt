package com.example.firelauncher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Repository to query live weather and geocoding search from Open-Meteo API.
 * Free, accurate, and requires no API keys.
 */
class WeatherRepository {

    /**
     * Searches cities globally using Open-Meteo Geocoding API.
     */
    suspend fun searchCities(query: String): List<CityLocationModel> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=8&language=en&format=json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val root = JSONObject(text)
                if (!root.has("results")) return@withContext emptyList()
                val array = root.getJSONArray("results")
                val list = mutableListOf<CityLocationModel>()

                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    list.add(
                        CityLocationModel(
                            id = "geo_" + item.optLong("id", i.toLong()),
                            name = item.optString("name", "Unknown"),
                            admin1 = if (item.has("admin1")) item.optString("admin1") else null,
                            country = item.optString("country", ""),
                            countryCode = item.optString("country_code", ""),
                            latitude = item.optDouble("latitude", 0.0),
                            longitude = item.optDouble("longitude", 0.0)
                        )
                    )
                }
                return@withContext list
            }
        } catch (_: Exception) {
            // return empty list on network error
        }
        emptyList()
    }

    /**
     * Fetches live weather for specified coordinates from Open-Meteo Forecast API.
     */
    suspend fun fetchLiveWeather(
        cityName: String,
        country: String,
        lat: Double,
        lon: Double,
        isCelsius: Boolean
    ): WeatherData = withContext(Dispatchers.IO) {
        try {
            val urlStr = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto&temperature_unit=fahrenheit&wind_speed_unit=mph"

            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val root = JSONObject(text)
                val current = root.getJSONObject("current")
                val daily = root.optJSONObject("daily")

                val tempF = Math.round(current.optDouble("temperature_2m", 68.0)).toInt()
                val tempC = Math.round(((tempF - 32) * 5.0) / 9.0).toInt()

                val feelsLikeF = Math.round(current.optDouble("apparent_temperature", tempF.toDouble())).toInt()
                val feelsLikeC = Math.round(((feelsLikeF - 32) * 5.0) / 9.0).toInt()

                val wmoCode = current.optInt("weather_code", 1)
                val isDay = current.optInt("is_day", 1) == 1

                val highF = if (daily != null && daily.has("temperature_2m_max")) {
                    Math.round(daily.getJSONArray("temperature_2m_max").optDouble(0, (tempF + 5).toDouble())).toInt()
                } else tempF + 5

                val lowF = if (daily != null && daily.has("temperature_2m_min")) {
                    Math.round(daily.getJSONArray("temperature_2m_min").optDouble(0, (tempF - 6).toDouble())).toInt()
                } else tempF - 6

                val humidity = Math.round(current.optDouble("relative_humidity_2m", 55.0)).toInt()
                val windMph = Math.round(current.optDouble("wind_speed_10m", 8.0)).toInt()

                val (conditionStr, iconStr) = mapWmoCode(wmoCode, isDay)

                return@withContext WeatherData(
                    city = cityName,
                    country = country,
                    latitude = lat,
                    longitude = lon,
                    temperatureF = tempF,
                    temperatureC = tempC,
                    feelsLikeF = feelsLikeF,
                    feelsLikeC = feelsLikeC,
                    condition = conditionStr,
                    iconType = iconStr,
                    highF = highF,
                    lowF = lowF,
                    humidity = humidity,
                    windMph = windMph,
                    isCelsius = isCelsius,
                    isLive = true
                )
            }
        } catch (_: Exception) {
            // fallback
        }

        // Fallback default
        WeatherData(
            city = cityName,
            country = country,
            latitude = lat,
            longitude = lon,
            temperatureF = 68,
            temperatureC = 20,
            condition = "Partly Cloudy",
            iconType = "partly_cloudy",
            isCelsius = isCelsius,
            isLive = false
        )
    }

    private fun mapWmoCode(code: Int, isDay: Boolean): Pair<String, String> {
        return when (code) {
            0 -> (if (isDay) "Sunny" else "Clear Sky") to "clear"
            1 -> (if (isDay) "Mainly Sunny" else "Mainly Clear") to "clear"
            2 -> "Partly Cloudy" to "partly_cloudy"
            3 -> "Overcast" to "cloudy"
            45, 48 -> "Fog & Mist" to "cloudy"
            51, 53, 55, 56, 57 -> "Light Drizzle" to "rain"
            61, 63, 65, 66, 67 -> "Rain" to "rain"
            71, 73, 75, 77 -> "Snow" to "snow"
            80, 81, 82 -> "Passing Showers" to "rain"
            85, 86 -> "Snow Showers" to "snow"
            95, 96, 99 -> "Thunderstorm" to "thunder"
            else -> "Fair" to "partly_cloudy"
        }
    }
}
