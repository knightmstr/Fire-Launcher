package com.knightmstr.onyxtv.upload

import com.knightmstr.onyxtv.data.WallpaperLinks
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * A tiny, token-gated HTTP server that lets a phone on the same Wi-Fi set up the TV from a
 * browser: upload wallpaper photos, manage apps (favourites and their order, hide, rename) and
 * change launcher settings, and back up or restore the whole setup.
 * No adb, no account. It only runs while the "Set up from phone" screen is open on the TV.
 *
 * Security model:
 *  - A random [token] ([SetupToken], ~82 bits) is minted per session and shown on the TV as part of
 *    the URL. Every API call must carry it (`?t=<token>`), compared in constant time; without it
 *    the server answers 403.
 *  - Requests must name the TV by IP address (or localhost) in the Host header, which blocks DNS
 *    rebinding: a web page on some other site can't make the phone's browser treat the TV as
 *    its own origin.
 *  - Every response carries no-store / nosniff / no-referrer / no-framing headers, and the page has
 *    a strict Content-Security-Policy that allows only its own inline script and style (by hash).
 *  - [lastActivityMs] lets the TV stop the server after a while without use.
 *  - Every change must be a POST. JSON endpoints also require `Content-Type: application/json`,
 *    which a web page on another site can't send here without a CORS preflight this server never
 *    approves, and a body no bigger than [SetupValidation.MAX_JSON_BYTES].
 *  - While Kids mode is on ([PhoneSetupBridge.isLocked]) every change is refused.
 *  - Package names and lists from the phone are validated ([SetupValidation]); labels are
 *    sanitised by the prefs store. Photo names are generated, never trusted, so no path traversal.
 *  - Traffic is plain HTTP on the LAN (no certificate on a home TV).
 */
