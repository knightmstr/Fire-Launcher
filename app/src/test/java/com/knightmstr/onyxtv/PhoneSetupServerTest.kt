package com.knightmstr.onyxtv

import com.knightmstr.onyxtv.data.CardShape
import com.knightmstr.onyxtv.data.LauncherSettings
import com.knightmstr.onyxtv.data.UpdateChannel
import com.knightmstr.onyxtv.upload.PhoneSetupBridge
import com.knightmstr.onyxtv.upload.PhoneSetupServer
import com.knightmstr.onyxtv.upload.SetupApp
import com.knightmstr.onyxtv.upload.SetupValidation
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.Socket
import java.net.URL

/**
 * Starts the real phone-setup server on localhost with a fake bridge and attacks it: missing or
 * wrong tokens, wrong methods, cross-site style requests, oversized and malformed bodies, bad
 * package names, path tricks, and changes while Kids mode is on.
 */
class PhoneSetupServerTest {

    private class FakeBridge : PhoneSetupBridge {
        val installed = mutableListOf(
            SetupApp("com.netflix.ninja", "Netflix", "Netflix", hidden = false),
            SetupApp("com.plexapp.android", "Plex", "Plex", hidden = false),
            SetupApp("org.xbmc.kodi", "Kodi", "Kodi", hidden = true)
        )
        var favorites = listOf("com.plexapp.android", "com.gone.app")
        val hiddenCalls = mutableListOf<Pair<String, Boolean>>()
        val renameCalls = mutableListOf<Pair<String, String?>>()
        var locked = false
        var changes = 0
        val photos = mutableListOf<File>()

        override fun listPhotos(): List<File> = photos
        override fun savePhoto(bytes: ByteArray, originalName: String?): File? = null
        override fun deletePhoto(name: String): Boolean = false
        override fun apps(): List<SetupApp> = installed
        override fun favoriteOrder(): List<String> = favorites
        override fun iconPng(packageName: String): ByteArray? =
            if (installed.any { it.packageName == packageName }) byteArrayOf(-119, 80, 78, 71) else null
        override fun setFavoriteOrder(order: List<String>) { favorites = order }
        override fun setHidden(packageName: String, hidden: Boolean) { hiddenCalls += packageName to hidden }
        override fun rename(packageName: String, label: String?) { renameCalls += packageName to label }
        var settings = LauncherSettings()
        override fun settings(): LauncherSettings = settings
        override fun updateSettings(settings: LauncherSettings) { this.settings = settings }
        override fun isLocked(): Boolean = locked
        override fun onChanged() { changes++ }
    }

    private lateinit var bridge: FakeBridge
    private lateinit var server: PhoneSetupServer
    private var port = 0
    private val token = "s3cr3ttoken1"

    @Before
    fun setUp() {
        bridge = FakeBridge()
        port = ServerSocket(0).use { it.localPort }
        server = PhoneSetupServer(port, token, "<html>setup</html>", bridge)
        server.startServer()
    }

    @After
    fun tearDown() {
        server.stop()
    }

    // ------------------------------------------------------------------ helpers

    private data class Reply(val code: Int, val body: String, val type: String?)

