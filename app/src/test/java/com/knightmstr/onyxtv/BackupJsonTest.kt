package com.knightmstr.onyxtv

import com.knightmstr.onyxtv.data.AppSortOrder
import com.knightmstr.onyxtv.data.BackupData
import com.knightmstr.onyxtv.data.BackupWeather
import com.knightmstr.onyxtv.data.CardShape
import com.knightmstr.onyxtv.data.LauncherSettings
import com.knightmstr.onyxtv.data.UpdateChannel
import com.knightmstr.onyxtv.upload.BackupJson
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupJsonTest {

    private val sample = BackupData(
        settings = LauncherSettings(cardShape = CardShape.PILL, tileWidthDp = 120, nightDim = false),
        favorites = listOf("com.netflix.ninja", "com.plexapp.android"),
        hidden = setOf("org.xbmc.kodi"),
        labels = mapOf("com.netflix.ninja" to "Movies"),
        appOrder = listOf("com.plexapp.android", "com.netflix.ninja"),
        sortOrder = AppSortOrder.CUSTOM,
        weather = BackupWeather("Darwin", "NT, AU", -12.46, 130.84),
        celsius = true,
        wallpaperUrls = listOf("https://example.com/a.jpg")
    )

    private fun json() = JSONObject(BackupJson.toJson(sample).toString())

    private fun parse(j: JSONObject, current: LauncherSettings = LauncherSettings()) = BackupJson.parse(j, current)

    @Test
    fun `round trip`() {
        assertEquals(BackupJson.Result.Ok(sample), parse(json()))
    }

    @Test
    fun `backup never holds kids mode, pin, usage or update channel`() {
        val text = BackupJson.toJson(sample).toString().lowercase()
        for (word in listOf("kids", "pin", "usage", "launchcount", "updatechannel")) {
            assertFalse(word, text.contains(word))
        }
    }

    @Test
    fun `restore keeps the TV's own update channel`() {
        val r = parse(json(), LauncherSettings(updateChannel = UpdateChannel.BETA)) as BackupJson.Result.Ok
        assertEquals(UpdateChannel.BETA, r.data.settings.updateChannel)
    }

    @Test
    fun `wrong or future files are refused`() {
        assertTrue(parse(JSONObject("{}")) is BackupJson.Result.Invalid)
        assertTrue(parse(json().put("format", "something-else")) is BackupJson.Result.Invalid)
        assertEquals(BackupJson.Result.Invalid("newer_version"), parse(json().put("version", 99)))
        assertTrue(parse(json().put("version", "1")) is BackupJson.Result.Invalid)
    }

    @Test
    fun `any bad part rejects the whole file`() {
        val cases = listOf(
            json().put("favorites", JSONArray(listOf("com.ok", "../etc"))),
            json().put("hidden", "org.xbmc.kodi"),
            json().put("appOrder", JSONArray(listOf(1, 2))),
            json().put("labels", JSONObject().put("bad pkg", "x")),
            json().put("labels", JSONObject().put("com.ok", 5)),
            json().put("sortOrder", "custom"),
            json().put("celsius", "yes"),
            json().put("weather", JSONObject().put("city", "X").put("country", "Y").put("latitude", 200).put("longitude", 0)),
            json().put("weather", JSONObject().put("city", "Evil|city").put("country", "Y").put("latitude", 1).put("longitude", 1)),
            json().put("weather", "Darwin"),
            json().put("wallpaperUrls", JSONArray(listOf("http://example.com/a.jpg"))),
            json().put("wallpaperUrls", JSONArray(listOf("javascript:alert(1)"))),
            json().put("wallpaperUrls", JSONArray(listOf("file:///data/data/x"))),
            json().put("wallpaperUrls", JSONArray(listOf("https://user:pass@example.com/a.jpg"))),
            json().put("wallpaperUrls", JSONArray(listOf("https://example.com/a b.jpg"))),
            json().put("settings", JSONObject().put("tileWidthDp", 9999)),
            json().put("settings", JSONObject().put("updateChannel", "BETA")),
            json().put("favorites", JSONArray((1..501).map { "p.app$it" }))
        )
        cases.forEachIndexed { i, c -> assertTrue("case $i", parse(c) is BackupJson.Result.Invalid) }
    }

    @Test
    fun `labels are sanitised and blank ones dropped`() {
        val j = json().put("labels", JSONObject().put("com.a", "Line\nbreak|pipe" + "x".repeat(100)).put("com.b", "   "))
        val r = parse(j) as BackupJson.Result.Ok
        val label = r.data.labels.getValue("com.a")
        assertFalse(label.contains("\n") || label.contains("|"))
        assertTrue(label.length <= 40)
        assertFalse(r.data.labels.containsKey("com.b"))
    }

    @Test
    fun `folders round trip and their dock keys are allowed`() {
        val withFolders = sample.copy(
            favorites = listOf("com.netflix.ninja", "folder:f1"),
            folders = listOf(com.knightmstr.onyxtv.data.AppFolder("f1", "Movies", listOf("com.plexapp.android", "org.xbmc.kodi")))
        )
        val back = parse(JSONObject(BackupJson.toJson(withFolders).toString()))
        assertEquals(BackupJson.Result.Ok(withFolders), back)
    }

    @Test
    fun `a dock folder key needs its folder, and folders are checked`() {
        val j = json().put("favorites", JSONArray(listOf("folder:f1")))
        assertTrue(parse(j) is BackupJson.Result.Invalid)
        val bad = listOf(
            JSONArray().put(JSONObject().put("id", "../x").put("name", "A").put("apps", JSONArray(listOf("a.b")))),
            JSONArray().put(JSONObject().put("id", "f1").put("name", "A").put("apps", JSONArray(listOf("bad pkg")))),
            JSONArray().put(JSONObject().put("id", "f1").put("name", "A").put("apps", JSONArray())),
            JSONArray().put(JSONObject().put("id", "f1").put("name", "A").put("apps", JSONArray(listOf("a.b"))))
                .put(JSONObject().put("id", "f1").put("name", "B").put("apps", JSONArray(listOf("c.d"))))
        )
        bad.forEachIndexed { i, f -> assertTrue("case $i", parse(json().put("folders", f)) is BackupJson.Result.Invalid) }
    }

    @Test
    fun `older backups without folders still restore`() {
        val j = json().apply { remove("folders") }
        assertTrue(parse(j) is BackupJson.Result.Ok)
    }

    @Test
    fun `weather is optional`() {
        val j = json().apply { remove("weather") }
        assertEquals(null, (parse(j) as BackupJson.Result.Ok).data.weather)
    }
}