class PhoneSetupServer(
    port: Int,
    private val token: String,
    private val page: String,
    private val bridge: PhoneSetupBridge
) : NanoHTTPD(port) {

    fun startServer() = start(SOCKET_READ_TIMEOUT, true)

    /** When the phone last made a request with the right token (or when the server started). */
    @Volatile
    var lastActivityMs: Long = System.currentTimeMillis()
        private set

    private val tokenBytes = token.toByteArray(Charsets.UTF_8)

    /** CSP for the page: nothing but its own inline <script> and <style>, pinned by hash. */
    private val pageCsp: String = buildString {
        append("default-src 'none'; img-src 'self'; connect-src 'self'; ")
        append("script-src ${hashes(page, "script")}; style-src ${hashes(page, "style")}; ")
        append("base-uri 'none'; form-action 'none'; frame-ancestors 'none'")
    }

    override fun serve(session: IHTTPSession): Response {
        val response = if (!SetupValidation.isLocalHost(session.headers["host"])) {
            status(Response.Status.FORBIDDEN, "Open the address shown on the TV")
        } else {
            route(session)
        }
        response.addHeader("X-Content-Type-Options", "nosniff")
        response.addHeader("Referrer-Policy", "no-referrer")
        response.addHeader("X-Frame-Options", "DENY")
        if (response.getHeader("Cache-Control") == null) response.addHeader("Cache-Control", "no-store")
        // A refused request may leave its body unread on the socket, and the next request on a
        // kept-alive connection would then be parsed from those leftover bytes. So any request
        // that can carry a body ends its connection.
        if (session.method != Method.GET && session.method != Method.HEAD) response.closeConnection(true)
        return response
    }

    private fun route(session: IHTTPSession): Response = try {
        when (session.uri) {
            "/", "/index.html" -> html(page).apply { addHeader("Content-Security-Policy", pageCsp) }

            // Photos
            "/api/list" -> guarded(session) { listPhotos() }
            "/api/img" -> guarded(session) { photo(session) }
            "/api/upload" -> mutation(session) { upload(session) }
            "/api/delete" -> mutation(session) { deletePhoto(session) }

            // Apps
            "/api/apps" -> guarded(session) { listApps() }
            "/api/icon" -> guarded(session) { icon(session) }
            "/api/apps/favorites" -> jsonMutation(session) { setFavorites(it) }
            "/api/apps/hidden" -> jsonMutation(session) { setHidden(it) }
            "/api/apps/rename" -> jsonMutation(session) { rename(it) }

            // Settings
            "/api/settings" -> when (session.method) {
                Method.GET -> guarded(session) { getSettings() }
                else -> jsonMutation(session) { updateSettings(it) }
            }

            // Wallpaper links
            "/api/wallpaper-urls" -> guarded(session) { listWallpaperUrls() }
            "/api/wallpaper-urls/add" -> jsonMutation(session) { addWallpaperUrl(it) }
            "/api/wallpaper-urls/remove" -> jsonMutation(session) { removeWallpaperUrl(it) }

            // Install an app (sideload)
            "/api/apk/upload" -> guarded(session) { uploadApk(session) }
            "/api/apk/status" -> guarded(session) { json(apkStatusJson(bridge.apkStatus())) }
            "/api/apk/favorite" -> jsonMutation(session) { favoriteInstalled(it) }

            // Backup & restore
            "/api/backup" -> guarded(session) { backup() }
            "/api/restore" -> jsonMutation(session, BackupJson.MAX_BYTES) { restore(it) }

            else -> status(Response.Status.NOT_FOUND, "Not found")
        }
    } catch (e: Exception) {
        status(Response.Status.INTERNAL_ERROR, "Server error")
    }

    // ---------------------------------------------------------------- Guards

    private inline fun guarded(session: IHTTPSession, block: () -> Response): Response {
        val given = session.parameters["t"]?.firstOrNull() ?: return status(Response.Status.FORBIDDEN, "Forbidden")
        if (!MessageDigest.isEqual(given.toByteArray(Charsets.UTF_8), tokenBytes)) {
            return status(Response.Status.FORBIDDEN, "Forbidden")
        }
        lastActivityMs = System.currentTimeMillis()
        return block()
    }

    /** Changes run one at a time, so two quick requests can't read-modify-write over each other. */
    private val writeLock = Any()

    /** A change: token, POST only, and refused while Kids mode is on. */
    private inline fun mutation(session: IHTTPSession, block: () -> Response): Response = guarded(session) {
        when {
            session.method != Method.POST -> status(Response.Status.METHOD_NOT_ALLOWED, "POST only")
            else -> synchronized(writeLock) {
                if (bridge.isLocked()) error(Response.Status.FORBIDDEN, "kids_mode") else block()
            }
        }
    }

    /** A JSON change: as [mutation], plus a JSON content type and a small, parsed body. */
    private inline fun jsonMutation(
        session: IHTTPSession,
        maxBytes: Int = SetupValidation.MAX_JSON_BYTES,
        block: (JSONObject) -> Response
    ): Response =
        mutation(session) {
            val type = session.headers["content-type"].orEmpty().substringBefore(';').trim()
            val length = session.headers["content-length"]?.toLongOrNull()
            when {
                !type.equals("application/json", ignoreCase = true) ->
                    error(Response.Status.UNSUPPORTED_MEDIA_TYPE, "json_only")
                length == null || length <= 0 -> error(Response.Status.BAD_REQUEST, "no_body")
                length > maxBytes -> error(Response.Status.PAYLOAD_TOO_LARGE, "too_large")
                else -> {
                    val files = HashMap<String, String>()
                    session.parseBody(files)
                    val body = try {
                        JSONObject(files["postData"].orEmpty())
                    } catch (e: JSONException) {
                        null
                    }
                    if (body == null) error(Response.Status.BAD_REQUEST, "bad_json") else block(body)
                }
            }
        }

    // ---------------------------------------------------------------- Photos

    private fun listPhotos(): Response {
        val array = JSONArray()
        bridge.listPhotos().forEach { array.put(JSONObject().put("name", it.name)) }
        return json(JSONObject().put("photos", array))
    }

    private fun photo(session: IHTTPSession): Response {
        val name = session.parameters["name"]?.firstOrNull() ?: return status(Response.Status.BAD_REQUEST, "No name")
        val file = bridge.listPhotos().firstOrNull { it.name == name }
            ?: return status(Response.Status.NOT_FOUND, "Not found")
        return newChunkedResponse(Response.Status.OK, mimeFor(file), FileInputStream(file))
    }

    private fun upload(session: IHTTPSession): Response {
        val length = session.headers["content-length"]?.toLongOrNull() ?: 0L
        if (length > MAX_UPLOAD_BYTES) return json(JSONObject().put("ok", false).put("error", "too_large"))
        val files = HashMap<String, String>()
        session.parseBody(files)
        val tempPath = files["photo"] ?: return json(JSONObject().put("ok", false).put("error", "no_file"))
        val originalName = session.parameters["photo"]?.firstOrNull()
        val saved = bridge.savePhoto(File(tempPath).readBytes(), originalName)
        return if (saved != null) {
            bridge.onChanged()
            json(JSONObject().put("ok", true).put("name", saved.name))
        } else {
            json(JSONObject().put("ok", false).put("error", "rejected"))
        }
    }

    private fun deletePhoto(session: IHTTPSession): Response {
        val name = session.parameters["name"]?.firstOrNull() ?: return status(Response.Status.BAD_REQUEST, "No name")
        val ok = bridge.deletePhoto(name)
        if (ok) bridge.onChanged()
        return json(JSONObject().put("ok", ok))
    }

    // ---------------------------------------------------------------- Apps

    private fun listApps(): Response {
        val apps = JSONArray()
        bridge.apps().sortedBy { it.label.lowercase() }.forEach { app ->
            apps.put(
                JSONObject()
                    .put("pkg", app.packageName)
                    .put("label", app.label)
                    .put("original", app.originalLabel)
                    .put("hidden", app.hidden)
            )
        }
        val installed = bridge.apps().map { it.packageName }.toSet()
        val favorites = JSONArray()
        bridge.favoriteOrder().filter { it in installed }.forEach { favorites.put(it) }
        return json(JSONObject().put("apps", apps).put("favorites", favorites).put("locked", bridge.isLocked()))
    }

    private fun icon(session: IHTTPSession): Response {
        val pkg = session.parameters["pkg"]?.firstOrNull()
        if (!SetupValidation.isPackageName(pkg)) return status(Response.Status.BAD_REQUEST, "Bad package")
        val png = bridge.iconPng(pkg!!) ?: return status(Response.Status.NOT_FOUND, "Not found")
        return newFixedLengthResponse(Response.Status.OK, "image/png", ByteArrayInputStream(png), png.size.toLong())
            .apply { addHeader("Cache-Control", "private, max-age=3600") }
    }

    private fun setFavorites(body: JSONObject): Response {
        val array = body.optJSONArray("order") ?: return error(Response.Status.BAD_REQUEST, "no_order")
        if (array.length() > SetupValidation.MAX_FAVORITES * 2) return error(Response.Status.BAD_REQUEST, "too_many")
        val requested = (0 until array.length()).mapNotNull { array.opt(it) as? String }
        val installed = bridge.apps().map { it.packageName }.toSet()
        val merged = SetupValidation.mergeFavorites(requested, installed, bridge.favoriteOrder())
        bridge.setFavoriteOrder(merged)
        bridge.onChanged()
        return ok()
    }

    private fun setHidden(body: JSONObject): Response {
        val pkg = installedPackage(body) ?: return error(Response.Status.BAD_REQUEST, "bad_package")
        val hidden = body.opt("hidden") as? Boolean ?: return error(Response.Status.BAD_REQUEST, "bad_value")
        bridge.setHidden(pkg, hidden)
        bridge.onChanged()
        return ok()
    }

    private fun rename(body: JSONObject): Response {
        val pkg = installedPackage(body) ?: return error(Response.Status.BAD_REQUEST, "bad_package")
        val raw = body.opt("label")
        val label = when (raw) {
            null, JSONObject.NULL -> null
            is String -> raw.take(MAX_RAW_LABEL)
            else -> return error(Response.Status.BAD_REQUEST, "bad_value")
        }
        bridge.rename(pkg, label)
        bridge.onChanged()
        return ok()
    }

    /** The "pkg" field, only if it's well-formed and actually installed. */
    private fun installedPackage(body: JSONObject): String? {
        val pkg = body.opt("pkg") as? String
        if (!SetupValidation.isPackageName(pkg)) return null
        return pkg.takeIf { p -> bridge.apps().any { it.packageName == p } }
    }

    // ---------------------------------------------------------------- Settings

    private fun getSettings(): Response = json(
        JSONObject()
            .put("settings", SettingsJson.toJson(bridge.settings()))
            .put("options", SettingsJson.options())
            .put("locked", bridge.isLocked())
    )

    private fun updateSettings(body: JSONObject): Response =
        when (val result = SettingsJson.apply(bridge.settings(), body)) {
            is SettingsJson.Result.Invalid -> error(Response.Status.BAD_REQUEST, "bad_setting")
            is SettingsJson.Result.Ok -> {
                bridge.updateSettings(result.settings)
                bridge.onChanged()
                json(JSONObject().put("ok", true).put("settings", SettingsJson.toJson(result.settings)))
            }
        }

    // ---------------------------------------------------------------- Wallpaper links

    private fun listWallpaperUrls(): Response = json(
        JSONObject()
            .put("urls", JSONArray(bridge.wallpaperUrls()))
            .put("max", MAX_WALLPAPER_URLS)
            .put("locked", bridge.isLocked())
    )

    private fun addWallpaperUrl(body: JSONObject): Response {
        val raw = (body.opt("url") as? String)?.trim()
            ?: return error(Response.Status.BAD_REQUEST, "bad_url")
        // Work out what it is offline first, so junk never makes the TV fetch anything.
        val stored = when (val kind = WallpaperLinks.classify(raw)) {
            is WallpaperLinks.Kind.Unsupported -> return linkProblem(kind.message)
            is WallpaperLinks.Kind.Image ->
                if (SetupValidation.isHttpsUrl(kind.url)) kind.url else return error(Response.Status.BAD_REQUEST, "bad_url")
            is WallpaperLinks.Kind.Subreddit -> kind.url
            is WallpaperLinks.Kind.DriveFolder -> kind.url
            is WallpaperLinks.Kind.RedditPost -> null // its pictures are only known once fetched
        }
        val current = bridge.wallpaperUrls()
        if (stored != null && stored in current) return json(JSONObject().put("ok", true).put("added", "already"))
        if (current.size >= MAX_WALLPAPER_URLS) return error(Response.Status.BAD_REQUEST, "full")
        return when (val result = bridge.addWallpaperUrl(raw)) {
            is LinkResult.Failed -> linkProblem(result.message)
            is LinkResult.Added -> {
                bridge.onChanged()
                json(JSONObject().put("ok", true).put("added", result.what.take(80)))
            }
        }
    }

    private fun linkProblem(message: String): Response = newFixedLengthResponse(
        Response.Status.BAD_REQUEST, "application/json",
        JSONObject().put("ok", false).put("error", "not_image").put("message", message.take(200)).toString()
    )

    private fun removeWallpaperUrl(body: JSONObject): Response {
        val url = body.opt("url") as? String ?: return error(Response.Status.BAD_REQUEST, "bad_url")
        if (url !in bridge.wallpaperUrls()) return error(Response.Status.NOT_FOUND, "not_found")
        bridge.removeWallpaperUrl(url)
        bridge.onChanged()
        return ok()
    }

    // ---------------------------------------------------------------- Install an app (sideload)

    /** One upload at a time, kept apart from [writeLock] so a long upload doesn't hold up other changes. */
    private val apkBusy = java.util.concurrent.atomic.AtomicBoolean(false)

    /**
     * The phone sends the APK as the raw request body (Content-Type
     * application/vnd.android.package-archive), streamed straight to a file: no multipart parsing,
     * no large buffers in memory. That content type also can't be sent cross-site without a CORS
     * preflight this server never approves.
     */
    private fun uploadApk(session: IHTTPSession): Response {
        if (session.method != Method.POST) return status(Response.Status.METHOD_NOT_ALLOWED, "POST only")
        if (bridge.isLocked()) return error(Response.Status.FORBIDDEN, "kids_mode")
        val type = session.headers["content-type"].orEmpty().substringBefore(';').trim()
        if (!type.equals(APK_TYPE, ignoreCase = true)) return error(Response.Status.UNSUPPORTED_MEDIA_TYPE, "apk_only")
        val length = session.headers["content-length"]?.toLongOrNull()
            ?: return error(Response.Status.BAD_REQUEST, "no_length")
        if (length <= 0) return error(Response.Status.BAD_REQUEST, "no_body")
        if (length > MAX_APK_BYTES) return error(Response.Status.PAYLOAD_TOO_LARGE, "too_large")
        // Room for the upload plus the installer's own copy of it
        if (bridge.freeSpaceBytes() < length * 2 + SPACE_MARGIN_BYTES) return error(Response.Status.BAD_REQUEST, "no_space")
        if (bridge.apkStatus().state == "waiting") return error(Response.Status.CONFLICT, "busy")
        if (!apkBusy.compareAndSet(false, true)) return error(Response.Status.CONFLICT, "busy")

        val file = File(bridge.apkWorkDir(), "upload-${System.nanoTime()}.apk")
        try {
            // Stream exactly Content-Length bytes to disk
            var remaining = length
            file.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                val input = session.inputStream
                while (remaining > 0) {
                    val n = input.read(buf, 0, minOf(buf.size.toLong(), remaining).toInt())
                    if (n < 0) break
                    out.write(buf, 0, n)
                    remaining -= n
                    lastActivityMs = System.currentTimeMillis() // a long upload counts as use
                }
            }
            if (remaining != 0L) return error(Response.Status.BAD_REQUEST, "incomplete")
            if (!looksLikeApk(file)) return error(Response.Status.BAD_REQUEST, "not_apk")
            return when (val start = bridge.startApkInstall(file)) {
                is ApkStart.Failed -> newFixedLengthResponse(
                    Response.Status.BAD_REQUEST, "application/json",
                    JSONObject().put("ok", false).put("error", "install").put("message", start.message.take(300)).toString()
                )
                is ApkStart.Started -> json(apkStatusJson(start.status).put("ok", true))
            }
        } finally {
            file.delete() // normally already handed to the installer and deleted by the bridge
            apkBusy.set(false)
        }
    }

    private fun apkStatusJson(s: ApkStatus): JSONObject = JSONObject()
        .put("state", s.state)
        .put("label", s.label ?: JSONObject.NULL)
        .put("pkg", s.packageName ?: JSONObject.NULL)
        .put("version", s.versionName ?: JSONObject.NULL)
        .put("isUpdate", s.isUpdate)
        .put("message", s.message ?: JSONObject.NULL)
        .put("serial", s.serial)
        .put("favorite", s.packageName != null && s.packageName in bridge.favoriteOrder())

    /** Adds a just-installed app to the end of the dock. */
    private fun favoriteInstalled(body: JSONObject): Response {
        val pkg = installedPackage(body) ?: return error(Response.Status.BAD_REQUEST, "bad_package")
        val current = bridge.favoriteOrder()
        if (pkg !in current) {
            bridge.setFavoriteOrder(current + pkg)
            bridge.onChanged()
        }
        return ok()
    }

    // ---------------------------------------------------------------- Backup & restore

    private fun backup(): Response =
        newFixedLengthResponse(Response.Status.OK, "application/json", BackupJson.toJson(bridge.exportBackup()).toString(2))
            .apply { addHeader("Content-Disposition", "attachment; filename=\"onyx-tv-backup.json\"") }

    private fun restore(body: JSONObject): Response =
        when (val result = BackupJson.parse(body, bridge.settings())) {
            is BackupJson.Result.Invalid -> error(Response.Status.BAD_REQUEST, "bad_backup_" + result.reason)
            is BackupJson.Result.Ok -> {
                bridge.restoreBackup(result.data)
                bridge.onChanged()
                ok()
            }
        }

    // ---------------------------------------------------------------- Responses

    private fun mimeFor(file: File): String = when (file.extension.lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }

    private fun ok(): Response = json(JSONObject().put("ok", true))

    private fun error(code: Response.Status, reason: String): Response =
        newFixedLengthResponse(code, "application/json", JSONObject().put("ok", false).put("error", reason).toString())

    private fun json(body: JSONObject): Response =
        newFixedLengthResponse(Response.Status.OK, "application/json", body.toString())

    private fun html(body: String): Response =
        newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", body)

    private fun status(code: Response.Status, message: String): Response =
        newFixedLengthResponse(code, "text/plain", message)

    companion object {
        /** `'sha256-…'` sources for every inline <[tag]> block in [html], for the CSP. */
        @OptIn(ExperimentalEncodingApi::class) // Kotlin's Base64: java.util.Base64 needs API 26
        fun hashes(html: String, tag: String): String {
            val blocks = Regex("<$tag(?:\\s[^>]*)?>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL)
                .findAll(html).map { it.groupValues[1] }.toList()
            if (blocks.isEmpty()) return "'none'"
            val digest = { text: String ->
                Base64.Default.encode(MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)))
            }
            return blocks.joinToString(" ") { "'sha256-${digest(it)}'" }
        }

        /** Matches LocalPhotoSource's cap; checked here too so huge bodies are refused unread. */
        const val MAX_UPLOAD_BYTES = 20L * 1024 * 1024

        const val APK_TYPE = "application/vnd.android.package-archive"

        /** Largest app the phone can upload. */
        const val MAX_APK_BYTES = 300L * 1024 * 1024

        /** Kept free on the TV after an upload and its install copy. */
        const val SPACE_MARGIN_BYTES = 100L * 1024 * 1024

        /** An APK is a zip with an AndroidManifest.xml at its root. */
        fun looksLikeApk(file: File): Boolean = try {
            val magic = file.inputStream().use { input -> ByteArray(4).also { input.read(it) } }
            magic[0] == 'P'.code.toByte() && magic[1] == 'K'.code.toByte() &&
                magic[2] == 3.toByte() && magic[3] == 4.toByte() &&
                java.util.zip.ZipFile(file).use { it.getEntry("AndroidManifest.xml") != null }
        } catch (e: Exception) {
            false
        }

        /** Same cap as the TV's own "My links" list. */
        const val MAX_WALLPAPER_URLS = 50

        /** Labels are trimmed to 40 by the prefs store; anything far longer is cut before that. */
        private const val MAX_RAW_LABEL = 200
    }
}
