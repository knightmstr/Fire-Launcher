package com.knightmstr.onyxtv.data

import java.net.URI
import java.net.URLDecoder

/**
 * Works out what a pasted "My links" link is, without touching the network.
 *
 *  - Share links from Google Drive, Dropbox and OneDrive become direct image links.
 *  - A subreddit (`reddit.com/r/EarthPorn`, or just `r/EarthPorn`) becomes a feed that keeps
 *    pulling new pictures; it's stored as `https://www.reddit.com/r/Name`.
 *  - A Reddit post link is resolved later (online) to the image or gallery images in it.
 *  - Links that can never work without signing in (Drive folders, Google Photos albums) get a
 *    clear explanation instead of a confusing "not an image".
 *
 * Pure Kotlin so it can be unit-tested on the JVM.
 */
object WallpaperLinks {

    sealed class Kind {
        /** A link to try as an image (possibly rewritten from a share link). */
        data class Image(val url: String) : Kind()

        /** A subreddit feed. [name] is checked against [SUBREDDIT]. */
        data class Subreddit(val name: String) : Kind() {
            val url: String get() = subredditUrl(name)
        }

        /** A Reddit post; [id] is its base-36 post id. */
        data class RedditPost(val id: String) : Kind()

        data class Unsupported(val message: String) : Kind()
    }

    private val SUBREDDIT = Regex("^[A-Za-z0-9][A-Za-z0-9_]{1,20}$")
    private val POST_ID = Regex("^[a-z0-9]{3,12}$")
    private val DRIVE_ID = Regex("^[A-Za-z0-9_-]{10,200}$")
    private const val MAX_LENGTH = 2048

    fun subredditUrl(name: String) = "https://www.reddit.com/r/$name"

    /** The subreddit name if [url] is a stored subreddit feed, else null. */
    fun subredditOf(url: String): String? {
        val k = classify(url)
        return if (k is Kind.Subreddit && url == k.url) k.name else null
    }

    fun classify(raw: String): Kind {
        var text = raw.trim()
        if (text.isEmpty() || text.length > MAX_LENGTH || text.any { it.isWhitespace() || it.isISOControl() }) {
            return Kind.Unsupported(NOT_A_LINK)
        }
        // "r/EarthPorn" or "/r/EarthPorn" on its own
        Regex("^/?r/([A-Za-z0-9_]+)/?$").find(text)?.let { m ->
            return subreddit(m.groupValues[1])
        }
        if (!text.contains("://")) text = "https://$text"

        val uri = try {
            URI(text)
        } catch (e: Exception) {
            return Kind.Unsupported(NOT_A_LINK)
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "https" && scheme != "http") return Kind.Unsupported(NOT_A_LINK)
        if (uri.rawUserInfo != null) return Kind.Unsupported(NOT_A_LINK)
        val host = uri.host?.lowercase()?.removeSuffix(".") ?: return Kind.Unsupported(NOT_A_LINK)
        val path = uri.rawPath.orEmpty()
        val segments = path.split('/').filter { it.isNotEmpty() }

        // ---- Reddit
        if (host == "reddit.com" || host.endsWith(".reddit.com")) {
            // /r/Name/comments/<id>/... → a post
            if (segments.size >= 4 && segments[0] == "r" && segments[2] == "comments") return post(segments[3])
            // /comments/<id> → a post
            if (segments.size >= 2 && segments[0] == "comments") return post(segments[1])
            // /r/Name, /r/Name/hot, /r/Name/top … → a subreddit
            if (segments.size >= 2 && segments[0] == "r" &&
                (segments.size == 2 || segments[2] in setOf("hot", "new", "top", "rising"))
            ) return subreddit(segments[1])
            if (segments.size >= 3 && segments[0] == "r" && segments[2] == "s") {
                return Kind.Unsupported("Reddit's short share links can't be opened. Open the post and copy its full link.")
            }
            return Kind.Unsupported("Paste a subreddit (reddit.com/r/Name) or a post link.")
        }
        if (host == "redd.it" && segments.size == 1) return post(segments[0])

        // Everything below must be https from here on (http image links are refused later anyway).
        if (scheme != "https") return Kind.Image(text)

        // ---- Google Drive
        if (host == "drive.google.com" || host == "docs.google.com") {
            if (segments.firstOrNull() == "drive" && "folders" in segments) {
                return Kind.Unsupported(
                    "Drive folders need a sign-in to list. Share each photo instead (Anyone with the link), " +
                        "then paste its link."
                )
            }
            val id = when {
                segments.size >= 3 && segments[0] == "file" && segments[1] == "d" -> segments[2]
                else -> query(uri)["id"]
            }
            if (id != null && DRIVE_ID.matches(id)) {
                return Kind.Image("https://drive.google.com/uc?export=view&id=$id")
            }
            return Kind.Unsupported("Paste the share link of a single photo in Drive.")
        }

        // ---- Google Photos
        if (host == "photos.app.goo.gl" || host == "photos.google.com") {
            return Kind.Unsupported(
                "Google Photos albums can't be read without signing in. Open a photo, long-press it and " +
                    "copy the image address, or use Google Drive."
            )
        }

        // ---- Dropbox: force the raw file instead of the preview page
        if (host == "dropbox.com" || host == "www.dropbox.com") {
            val params = query(uri).filterKeys { it != "dl" && it != "raw" }
            val rebuilt = buildString {
                append("https://www.dropbox.com").append(path).append("?")
                params.forEach { (k, v) -> append(enc(k)).append('=').append(enc(v)).append('&') }
                append("raw=1")
            }
            return Kind.Image(rebuilt)
        }

        // ---- OneDrive (personal): the public "shares" API returns the file itself
        if (host == "1drv.ms" || host == "onedrive.live.com") {
            val encoded = base64Url(text.toByteArray(Charsets.UTF_8))
            return Kind.Image("https://api.onedrive.com/v1.0/shares/u!$encoded/root/content")
        }

        return Kind.Image(text)
    }

    private fun subreddit(name: String): Kind =
        if (SUBREDDIT.matches(name)) Kind.Subreddit(name) else Kind.Unsupported("That isn't a subreddit name.")

    private fun post(id: String): Kind {
        val clean = id.lowercase()
        return if (POST_ID.matches(clean)) Kind.RedditPost(clean) else Kind.Unsupported("That Reddit link isn't a post.")
    }

    private fun query(uri: URI): Map<String, String> =
        uri.rawQuery.orEmpty().split('&').filter { it.isNotEmpty() }.associate { pair ->
            val k = pair.substringBefore('=')
            val v = if ('=' in pair) pair.substringAfter('=') else ""
            dec(k) to dec(v)
        }

    private fun dec(s: String) = try {
        URLDecoder.decode(s, "UTF-8")
    } catch (e: Exception) {
        s
    }

    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")

    /** Base64url without padding, done by hand: java.util.Base64 needs API 26 (minSdk is 25). */
    private fun base64Url(bytes: ByteArray): String {
        val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val out = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xff
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xff else -1
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xff else -1
            out.append(table[b0 shr 2])
            out.append(table[((b0 and 3) shl 4) or (if (b1 < 0) 0 else b1 shr 4)])
            if (b1 >= 0) out.append(table[((b1 and 15) shl 2) or (if (b2 < 0) 0 else b2 shr 6)])
            if (b2 >= 0) out.append(table[b2 and 63])
            i += 3
        }
        return out.toString()
    }

    const val NOT_A_LINK = "That doesn't look like a web address."
}
