package com.knightmstr.onyxtv

import com.knightmstr.onyxtv.data.HomeLayout
import com.knightmstr.onyxtv.data.LauncherSettings
import com.knightmstr.onyxtv.data.WallpaperFit
import com.knightmstr.onyxtv.upload.SettingsJson
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsJsonTest {

    private fun apply(json: String, current: LauncherSettings = LauncherSettings()) =
        SettingsJson.apply(current, JSONObject(json))

    @Test
    fun `round trip keeps every exposed setting`() {
        val s = LauncherSettings(homeLayout = HomeLayout.CLEAN, tileWidthDp = 120, wallpaperFit = WallpaperFit.FIT,
            wallpaperIntervalMinutes = 60, screensaverTimeoutMinutes = 0, nightDim = false, showAllAppsRow = true)
        val back = apply(SettingsJson.toJson(s).toString())
        assertEquals(SettingsJson.Result.Ok(s), back)
    }

    @Test
    fun `json exposes exactly the allowed keys`() {
        assertEquals(SettingsJson.KEYS, SettingsJson.toJson(LauncherSettings()).keySet())
    }

    @Test
    fun `whole numbers may arrive as doubles`() {
        val r = apply("""{"tileWidthDp":130.0}""")
        assertTrue(r is SettingsJson.Result.Ok)
        assertEquals(130, (r as SettingsJson.Result.Ok).settings.tileWidthDp)
    }

    @Test
    fun `one bad key rejects the whole change`() {
        val r = apply("""{"nightDim":false,"tileWidthDp":131}""")
        assertEquals(SettingsJson.Result.Invalid("tileWidthDp"), r)
        for (bad in listOf("""{"tileWidthDp":130.5}""", """{"tileWidthDp":1e12}""", """{"tileWidthDp":null}""",
            """{"homeLayout":null}""", """{"homeLayout":["DOCK"]}""", """{"updateChannel":"BETA"}""")) {
            assertTrue(bad, apply(bad) is SettingsJson.Result.Invalid)
        }
    }

    @Test
    fun `night hours accept 0 to 23 only`() {
        val r = apply("""{"nightStartHour":21,"nightEndHour":7}""") as SettingsJson.Result.Ok
        assertEquals(21, r.settings.nightStartHour)
        assertEquals(7, r.settings.nightEndHour)
        for (bad in listOf("24", "-1", "6.5", "\"6\"", "true")) {
            assertTrue(bad, apply("""{"nightEndHour":$bad}""") is SettingsJson.Result.Invalid)
        }
    }

    @Test
    fun `night window handles midnight and equal hours`() {
        assertTrue(LauncherSettings.isNight(23, 22, 6))
        assertTrue(LauncherSettings.isNight(0, 22, 6))
        assertTrue(LauncherSettings.isNight(5, 22, 6))
        assertFalse(LauncherSettings.isNight(6, 22, 6))
        assertFalse(LauncherSettings.isNight(21, 22, 6))
        assertTrue(LauncherSettings.isNight(14, 13, 15))
        assertFalse(LauncherSettings.isNight(15, 13, 15))
        assertFalse(LauncherSettings.isNight(12, 0, 0))
        assertTrue(LauncherSettings.isNight(0, 0, 1))
        assertEquals("12 am", LauncherSettings.describeHour(0))
        assertEquals("12 pm", LauncherSettings.describeHour(12))
        assertEquals("10 pm", LauncherSettings.describeHour(22))
    }

    @Test
    fun `every option the page is offered is accepted`() {
        val opts = SettingsJson.options()
        for (key in listOf("homeLayout", "cardShape", "wallpaperSource", "wallpaperFit",
            "wallpaperIntervalMinutes", "screensaverTimeoutMinutes", "nightStartHour", "nightEndHour")) {
            val arr = opts.getJSONArray(key)
            for (i in 0 until arr.length()) {
                val v = arr.getJSONObject(i).get("value")
                assertTrue("$key=$v", apply(JSONObject().put(key, v).toString()) is SettingsJson.Result.Ok)
            }
        }
    }
}