    private fun request(
        path: String,
        method: String = "GET",
        body: String? = null,
        contentType: String? = "application/json"
    ): Reply {
        val conn = URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        if (body != null) {
            conn.doOutput = true
            if (contentType != null) conn.setRequestProperty("Content-Type", contentType)
            conn.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = conn.responseCode
        val stream = if (code < 400) conn.inputStream else conn.errorStream
        val text = stream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
        return Reply(code, text, conn.contentType)
    }

    private fun t(path: String) = path + (if ('?' in path) "&" else "?") + "t=$token"

    private fun post(path: String, json: String) = request(t(path), "POST", json)

    /** Sends raw bytes, for requests HttpURLConnection won't make (lying Content-Length etc.). */
    private fun raw(text: String): String = Socket("127.0.0.1", port).use { s ->
        s.soTimeout = 5000
        s.getOutputStream().write(text.toByteArray())
        s.getOutputStream().flush()
        s.getInputStream().bufferedReader().readLine().orEmpty()
    }

    // ------------------------------------------------------------------ token

    @Test
    fun `page itself loads without a token but holds no data`() {
        val r = request("/")
        assertEquals(200, r.code)
        assertTrue(r.body.contains("setup"))
    }

    @Test
    fun `every api endpoint refuses a missing or wrong token`() {
        val endpoints = listOf(
            "/api/list", "/api/img?name=x", "/api/upload", "/api/delete?name=x",
            "/api/apps", "/api/icon?pkg=com.netflix.ninja",
            "/api/apps/favorites", "/api/apps/hidden", "/api/apps/rename", "/api/settings"
        )
        for (e in endpoints) {
            val sep = if ('?' in e) "&" else "?"
            assertEquals("no token: $e", 403, request(e).code)
            assertEquals("wrong token: $e", 403, request("$e${sep}t=wrong").code)
            assertEquals("token prefix: $e", 403, request("$e${sep}t=${token.dropLast(1)}").code)
            assertEquals("token case: $e", 403, request("$e${sep}t=${token.uppercase()}").code)
            assertEquals("token under other name: $e", 403, request("$e${sep}token=$token").code)
        }
        assertEquals(0, bridge.changes)
    }

    @Test
    fun `a wrong token can't change anything even with a valid body`() {
        val r = request("/api/apps/hidden?t=nope", "POST", """{"pkg":"com.netflix.ninja","hidden":true}""")
        assertEquals(403, r.code)
        assertTrue(bridge.hiddenCalls.isEmpty())
    }

    // ------------------------------------------------------------------ methods & content types

    @Test
    fun `changes must be POST`() {
        for (e in listOf("/api/apps/favorites", "/api/apps/hidden", "/api/apps/rename", "/api/delete?name=x", "/api/upload")) {
            assertEquals("GET $e", 405, request(t(e)).code)
        }
        assertEquals(0, bridge.changes)
    }

    @Test
    fun `cross-site form posts are refused (no JSON content type)`() {
        val form = request(t("/api/apps/hidden"), "POST", "pkg=com.netflix.ninja&hidden=true",
            contentType = "application/x-www-form-urlencoded")
        assertEquals(415, form.code)
        val plain = request(t("/api/apps/hidden"), "POST", """{"pkg":"com.netflix.ninja","hidden":true}""",
            contentType = "text/plain")
        assertEquals(415, plain.code)
        assertTrue(bridge.hiddenCalls.isEmpty())
    }

    @Test
    fun `oversized bodies are refused before reading`() {
        val big = "{\"order\":[" + (1..3000).joinToString(",") { "\"com.netflix.ninja\"" } + "]}"
        assertTrue(big.length > SetupValidation.MAX_JSON_BYTES)
        assertEquals(413, post("/api/apps/favorites", big).code)

        // A Content-Length that claims to be huge, with almost no body sent.
        val status = raw(
            "POST ${t("/api/apps/favorites")} HTTP/1.1\r\nHost: x\r\nContent-Type: application/json\r\n" +
                "Content-Length: 999999999\r\n\r\n{}"
        )
        assertTrue(status, status.contains("413"))
        assertEquals(listOf("com.plexapp.android", "com.gone.app"), bridge.favorites)
    }

    @Test
    fun `a refused post closes its connection so leftover body bytes can't become a request`() {
        val smuggled = "GET ${t("/api/apps")} HTTP/1.1\r\nHost: x\r\n\r\n"
        val reply = Socket("127.0.0.1", port).use { s ->
            s.soTimeout = 5000
            s.getOutputStream().write(
                ("POST /api/apps/hidden?t=wrong HTTP/1.1\r\nHost: x\r\nContent-Type: application/json\r\n" +
                    "Content-Length: ${smuggled.length}\r\n\r\n$smuggled").toByteArray()
            )
            s.getInputStream().readBytes().toString(Charsets.UTF_8)
        }
        assertTrue(reply, reply.startsWith("HTTP/1.1 403"))
        assertTrue(reply, reply.contains("Connection: close", ignoreCase = true))
        assertFalse("second response leaked: $reply", reply.contains("\"apps\""))
    }

    @Test
    fun `malformed or empty JSON is refused`() {
        assertEquals(400, post("/api/apps/hidden", "{not json").code)
        assertEquals(400, post("/api/apps/hidden", "[]").code)
        assertEquals(400, post("/api/apps/favorites", "{}").code)
        assertEquals(400, post("/api/apps/favorites", """{"order":"com.netflix.ninja"}""").code)
        assertEquals(0, bridge.changes)
    }

    // ------------------------------------------------------------------ apps

    @Test
    fun `apps list returns installed apps and only installed favourites`() {
        val r = request(t("/api/apps"))
        assertEquals(200, r.code)
        assertTrue(r.type!!.startsWith("application/json"))
        val json = JSONObject(r.body)
        assertEquals(3, json.getJSONArray("apps").length())
        assertEquals(1, json.getJSONArray("favorites").length())
        assertEquals("com.plexapp.android", json.getJSONArray("favorites").getString(0))
        assertFalse(json.getBoolean("locked"))
    }

    @Test
    fun `favourites drop junk, duplicates and uninstalled apps but keep hidden stored ones`() {
        val r = post(
            "/api/apps/favorites",
            """{"order":["com.netflix.ninja","../../etc","com.netflix.ninja","not installed.app",
               "com.evil.app","org.xbmc.kodi",42,null,"com.plexapp.android"]}"""
        )
        assertEquals(200, r.code)
        assertEquals(
            listOf("com.netflix.ninja", "org.xbmc.kodi", "com.plexapp.android", "com.gone.app"),
            bridge.favorites
        )
    }

    @Test
    fun `an empty favourites list clears installed favourites only`() {
        assertEquals(200, post("/api/apps/favorites", """{"order":[]}""").code)
        assertEquals(listOf("com.gone.app"), bridge.favorites)
    }

    @Test
    fun `hide needs an installed package and a real boolean`() {
        assertEquals(400, post("/api/apps/hidden", """{"pkg":"com.evil.app","hidden":true}""").code)
        assertEquals(400, post("/api/apps/hidden", """{"pkg":"com.netflix.ninja; rm -rf /","hidden":true}""").code)
        assertEquals(400, post("/api/apps/hidden", """{"pkg":"com.netflix.ninja","hidden":"yes"}""").code)
        assertEquals(400, post("/api/apps/hidden", """{"pkg":["com.netflix.ninja"],"hidden":true}""").code)
        assertTrue(bridge.hiddenCalls.isEmpty())

        assertEquals(200, post("/api/apps/hidden", """{"pkg":"com.netflix.ninja","hidden":true}""").code)
        assertEquals(listOf("com.netflix.ninja" to true), bridge.hiddenCalls)
    }

    @Test
    fun `rename passes labels through bounded, and null resets`() {
        val long = "A".repeat(5000)
        assertEquals(200, post("/api/apps/rename", """{"pkg":"com.netflix.ninja","label":"$long"}""").code)
        assertEquals(200, bridge.renameCalls.last().second!!.length)

        assertEquals(200, post("/api/apps/rename", """{"pkg":"com.netflix.ninja","label":null}""").code)
        assertEquals(null, bridge.renameCalls.last().second)

        assertEquals(400, post("/api/apps/rename", """{"pkg":"com.netflix.ninja","label":{"x":1}}""").code)
        assertEquals(400, post("/api/apps/rename", """{"pkg":"com.evil.app","label":"x"}""").code)
        assertEquals(2, bridge.renameCalls.size)
    }

    @Test
    fun `labels with markup come back as JSON data, not HTML`() {
        bridge.installed[0] = bridge.installed[0].copy(label = "<img src=x onerror=alert(1)>")
        val r = request(t("/api/apps"))
        assertTrue(r.type!!.startsWith("application/json"))
        assertEquals("<img src=x onerror=alert(1)>", JSONObject(r.body).getJSONArray("apps").getJSONObject(0).getString("label"))
    }

    @Test
    fun `icons only for well-formed installed packages`() {
        assertEquals(400, request(t("/api/icon?pkg=../../data/data")).code)
        assertEquals(400, request(t("/api/icon")).code)
        assertEquals(404, request(t("/api/icon?pkg=com.evil.app")).code)
        val ok = request(t("/api/icon?pkg=com.netflix.ninja"))
        assertEquals(200, ok.code)
        assertEquals("image/png", ok.type)
    }

    // ------------------------------------------------------------------ photos

    @Test
    fun `photo reads are limited to listed files`() {
        val f = File.createTempFile("onyx", ".jpg").apply { writeText("jpg"); deleteOnExit() }
        bridge.photos += f
        assertEquals(200, request(t("/api/img?name=${f.name}")).code)
        assertEquals(404, request(t("/api/img?name=../../../../etc/passwd")).code)
        assertEquals(404, request(t("/api/img?name=%2Fetc%2Fpasswd")).code)
    }

    // ------------------------------------------------------------------ kids mode

    @Test
    fun `nothing can be changed while Kids mode is on`() {
        bridge.locked = true
        val attempts = listOf(
            "/api/apps/favorites" to """{"order":[]}""",
            "/api/apps/hidden" to """{"pkg":"com.netflix.ninja","hidden":true}""",
            "/api/apps/rename" to """{"pkg":"com.netflix.ninja","label":"x"}""",
            "/api/settings" to """{"nightDim":false}"""
        )
        for ((path, body) in attempts) {
            val r = post(path, body)
            assertEquals(path, 403, r.code)
            assertTrue(r.body.contains("kids_mode"))
        }
        assertEquals(403, request(t("/api/delete?name=x"), "POST", "").code)
        assertEquals(listOf("com.plexapp.android", "com.gone.app"), bridge.favorites)
        assertTrue(bridge.hiddenCalls.isEmpty() && bridge.renameCalls.isEmpty())
        assertEquals(0, bridge.changes)
    }

    // ------------------------------------------------------------------ settings

    @Test
    fun `settings read back with the TV's options`() {
        val r = request(t("/api/settings"))
        assertEquals(200, r.code)
        val json = JSONObject(r.body)
        assertEquals("ROUNDED", json.getJSONObject("settings").getString("cardShape"))
        assertTrue(json.getJSONObject("options").has("wallpaperIntervalMinutes"))
        assertFalse("update channel stays TV-only", json.getJSONObject("settings").has("updateChannel"))
    }

    @Test
    fun `settings change only with valid values`() {
        assertEquals(200, post("/api/settings", """{"cardShape":"PILL","tileWidthDp":100,"nightDim":false}""").code)
        assertEquals(CardShape.PILL, bridge.settings.cardShape)
        assertEquals(100, bridge.settings.tileWidthDp)
        assertFalse(bridge.settings.nightDim)

        val before = bridge.settings
        for (bad in listOf(
            """{"updateChannel":"BETA"}""",
            """{"cardShape":"pill"}""",
            """{"cardShape":"HEXAGON"}""",
            """{"tileWidthDp":5000}""",
            """{"tileWidthDp":105}""",
            """{"tileWidthDp":"100"}""",
            """{"wallpaperIntervalMinutes":1}""",
            """{"screensaverTimeoutMinutes":-5}""",
            """{"nightDim":"false"}""",
            """{"nightDim":true,"bogus":1}""",
            """{}"""
        )) {
            assertEquals("should refuse $bad", 400, post("/api/settings", bad).code)
        }
        assertEquals(before, bridge.settings)
        assertEquals(UpdateChannel.STABLE, bridge.settings.updateChannel)
    }

    @Test
    fun `settings writes need POST and JSON`() {
        assertEquals(415, request(t("/api/settings"), "POST", "nightDim=false",
            contentType = "application/x-www-form-urlencoded").code)
        assertEquals(405, request(t("/api/settings"), "PUT", """{"nightDim":false}""").code)
        assertTrue(bridge.settings.nightDim)
    }

    @Test
    fun `concurrent changes don't lose each other`() {
        val threads = (1..20).map { i ->
            Thread {
                val shape = if (i % 2 == 0) "SQUARE" else "PILL"
                post("/api/settings", """{"cardShape":"$shape"}""")
                post("/api/apps/hidden", """{"pkg":"com.netflix.ninja","hidden":${i % 2 == 0}}""")
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertEquals(20, bridge.hiddenCalls.size)
        assertTrue(bridge.settings.cardShape == CardShape.SQUARE || bridge.settings.cardShape == CardShape.PILL)
    }

    @Test
    fun `unknown paths are 404`() {
        assertEquals(404, request(t("/api/../../etc/passwd")).code)
        assertEquals(404, request("/admin").code)
    }
}
