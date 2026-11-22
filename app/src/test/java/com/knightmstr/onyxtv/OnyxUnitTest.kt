package com.knightmstr.onyxtv

import com.knightmstr.onyxtv.data.AppSortOrder
import com.knightmstr.onyxtv.data.WeatherData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnyxUnitTest {

    @Test
    fun `AppSortOrder fromName resolves valid and fallback values`() {
        assertEquals(AppSortOrder.ALPHABETICAL, AppSortOrder.fromName(null))
        assertEquals(AppSortOrder.ALPHABETICAL, AppSortOrder.fromName("unknown"))
        assertEquals(AppSortOrder.MOST_RECENT, AppSortOrder.fromName("most_recent"))
        assertEquals(AppSortOrder.MOST_FREQUENT, AppSortOrder.fromName("MOST_FREQUENT"))
    }

    @Test
    fun `WeatherData formats temperature units accurately`() {
        val fahrenheit = WeatherData.defaultSeattle(isCelsius = false)
        assertEquals("64°F", fahrenheit.displayTemperature)
        assertEquals("H:68° L:52°", fahrenheit.displayHighLow)

        val celsius = WeatherData.defaultSeattle(isCelsius = true)
        assertEquals("18°C", celsius.displayTemperature)
        assertEquals("H:20° L:11°", celsius.displayHighLow)
    }
}
