package com.knightmstr.onyxtv.upload

import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

/**
 * A tiny, token-gated HTTP server that lets a phone on the same Wi-Fi set up the TV from a
 * browser: upload wallpaper photos, and manage apps (favourites and their order, hide, rename).
 * No adb, no account. It only runs while the "Set up from phone" screen is open on the TV.
 *
 * Security model:
 *  - A random [token] is minted per session and shown on the TV as part of the URL. Every API call
 *    must carry it (`?t=<token>`); without it the server answers 403.
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

    override fun serve(session: IHTTPSession): Response {
        val response = route(session)
        // A refused request may leave its body unread on the socket, and the next request on a
        // kept-alive connection would then be parsed from those leftover bytes. So any request
        // that can carry a body ends its connection.
        if (session.method != Method.GET && session.method != Method.HEAD) response.closeConnection(true)
        return response
    }

    private fun route(session: IHTTPSession): Response = try {
        when (session.uri) {
            "/", "/index.html" -> html(page)

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

            else -> status(Response.Status.NOT_FOUND, "Not found")
        }
    } catch (e: Exception) {
        status(Response.Status.INTERNAL_ERROR, "Server error")
    }

    // ---------------------------------------------------------------- Guards

    private inline fun guarded(session: IHTTPSession, block: () -> Response): Response {
        val given = session.parameters["t"]?.firstOrNull()
        return if (given == token) block() else status(Response.Status.FORBIDDEN, "Forbidden")
    }

    /** A change: token, POST only, and refused while Kids mode is on. */
    private inline fun mutation(session: IHTTPSession, block: () -> Response): Response = guarded(session) {
        when {
            session.method != Method.POST -> status(Response.Status.METHOD_NOT_ALLOWED, "POST only")
            bridge.isLocked() -> error(Response.Status.FORBIDDEN, "kids_mode")
            else -> block()
        }
    }

    /** A JSON change: as [mutation], plus a JSON content type and a small, parsed body. */
    private inline fun jsonMutation(session: IHTTPSession, block: (JSONObject) -> Response): Response =
        mutation(session) {
            val type = session.headers["content-type"].orEmpty().substringBefore(';').trim()
            val length = session.headers["content-length"]?.toLongOrNull()
            when {
                !type.equals("application/json", ignoreCase = true) ->
                    error(Response.Status.UNSUPPORTED_MEDIA_TYPE, "json_only")
                length == null || length <= 0 -> error(Response.Status.BAD_REQUEST, "no_body")
                length > SetupValidation.MAX_JSON_BYTES -> error(Response.Status.PAYLOAD_TOO_LARGE, "too_large")
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
        /** Matches LocalPhotoSource's cap; checked here too so huge bodies are refused unread. */
        const val MAX_UPLOAD_BYTES = 20L * 1024 * 1024

        /** Labels are trimmed to 40 by the prefs store; anything far longer is cut before that. */
        private const val MAX_RAW_LABEL = 200
    }
}
