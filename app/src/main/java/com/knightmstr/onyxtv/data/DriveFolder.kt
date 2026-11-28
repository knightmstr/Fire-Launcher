package com.knightmstr.onyxtv.data

import java.net.HttpURLConnection
import java.net.URL

/**
 * Lists the photos in a Google Drive folder shared with "Anyone with the link", without a
 * sign-in or API key, using Drive's simple embedded folder page.
 *
 * That page isn't an official API, so parsing is deliberately forgiving: it looks for file
 * entries and their names, keeps picture files, and ignores everything else. If Google changes
 * the page, the folder simply yields no photos and the app keeps the last list it found.
 *
 * Parsing is pure (testable on the JVM); [fetchHtml] does the network part.
 */
object DriveFolder {

    /** Most photos taken from one folder. */
    const val MAX_PHOTOS = 100

    private const val MAX_HTML_BYTES = 3L * 1024 * 1024
    private val DRIVE_ID = Regex("^[A-Za-z0-9_-]{10,200}$")
    private val IMAGE_NAME = Regex("""\.(jpe?g|png|webp)$""", RegexOption.IGNORE_CASE)

    data class Entry(val id: String, val title: String, val isFolder: Boolean)

    private val RESOURCE_KEY = Regex("^[A-Za-z0-9_-]{1,100}$")

    fun embeddedUrl(folderId: String, resourceKey: String? = null): String {
        require(DRIVE_ID.matches(folderId)) { "Bad folder id" }
        require(resourceKey == null || RESOURCE_KEY.matches(resourceKey)) { "Bad resource key" }
        return "https://drive.google.com/embeddedfolderview?id=$folderId" +
            (resourceKey?.let { "&resourcekey=$it" } ?: "")
    }

    /** The direct link the TV loads for one photo, same form as a single shared Drive photo. */
    fun photoUrl(fileId: String) = "https://drive.google.com/uc?export=view&id=$fileId"

    /** Every file and sub-folder entry on the embedded page, in page order. */
    fun parseEntries(html: String): List<Entry> {
        val starts = Regex("""id="entry-([A-Za-z0-9_-]{10,200})"""").findAll(html).toList()
        val out = ArrayList<Entry>()
        val seen = HashSet<String>()
        for ((i, m) in starts.withIndex()) {
            val id = m.groupValues[1]
            if (!seen.add(id)) continue
            val end = starts.getOrNull(i + 1)?.range?.first ?: html.length
            val block = html.substring(m.range.last + 1, end)
            val href = Regex("""href="([^"]+)"""").find(block)?.groupValues?.get(1).orEmpty()
            val title = Regex("""class="flip-entry-title"[^>]*>([^<]*)<""").find(block)?.groupValues?.get(1)
                ?.let { unescape(it).trim() }.orEmpty()
            val isFolder = href.contains("/drive/folders/") || href.contains("/folderview")
            out += Entry(id, title, isFolder)
        }
        return out
    }

    /** Direct photo links for the picture files in [html] (sub-folders and other files skipped). */
    fun photoUrls(html: String): List<String> =
        parseEntries(html).asSequence()
            .filter { !it.isFolder && IMAGE_NAME.containsMatchIn(it.title) }
            .map { photoUrl(it.id) }
            .take(MAX_PHOTOS)
            .toList()

    private fun unescape(s: String) = s
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&#39;", "'")

    /**
     * The embedded page for a public folder, or null. Redirects are not followed: a private
     * folder redirects to a Google sign-in page, which counts as "not shared".
     */
    fun fetchHtml(folderId: String, resourceKey: String? = null): String? = try {
        val conn = (URL(embeddedUrl(folderId, resourceKey)).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 10_000
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 9; AFTMM) OnyxTV")
            setRequestProperty("Accept", "text/html")
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                null
            } else {
                conn.inputStream.use { input ->
                    val buf = java.io.ByteArrayOutputStream()
                    val chunk = ByteArray(32 * 1024)
                    var tooBig = false
                    while (true) {
                        val n = input.read(chunk)
                        if (n < 0) break
                        buf.write(chunk, 0, n)
                        if (buf.size() > MAX_HTML_BYTES) { tooBig = true; break }
                    }
                    if (tooBig) null else String(buf.toByteArray(), Charsets.UTF_8)
                }
            }
        } finally {
            conn.disconnect()
        }
    } catch (e: Exception) {
        null
    }
}
