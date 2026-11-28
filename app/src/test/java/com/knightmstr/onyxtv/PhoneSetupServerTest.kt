package com.knightmstr.onyxtv

import com.knightmstr.onyxtv.data.AppSortOrder
import com.knightmstr.onyxtv.data.BackupData
import com.knightmstr.onyxtv.data.CardShape
import com.knightmstr.onyxtv.data.LauncherSettings
import com.knightmstr.onyxtv.data.UpdateChannel
import com.knightmstr.onyxtv.upload.LinkResult
import com.knightmstr.onyxtv.upload.PhoneSetupBridge
import com.knightmstr.onyxtv.upload.PhoneSetupServer
import com.knightmstr.onyxtv.upload.SetupApp
import com.knightmstr.onyxtv.upload.SetupToken
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
        var urls = mutableListOf("https://example.com/a.jpg")
        val fetched = mutableListOf<String>()
        override fun wallpaperUrls(): List<String> = urls
        override fun addWallpaperUrl(url: String): LinkResult {
            fetched += url
            if (url.contains("notimage")) return LinkResult.Failed("That link doesn't open as an image we can show.")
            urls.add(url); return LinkResult.Added("picture")
        }
        override fun removeWallpaperUrl(url: String) { urls.remove(url) }

        var restored: BackupData? = null
        override fun exportBackup() = BackupData(settings, favorites, setOf("org.xbmc.kodi"), emptyMap(),
            emptyList(), AppSortOrder.ALPHABETICAL, null, true, emptyList())
        override fun restoreBackup(backup: BackupData) { restored = backup }
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

    /** Status line and headers for a GET with a chosen Host header. */
    private fun rawGet(path: String, host: String): String = Socket("127.0.0.1", port).use { s ->
        s.soTimeout = 5000
        s.getOutputStream().write("GET $path HTTP/1.1\r\nHost: $host\r\nConnection: close\r\n\r\n".toByteArray())
        s.getInputStream().readBytes().toString(Charsets.UTF_8)
    }

    private fun headers(path: String): Map<String, String> {
        val conn = URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection
        conn.responseCode
        return conn.headerFields.filterKeys { it != null }.mapKeys { it.key.lowercase() }.mapValues { it.value.joinToString(",") }
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
            "/api/apps/favorites", "/api/apps/hidden", "/api/apps/rename", "/api/settings",
            "/api/backup", "/api/restore",
            "/api/wallpaper-urls", "/api/wallpaper-urls/add", "/api/wallpaper-urls/remove"
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
            "POST ${t("/api/apps/favorites")} HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Type: application/json\r\n" +
                "Content-Length: 999999999\r\n\r\n{}"
        )
        assertTrue(status, status.contains("413"))
        assertEquals(listOf("com.plexapp.android", "com.gone.app"), bridge.favorites)
    }

    @Test
    fun `a refused post closes its connection so leftover body bytes can't become a request`() {
        val smuggled = "GET ${t("/api/apps")} HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n"
        val reply = Socket("127.0.0.1", port).use { s ->
            s.soTimeout = 5000
            s.getOutputStream().write(
                ("POST /api/apps/hidden?t=wrong HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Type: application/json\r\n" +
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
            "/api/settings" to """{"nightDim":false}""",
            "/api/restore" to request(t("/api/backup")).body,
            "/api/wallpaper-urls/add" to """{"url":"https://example.com/b.jpg"}""",
            "/api/wallpaper-urls/remove" to """{"url":"https://example.com/a.jpg"}"""
        )
        for ((path, body) in attempts) {
            val r = post(path, body)
            assertEquals(path, 403, r.code)
            assertTrue(r.body.contains("kids_mode"))
        }
        assertEquals(403, request(t("/api/delete?name=x"), "POST", "").code)
        assertEquals(listOf("com.plexapp.android", "com.gone.app"), bridge.favorites)
        assertTrue(bridge.hiddenCalls.isEmpty() && bridge.renameCalls.isEmpty())
        assertEquals(null, bridge.restored)
        assertEquals(listOf("https://example.com/a.jpg"), bridge.urls)
        assertTrue("no fetch while locked", bridge.fetched.isEmpty())
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

    // ------------------------------------------------------------------ hardening (phase 3)

    @Test
    fun `host names are refused, so DNS rebinding can't reach the API`() {
        for (host in listOf("evil.example", "evil.example:$port", "192.168.1.5.evil.example", "localhost.evil", "", "[::1]:$port", "1.2.3.4:", "1.2.3.4:99999x")) {
            val r = rawGet(t("/api/apps"), host)
            assertTrue("host '$host': ${r.lineSequence().first()}", r.startsWith("HTTP/1.1 403"))
            assertFalse(r.contains("\"apps\""))
        }
        for (host in listOf("192.168.1.5:$port", "10.0.0.2", "127.0.0.1:$port", "localhost:$port")) {
            assertTrue("host '$host'", rawGet(t("/api/apps"), host).startsWith("HTTP/1.1 200"))
        }
    }

    @Test
    fun `every response carries the safety headers`() {
        for (path in listOf("/", t("/api/apps"), "/api/apps", "/nope")) {
            val h = headers(path)
            assertEquals(path, "nosniff", h["x-content-type-options"])
            assertEquals(path, "no-referrer", h["referrer-policy"])
            assertEquals(path, "DENY", h["x-frame-options"])
            assertEquals(path, "no-store", h["cache-control"])
        }
        assertEquals("private, max-age=3600", headers(t("/api/icon?pkg=com.netflix.ninja"))["cache-control"])
    }

    @Test
    fun `page CSP allows only its own inline script and style`() {
        val page = "<html><style>a{}</style><script>go()</script></html>"
        val csp = PhoneSetupServer.hashes(page, "script")
        assertTrue(csp.startsWith("'sha256-") && !csp.contains("unsafe"))
        assertEquals("'none'", PhoneSetupServer.hashes("<html></html>", "script"))
        assertEquals(2, PhoneSetupServer.hashes("<script>a</script><script>b</script>", "script").split(" ").size)

        val h = headers("/")["content-security-policy"].orEmpty()
        assertTrue(h, h.contains("default-src 'none'"))
        assertTrue(h, h.contains("frame-ancestors 'none'"))
        assertFalse(h, h.contains("unsafe-inline") || h.contains("unsafe-eval") || h.contains("*"))
    }

    @Test
    fun `only correct-token requests count as activity`() {
        val start = server.lastActivityMs
        Thread.sleep(20)
        request("/api/apps?t=wrong")
        request("/")
        assertEquals(start, server.lastActivityMs)
        request(t("/api/apps"))
        assertTrue(server.lastActivityMs > start)
    }

    @Test
    fun `tokens are long, random and url-safe`() {
        val tokens = (1..500).map { SetupToken.generate() }
        assertEquals(500, tokens.toSet().size)
        assertTrue(tokens.all { it.length == SetupToken.LENGTH && it.all { c -> c in 'a'..'z' || c in '0'..'9' } })
        assertTrue(SetupToken.LENGTH >= 16)
    }

    // ------------------------------------------------------------------ backup & restore (phase 4)

    @Test
    fun `backup downloads as a file and restores`() {
        val conn = URL("http://127.0.0.1:$port${t("/api/backup")}").openConnection() as HttpURLConnection
        assertEquals(200, conn.responseCode)
        assertTrue(conn.getHeaderField("Content-Disposition").startsWith("attachment"))
        val file = conn.inputStream.readBytes().toString(Charsets.UTF_8)
        assertEquals("onyx-tv-backup", JSONObject(file).getString("format"))

        assertEquals(200, post("/api/restore", file).code)
        assertEquals(listOf("com.plexapp.android", "com.gone.app"), bridge.restored!!.favorites)
        assertEquals(1, bridge.changes)
    }

    @Test
    fun `restore refuses bad files and changes nothing`() {
        assertEquals(400, post("/api/restore", """{"format":"onyx-tv-backup","version":1}""").code)
        assertEquals(400, post("/api/restore", """{"hello":"world"}""").code)
        val tampered = JSONObject(request(t("/api/backup")).body).put("favorites", org.json.JSONArray(listOf("../../x")))
        assertEquals(400, post("/api/restore", tampered.toString()).code)
        assertEquals(405, request(t("/api/restore")).code)
        assertEquals(null, bridge.restored)
    }

    @Test
    fun `restore accepts bigger bodies than other changes, up to its own cap`() {
        val pad = "x".repeat(40_000) // over the 16 KB cap for other endpoints
        val file = JSONObject(request(t("/api/backup")).body).put("note", pad).toString()
        assertEquals(200, post("/api/restore", file).code)
        assertEquals(413, post("/api/apps/hidden", """{"pkg":"com.netflix.ninja","hidden":true,"n":"$pad"}""").code)
        val tooBig = JSONObject(request(t("/api/backup")).body).put("note", "x".repeat(300_000)).toString()
        assertEquals(413, post("/api/restore", tooBig).code)
    }

    // ------------------------------------------------------------------ wallpaper links

    @Test
    fun `links list, add and remove`() {
        val list = JSONObject(request(t("/api/wallpaper-urls")).body)
        assertEquals(1, list.getJSONArray("urls").length())
        assertEquals(200, post("/api/wallpaper-urls/add", """{"url":"https://example.com/b.jpg"}""").code)
        assertEquals(listOf("https://example.com/a.jpg", "https://example.com/b.jpg"), bridge.urls)
        assertEquals(200, post("/api/wallpaper-urls/remove", """{"url":"https://example.com/a.jpg"}""").code)
        assertEquals(listOf("https://example.com/b.jpg"), bridge.urls)
        assertEquals(404, post("/api/wallpaper-urls/remove", """{"url":"https://example.com/zzz.jpg"}""").code)
    }

    @Test
    fun `only https links are even fetched`() {
        for (bad in listOf("http://example.com/a.jpg", "file:///etc/passwd", "javascript:alert(1)",
            "https://user:pw@example.com/a.jpg", "https:///nohost", "https://example.com/a b.jpg",
            "ftp://example.com/a.jpg", "https://" + "a".repeat(3000) + ".com")) {
            val r = post("/api/wallpaper-urls/add", JSONObject().put("url", bad).toString())
            assertEquals(bad.take(40), 400, r.code)
        }
        assertEquals(400, post("/api/wallpaper-urls/add", """{"url":42}""").code)
        assertTrue(bridge.fetched.isEmpty())
    }

    @Test
    fun `a link that isn't an image says why and isn't saved`() {
        val r = post("/api/wallpaper-urls/add", """{"url":"https://example.com/notimage.html"}""")
        assertEquals(400, r.code)
        assertTrue(JSONObject(r.body).getString("message").contains("image"))
        assertEquals(1, bridge.urls.size)
    }

    @Test
    fun `subreddits, posts and cloud links reach the TV's resolver`() {
        for (s in listOf("r/EarthPorn", "https://www.reddit.com/r/EarthPorn/comments/1abcde/x/",
            "https://drive.google.com/file/d/1AbCdEfGhIjKlMnOp/view?usp=sharing", "dropbox.com/s/abc/a.jpg?dl=0")) {
            assertEquals(s, 200, post("/api/wallpaper-urls/add", JSONObject().put("url", s).toString()).code)
        }
        assertEquals(4, bridge.fetched.size)
    }

    @Test
    fun `unsupported links explain themselves without any fetch`() {
        for (s in listOf("https://drive.google.com/drive/folders/1AbCdEfGhIjKlMnOp", "https://photos.app.goo.gl/x",
            "https://www.reddit.com/r/EarthPorn/s/AbC")) {
            val r = post("/api/wallpaper-urls/add", JSONObject().put("url", s).toString())
            assertEquals(400, r.code)
            assertTrue(JSONObject(r.body).getString("message").isNotEmpty())
        }
        assertTrue(bridge.fetched.isEmpty())
    }

    @Test
    fun `a stored subreddit isn't added twice`() {
        bridge.urls.add("https://www.reddit.com/r/EarthPorn")
        assertEquals(200, post("/api/wallpaper-urls/add", """{"url":"reddit.com/r/EarthPorn/"}""").code)
        assertTrue(bridge.fetched.isEmpty())
    }

    @Test
    fun `duplicates aren't fetched again and the list is capped`() {
        assertEquals(200, post("/api/wallpaper-urls/add", """{"url":"https://example.com/a.jpg"}""").code)
        assertTrue(bridge.fetched.isEmpty())
        while (bridge.urls.size < PhoneSetupServer.MAX_WALLPAPER_URLS) bridge.urls.add("https://example.com/${bridge.urls.size}.jpg")
        assertEquals(400, post("/api/wallpaper-urls/add", """{"url":"https://example.com/new.jpg"}""").code)
        assertTrue(bridge.fetched.isEmpty())
    }

    @Test
    fun `unknown paths are 404`() {
        assertEquals(404, request(t("/api/../../etc/passwd")).code)
        assertEquals(404, request("/admin").code)
    }
}
