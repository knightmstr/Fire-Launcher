package com.knightmstr.onyxtv.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the user's own photos for wallpapers from two places:
 *
 * 1. Any photo album named **OnyxTV** (e.g. `Pictures/OnyxTV`; the old **FireLauncher** name works too) via MediaStore —
 *    needs the photo permission.
 * 2. The app's own folder `Android/data/<package>/files/Pictures` — no permission needed;
 *    fill it with `adb push photo.jpg /sdcard/Android/data/<package>/files/Pictures/`.
 */
class LocalPhotoSource(private val context: Context) {

    companion object {
        const val ALBUM_NAME = "OnyxTV"
        /** Album name before the rename to Onyx TV; still read so existing albums keep working. */
        const val LEGACY_ALBUM_NAME = "FireLauncher"
        private const val MAX_PHOTOS = 200
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

        /** Hard caps for the in-app photo uploader (see [saveUpload]). */
        const val MAX_UPLOADS = 200
        const val MAX_UPLOAD_BYTES = 20L * 1024 * 1024

        /** Characters allowed in an uploaded file's name; anything else is stripped or rejected. */
        private val SAFE_NAME_CHARS: Set<Char> =
            (('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf('.', '-', '_')).toSet()

        val PHOTO_PERMISSION: String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
            else Manifest.permission.READ_EXTERNAL_STORAGE
    }

    fun hasPhotoPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, PHOTO_PERMISSION) == PackageManager.PERMISSION_GRANTED

    suspend fun load(): List<WallpaperModel> = withContext(Dispatchers.IO) {
        (fromMediaStore() + fromAppFolder()).distinctBy { it.url }.take(MAX_PHOTOS)
    }

    // ------------------------------------------------------------------ Uploads (app-private folder)
    //
    // These operate ONLY on the app's own Pictures folder (getExternalFilesDir) — the same place
    // `adb push` and the in-app uploader write to. They never touch the user's OnyxTV gallery album,
    // so they need no storage permission and show no system delete prompts.

    /** The app-private folder wallpapers are uploaded into. Created if missing. */
    fun uploadDir(): File =
        (context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: File(context.filesDir, "Pictures")).apply { mkdirs() }

    /** Image files currently in the upload folder, newest first. */
    fun listUploads(): List<File> =
        uploadDir().listFiles().orEmpty()
            .filter { it.isFile && it.extension.lowercase() in IMAGE_EXTENSIONS }
            .sortedByDescending { it.lastModified() }

    /** Deletes every uploaded photo. Returns how many were removed. */
    fun clearUploads(): Int = listUploads().count { it.delete() }

    /** Deletes one uploaded photo by file name. Confined to the upload folder. */
    fun deleteUpload(name: String): Boolean {
        val target = resolveInUploadDir(name) ?: return false
        return target.isFile && target.delete()
    }

    /**
     * Validates [bytes] is a real image and saves it into the upload folder under a safe,
     * generated name. Returns the saved file, or null if rejected (not an image, too large,
     * or the folder is full). The claimed [originalName] is only a hint for the display name;
     * the real type is taken from the decoded image, never the name or client-sent MIME.
     */
    fun saveUpload(bytes: ByteArray, originalName: String?): File? {
        if (bytes.isEmpty() || bytes.size > MAX_UPLOAD_BYTES) return null
        if (listUploads().size >= MAX_UPLOADS) return null
        val ext = detectImageExtension(bytes) ?: return null
        val base = safeBaseName(originalName)
        val target = uniqueFile(base, ext)
        return try {
            target.outputStream().use { it.write(bytes) }
            target
        } catch (_: Exception) {
            null
        }
    }

    /** Resolves a client-supplied name to a real file strictly inside the upload folder, or null. */
    private fun resolveInUploadDir(name: String): File? {
        val leaf = name.substringAfterLast('/').substringAfterLast('\\')
        if (leaf.isBlank() || leaf == "." || leaf == "..") return null
        if (leaf.any { it !in SAFE_NAME_CHARS }) return null
        val dir = uploadDir().canonicalFile
        val file = File(dir, leaf).canonicalFile
        return if (file.parentFile == dir) file else null
    }

    /** Confirms the bytes decode as an image and returns its real extension, else null. */
    private fun detectImageExtension(bytes: ByteArray): String? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        return when (opts.outMimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> null
        }
    }

    private fun safeBaseName(originalName: String?): String {
        val leaf = originalName?.substringAfterLast('/')?.substringAfterLast('\\')?.substringBeforeLast('.')
        val cleaned = leaf?.filter { it in SAFE_NAME_CHARS }?.take(40)?.trim('.', '-', '_')
        return if (cleaned.isNullOrBlank()) "photo" else cleaned
    }

    private fun uniqueFile(base: String, ext: String): File {
        val dir = uploadDir()
        var candidate = File(dir, "$base.$ext")
        var i = 1
        while (candidate.exists()) {
            candidate = File(dir, "${base}_$i.$ext")
            i++
        }
        return candidate
    }

    private fun fromMediaStore(): List<WallpaperModel> {
        if (!hasPhotoPermission()) return emptyList()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
        val result = mutableListOf<WallpaperModel>()
        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} IN (?, ?)",
                arrayOf(ALBUM_NAME, LEGACY_ALBUM_NAME),
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                while (cursor.moveToNext() && result.size < MAX_PHOTOS) {
                    val id = cursor.getLong(idCol)
                    result += localWallpaper(
                        id = "ms_$id",
                        name = cursor.getString(nameCol).orEmpty(),
                        uri = ContentUris.withAppendedId(collection, id)
                    )
                }
            }
        }
        return result
    }

    private fun fromAppFolder(): List<WallpaperModel> {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return emptyList()
        return dir.listFiles().orEmpty()
            .filter { it.isFile && it.extension.lowercase() in IMAGE_EXTENSIONS }
            .sortedByDescending { it.lastModified() }
            .map { file -> localWallpaper("file_${file.name}", file.name, Uri.fromFile(file)) }
    }

    private fun localWallpaper(id: String, name: String, uri: Uri) = WallpaperModel(
        id = id,
        title = name.substringBeforeLast('.').ifBlank { "Photo" },
        url = uri.toString(),
        author = "You",
        permalink = "",
        source = WallpaperModel.SOURCE_LOCAL
    )
}
