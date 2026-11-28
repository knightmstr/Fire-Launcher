package com.knightmstr.onyxtv.upload

import com.knightmstr.onyxtv.data.BackupData
import com.knightmstr.onyxtv.data.LauncherSettings
import java.io.File

/**
 * Everything the phone-setup web server is allowed to read or change, as plain types.
 *
 * The server only talks to this interface, never to DataStore, PackageManager or the ViewModel
 * directly. That keeps the HTTP layer free of Android classes, so it can be started and attacked
 * in plain JVM unit tests (see PhoneSetupServerTest) with a fake bridge.
 *
 * Calls arrive on NanoHTTPD's worker threads and may block.
 */
interface PhoneSetupBridge {

    // ---------------------------------------------------------------- Photos

    fun listPhotos(): List<File>
    fun savePhoto(bytes: ByteArray, originalName: String?): File?
    fun deletePhoto(name: String): Boolean

    // ---------------------------------------------------------------- Apps

    /** Every installed (non-shortcut) app, hidden ones included. */
    fun apps(): List<SetupApp>

    /** Favourite package names in their current order (may include apps no longer installed). */
    fun favoriteOrder(): List<String>

    /** PNG bytes of an installed app's icon, or null if [packageName] isn't installed. */
    fun iconPng(packageName: String): ByteArray?

    fun setFavoriteOrder(order: List<String>)
    fun setHidden(packageName: String, hidden: Boolean)

    /** Blank [label] (or the app's own name) restores the original name. */
    fun rename(packageName: String, label: String?)

    // ---------------------------------------------------------------- Settings

    fun settings(): LauncherSettings

    /** Saves settings already checked by [SettingsJson.apply]. */
    fun updateSettings(settings: LauncherSettings)

    // ---------------------------------------------------------------- Wallpaper links ("My links")

    fun wallpaperUrls(): List<String>

    /**
     * Checks a picture link, cloud share link, subreddit or Reddit post online and saves what it
     * resolves to (see WallpaperLinkResolver).
     */
    fun addWallpaperUrl(url: String): LinkResult

    fun removeWallpaperUrl(url: String)

    // ---------------------------------------------------------------- Backup

    fun exportBackup(): BackupData

    /** Restores a backup already checked by [BackupJson.parse]. */
    fun restoreBackup(backup: BackupData)

    // ---------------------------------------------------------------- Install an app (sideload)

    /** Free bytes where uploaded apps are kept until installed. */
    fun freeSpaceBytes(): Long

    /** Folder the server streams an uploaded APK into. */
    fun apkWorkDir(): File

    /**
     * Opens the TV's install screen for [apk] (the bridge deletes the file once it's handed over).
     * Nothing installs until someone presses Install on the TV.
     */
    fun startApkInstall(apk: File): ApkStart

    fun apkStatus(): ApkStatus

    // ---------------------------------------------------------------- Safety

    /**
     * True while Kids mode is on. The server refuses every change while locked, so a phone can
     * never be used to get round the Kids mode PIN.
     */
    fun isLocked(): Boolean

    /** Called after anything changed, so the TV can refresh (e.g. reload wallpapers). */
    fun onChanged() {}
}

sealed class LinkResult {
    /** [what] is e.g. "picture", "3 pictures", "r/EarthPorn". */
    data class Added(val what: String) : LinkResult()
    data class Failed(val message: String) : LinkResult()
}

sealed class ApkStart {
    data class Started(val status: ApkStatus) : ApkStart()
    data class Failed(val message: String) : ApkStart()
}

/** Where the last phone install is up to. [state]: idle, waiting, installed, cancelled, failed. */
data class ApkStatus(
    val state: String = "idle",
    val label: String? = null,
    val packageName: String? = null,
    val versionName: String? = null,
    val isUpdate: Boolean = false,
    val message: String? = null,
    val serial: Int = 0
)

/** An installed app as the phone page sees it. No icons inline: those come from /api/icon. */
data class SetupApp(
    val packageName: String,
    val label: String,
    val originalLabel: String,
    val hidden: Boolean
)
