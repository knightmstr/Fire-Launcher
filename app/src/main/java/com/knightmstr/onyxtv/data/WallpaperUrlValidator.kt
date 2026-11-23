package com.knightmstr.onyxtv.data

import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks a user-supplied wallpaper URL before it's saved: must be an https link that is publicly
 * reachable and actually returns a real image. This is a one-time check when the user adds a link.
 *
 * It deliberately does NOT judge whether the *content* is safe/appropriate — that can't be done
 * reliably on-device, so the UI shows a short "you're responsible for links you add" disclaimer
 * instead. (A future option is a Safe Browsing lookup, which would slot in here.)
 */
object WallpaperUrlValidator {

    private const val MAX_BYTES = 15L * 1024 * 1024

    sealed interface Result {
        data object Ok : Result
        data class Error(val message: String) : Result
    }

    suspend fun validate(raw: String): Result = withContext(Dispatchers.IO) {
        val text = raw.trim()
        val url = try {
            URL(text)
        } catch (_: Exception) {
            return@withContext Result.Error("That doesn't look like a web address.")
        }
        if (!url.protocol.equals("https", ignoreCase = true)) {
            return@withContext Result.Error("Use an https:// link — plain http isn't allowed.")
        }
        if (url.host.isNullOrBlank()) {
            return@withContext Result.Error("That doesn't look like a web address.")
        }

        val conn = try {
            (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "OnyxTV (Fire TV)")
                setRequestProperty("Accept", "image/*")
            }
        } catch (_: Exception) {
            return@withContext Result.Error("Couldn't open that link.")
        }

        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.Error("The link isn't reachable (HTTP ${conn.responseCode}).")
            }
            if (conn.contentLengthLong > MAX_BYTES) {
                return@withContext Result.Error("That image is too large (over 15 MB).")
            }
            val bytes = conn.inputStream.use { readCapped(it, MAX_BYTES) }
                ?: return@withContext Result.Error("That image is too large (over 15 MB).")

            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            if (opts.outWidth <= 0 || opts.outHeight <= 0) {
                return@withContext Result.Error("That link doesn't open as an image we can show.")
            }
            Result.Ok
        } catch (_: Exception) {
            Result.Error("Couldn't reach that link. Check it's public and on the internet.")
        } finally {
            conn.disconnect()
        }
    }

    /** Reads the stream into memory, or null if it exceeds [max] bytes. */
    private fun readCapped(input: InputStream, max: Long): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            if (total > max) return null
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }
}
