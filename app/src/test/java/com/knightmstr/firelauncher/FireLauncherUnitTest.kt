package com.knightmstr.firelauncher

import com.knightmstr.firelauncher.data.AppSortOrder
import com.knightmstr.firelauncher.data.CleanTvManager
import com.knightmstr.firelauncher.data.WeatherData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FireLauncherUnitTest {

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

    @Test
    fun `CleanTvManager targets all 5 core Amazon bloatware packages reversibly`() {
        val expectedPackages = listOf(
            "com.amazon.advertising.firetv",
            "com.amazon.device.sale.service",
            "com.amazon.client.metrics",
            "com.amazon.device.crashmanager",
            "com.amazon.ftv.screensaver"
        )
        val registered = CleanTvManager.SAFE_DEBLOAT_PACKAGES.map { it.packageName }
        expectedPackages.forEach { pkg ->
            assertTrue("Expected $pkg to be in SAFE_DEBLOAT_PACKAGES", registered.contains(pkg))
        }

        val debloatScript = CleanTvManager.generateDebloatScript()
        val restoreScript = CleanTvManager.generateRestoreScript()
        assertTrue(debloatScript.contains("pm disable-user --user 0"))
        assertTrue(restoreScript.contains("pm enable"))
        assertFalse(debloatScript.contains("pm uninstall"))
    }
}
