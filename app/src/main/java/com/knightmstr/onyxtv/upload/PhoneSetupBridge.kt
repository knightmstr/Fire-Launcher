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

    // ---------------------------------------------------------------- Backup

    fun exportBackup(): BackupData

    /** Restores a backup already checked by [BackupJson.parse]. */
    fun restoreBackup(backup: BackupData)

    // ---------------------------------------------------------------- Safety

    /**
     * True while Kids mode is on. The server refuses every change while locked, so a phone can
     * never be used to get round the Kids mode PIN.
     */
    fun isLocked(): Boolean

    /** Called after anything changed, so the TV can refresh (e.g. reload wallpapers). */
    fun onChanged() {}
}

/** An installed app as the phone page sees it. No icons inline: those come from /api/icon. */
data class SetupApp(
    val packageName: String,
    val label: String,
    val originalLabel: String,
    val hidden: Boolean
)
