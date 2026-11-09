package com.example.firelauncher.data

/**
 * Data model representing current weather conditions for the top-bar launcher widget.
 * Sourced live from the free and accurate Open-Meteo Weather API.
 */
data class WeatherData(
    val city: String = "Seattle",
    val country: String = "WA, USA",
    val latitude: Double = 47.6062,
    val longitude: Double = -122.3321,
    val temperatureF: Int = 68,
    val temperatureC: Int = 20,
    val feelsLikeF: Int = 68,
    val feelsLikeC: Int = 20,
    val condition: String = "Partly Cloudy",
    val iconType: String = "partly_cloudy", // "clear", "partly_cloudy", "cloudy", "rain", "snow", "thunder"
    val highF: Int = 72,
    val lowF: Int = 55,
    val humidity: Int = 65,
    val windMph: Int = 8,
    val isCelsius: Boolean = false,
    val isLive: Boolean = true
) {
    val displayTemperature: String
        get() = if (isCelsius) "$temperatureC°C" else "$temperatureF°F"

    val displayHighLow: String
        get() = if (isCelsius) {
            "H:${((highF - 32) * 5 / 9)}° L:${((lowF - 32) * 5 / 9)}°"
        } else {
            "H:$highF° L:$lowF°"
        }

    val displayLocation: String
        get() = if (country.isNotBlank()) "$city, $country" else city

    companion object {
        fun defaultSeattle(isCelsius: Boolean = false): WeatherData {
            return WeatherData(
                city = "Seattle",
                country = "WA, USA",
                latitude = 47.6062,
                longitude = -122.3321,
                temperatureF = 64,
                temperatureC = 18,
                condition = "Partly Cloudy",
                iconType = "partly_cloudy",
                highF = 68,
                lowF = 52,
                isCelsius = isCelsius
            )
        }
    }
}

/**
 * Model representing a geocoding search result when searching for cities.
 */
data class CityLocationModel(
    val id: String,
    val name: String,
    val admin1: String? = null,
    val country: String,
    val countryCode: String? = null,
    val latitude: Double,
    val longitude: Double
) {
    val displayName: String
        get() = if (!admin1.isNullOrBlank()) "$name, $admin1, $country" else "$name, $country"
}
