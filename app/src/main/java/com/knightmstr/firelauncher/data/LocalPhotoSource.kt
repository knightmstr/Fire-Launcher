package com.knightmstr.firelauncher.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the user's own photos for wallpapers from two places:
 *
 * 1. Any photo album named **FireLauncher** (e.g. `Pictures/FireLauncher`) via MediaStore —
 *    needs the photo permission.
 * 2. The app's own folder `Android/data/<package>/files/Pictures` — no permission needed;
 *    fill it with `adb push photo.jpg /sdcard/Android/data/<package>/files/Pictures/`.
 */
class LocalPhotoSource(private val context: Context) {

    companion object {
        const val ALBUM_NAME = "FireLauncher"
        private const val MAX_PHOTOS = 200
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

        val PHOTO_PERMISSION: String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
            else Manifest.permission.READ_EXTERNAL_STORAGE
    }

    fun hasPhotoPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, PHOTO_PERMISSION) == PackageManager.PERMISSION_GRANTED

    /** Absolute path of the no-permission folder, for showing in Settings. */
    val appFolderPath: String?
        get() = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath

    suspend fun load(): List<WallpaperModel> = withContext(Dispatchers.IO) {
        (fromMediaStore() + fromAppFolder()).distinctBy { it.url }.take(MAX_PHOTOS)
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
                "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?",
                arrayOf(ALBUM_NAME),
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
