package com.example.firelauncher.data

/**
 * Data model representing current weather conditions for the top-bar launcher widget.
 */
data class WeatherData(
    val city: String = "Seattle",
    val temperatureF: Int = 68,
    val temperatureC: Int = 20,
    val condition: String = "Partly Cloudy",
    val iconType: String = "partly_cloudy", // "clear", "partly_cloudy", "cloudy", "rain", "snow", "thunder"
    val highF: Int = 72,
    val lowF: Int = 55,
    val isCelsius: Boolean = false
) {
    val displayTemperature: String
        get() = if (isCelsius) "$temperatureC°C" else "$temperatureF°F"

    val displayHighLow: String
        get() = if (isCelsius) {
            "H:${((highF - 32) * 5 / 9)}° L:${((lowF - 32) * 5 / 9)}°"
        } else {
            "H:$highF° L:$lowF°"
        }

    companion object {
        val CITY_PRESETS = listOf(
            WeatherData("Seattle", 64, 18, "Partly Cloudy", "partly_cloudy", 68, 52),
            WeatherData("New York", 72, 22, "Sunny", "clear", 76, 61),
            WeatherData("Los Angeles", 78, 26, "Clear", "clear", 82, 65),
            WeatherData("Chicago", 61, 16, "Breezy", "cloudy", 65, 48),
            WeatherData("London", 59, 15, "Light Rain", "rain", 63, 50),
            WeatherData("Tokyo", 66, 19, "Clear", "clear", 70, 54),
            WeatherData("Paris", 63, 17, "Partly Cloudy", "partly_cloudy", 68, 52),
            WeatherData("Berlin", 57, 14, "Overcast", "cloudy", 61, 46),
            WeatherData("Sydney", 74, 23, "Sunny", "clear", 79, 62),
            WeatherData("Miami", 84, 29, "Scattered Showers", "rain", 88, 76),
            WeatherData("Denver", 65, 18, "Sunny", "clear", 71, 45)
        )

        fun forCity(cityName: String, isCelsius: Boolean = false): WeatherData {
            val preset = CITY_PRESETS.find { it.city.equals(cityName, ignoreCase = true) }
                ?: WeatherData(city = cityName, temperatureF = 70, temperatureC = 21, condition = "Fair", iconType = "clear", highF = 75, lowF = 58)
            return preset.copy(isCelsius = isCelsius)
        }
    }
}
