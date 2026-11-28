package com.knightmstr.onyxtv.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * Reads pictures from Reddit's public JSON: a subreddit's hot posts, or one post (including
 * galleries). NSFW posts and videos are skipped, and only images on Reddit's and Imgur's own image
 * hosts are used, so a post can't point the TV at an arbitrary server.
 *
 * Parsing is pure (testable on the JVM); [fetchJson] does the network part.
 */
object RedditImages {

    private const val USER_AGENT = "android:com.knightmstr.onyxtv:v1.0 (Fire OS Leanback)"
    private const val MAX_JSON_BYTES = 4L * 1024 * 1024

    /** Hosts Reddit images are served from. Anything else in a post is ignored. */
    fun isAllowedImageHost(url: String): Boolean {
        if (!url.startsWith("https://", ignoreCase = true)) return false
        val host = try {
            URI(url).host?.lowercase()
        } catch (e: Exception) {
            null
        } ?: return false
        return host == "i.redd.it" || host.endsWith(".redd.it") ||
            host == "i.imgur.com" || host.endsWith(".imgur.com") ||
            host == "images.unsplash.com"
    }

    // ---------------------------------------------------------------- parsing

    /** Pictures in a listing (`/r/Name/hot.json`). */
    fun parseListing(root: JSONObject, subreddit: String, limit: Int = 40): List<WallpaperModel> {
        val children = root.optJSONObject("data")?.optJSONArray("children") ?: return emptyList()
        val out = ArrayList<WallpaperModel>()
        for (i in 0 until children.length()) {
            val post = children.optJSONObject(i)?.optJSONObject("data") ?: continue
            if (post.optBoolean("over_18") || post.optBoolean("is_video")) continue
            // Galleries in a feed: just the first picture, so one post doesn't fill the rotation.
            val url = imagesOf(post).firstOrNull() ?: continue
            out += model(post, url, subreddit, index = 0)
            if (out.size >= limit) break
        }
        return out
    }

    /** Pictures in one post (`/comments/<id>.json`), every gallery image included. */
    fun parsePost(root: JSONArray): PostImages {
        val post = root.optJSONObject(0)?.optJSONObject("data")?.optJSONArray("children")
            ?.optJSONObject(0)?.optJSONObject("data") ?: return PostImages(emptyList(), nsfw = false)
        if (post.optBoolean("over_18")) return PostImages(emptyList(), nsfw = true)
        if (post.optBoolean("is_video")) return PostImages(emptyList(), nsfw = false)
        return PostImages(imagesOf(post), nsfw = false)
    }

    data class PostImages(val urls: List<String>, val nsfw: Boolean)

    /** True if `/r/Name/about.json` says the subreddit is marked 18+. */
    fun isNsfwSubreddit(about: JSONObject): Boolean =
        about.optJSONObject("data")?.optBoolean("over18") == true

    private fun imagesOf(post: JSONObject): List<String> {
        // Gallery: images in the author's order
        val media = post.optJSONObject("media_metadata")
        val items = post.optJSONObject("gallery_data")?.optJSONArray("items")
        if (post.optBoolean("is_gallery") && media != null && items != null) {
            val urls = ArrayList<String>()
            for (i in 0 until items.length()) {
                val mediaId = items.optJSONObject(i)?.optString("media_id").orEmpty()
                val meta = media.optJSONObject(mediaId) ?: continue
                if (meta.optString("e") != "Image") continue
                val u = meta.optJSONObject("s")?.optString("u").orEmpty().replace("&amp;", "&")
                if (isAllowedImageHost(u)) urls += u
            }
            return urls
        }
        // Single image: the post's own link if it's an image, else Reddit's full-size preview
        val direct = post.optString("url_overridden_by_dest").ifEmpty { post.optString("url") }
        val isImageFile = Regex("""\.(jpe?g|png|webp)(\?.*)?$""", RegexOption.IGNORE_CASE).containsMatchIn(direct)
        if (isImageFile && isAllowedImageHost(direct)) return listOf(direct)
        val preview = post.optJSONObject("preview")?.optJSONArray("images")?.optJSONObject(0)
            ?.optJSONObject("source")?.optString("url").orEmpty().replace("&amp;", "&")
        return if (isAllowedImageHost(preview)) listOf(preview) else emptyList()
    }

    private fun model(post: JSONObject, url: String, subreddit: String, index: Int) = WallpaperModel(
        id = "reddit_${post.optString("id", url.hashCode().toString())}_$index",
        title = post.optString("title", "r/$subreddit").take(120),
        url = url,
        author = "u/" + post.optString("author", "reddit_user").take(40),
        permalink = "https://reddit.com" + post.optString("permalink", "").take(200),
        source = WallpaperModel.SOURCE_REDDIT
    )

    // ---------------------------------------------------------------- network

    /** GETs a reddit.com JSON URL we built ourselves; null on any failure. */
    fun fetchJson(url: String): String? {
        require(url.startsWith("https://www.reddit.com/")) { "Only reddit.com JSON" }
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 8000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
                if (conn.contentLengthLong > MAX_JSON_BYTES) return null
                val bytes = conn.inputStream.use { input ->
                    val buf = java.io.ByteArrayOutputStream()
                    val chunk = ByteArray(32 * 1024)
                    while (true) {
                        val n = input.read(chunk)
                        if (n < 0) break
                        buf.write(chunk, 0, n)
                        if (buf.size() > MAX_JSON_BYTES) return null
                    }
                    buf.toByteArray()
                }
                String(bytes, Charsets.UTF_8)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun listingUrl(subreddit: String) = "https://www.reddit.com/r/$subreddit/hot.json?limit=40&raw_json=1"
    fun aboutUrl(subreddit: String) = "https://www.reddit.com/r/$subreddit/about.json?raw_json=1"
    fun postUrl(id: String) = "https://www.reddit.com/comments/$id.json?raw_json=1&limit=1"
}
