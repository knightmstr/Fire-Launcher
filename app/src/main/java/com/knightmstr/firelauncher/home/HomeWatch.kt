package com.knightmstr.firelauncher.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.provider.Settings

/**
 * Switches the Home button watcher ([HomeWatchService]) on and off, and keeps it on.
 *
 * Fire OS has no "default home" choice: the Home key always opens Amazon's launcher. The watcher
 * sees Amazon's home come to the front and brings FireLauncher back. Amazon's home may flash for
 * a moment first; nothing short of rooting the TV avoids that.
 *
 * FireLauncher can switch the watcher on by itself only after one grant over ADB:
 *   adb shell pm grant com.knightmstr.firelauncher android.permission.WRITE_SECURE_SETTINGS
 * With that grant it also puts the watcher back after updates and restarts, when Android drops it.
 *
 * Approach from couch-tv's FireWatch (github.com/Seagey/couch-tv, MIT).
 */
object HomeWatch {

    enum class Status { ON, OFF, NEEDS_PERMISSION }

    const val AMAZON_HOME_PACKAGE = "com.amazon.tv.launcher"
    private val AMAZON_HOME_ACTIVITIES = setOf(
        "$AMAZON_HOME_PACKAGE.ui.HomeActivity_vNext",
        "$AMAZON_HOME_PACKAGE.ui.HomeActivity"
    )
    private const val PREFS = "home_watch"
    private const val KEY_WANTED = "wanted"
    private const val SETTING_SERVICES = "enabled_accessibility_services"
    private const val SETTING_ENABLED = "accessibility_enabled"
    private const val AMAZON_GRACE_MS = 8_000L

    val GRANT_COMMAND =
        "adb shell pm grant com.knightmstr.firelauncher android.permission.WRITE_SECURE_SETTINGS"

    @Volatile private var amazonAllowedUntil = 0L

    /** Set while Android has the service bound. */
    @Volatile var connected = false

    /** Amazon's own HOME activities, read once; anything the launcher declares as a home screen. */
    @Volatile private var declaredHomes: Set<String>? = null

    /**
     * FireLauncher is about to open one of Amazon's own screens (Fire TV settings): for a few
     * seconds Amazon's windows coming forward are that, not a Home press.
     */
    fun allowAmazonBriefly() {
        amazonAllowedUntil = SystemClock.elapsedRealtime() + AMAZON_GRACE_MS
    }

    fun amazonAllowedNow(): Boolean = SystemClock.elapsedRealtime() < amazonAllowedUntil

    /** True only for Amazon's home screen itself, not its Settings or other screens. */
    fun isAmazonHome(context: Context, pkg: CharSequence?, cls: CharSequence?): Boolean {
        if (pkg?.toString() != AMAZON_HOME_PACKAGE || cls == null) return false
        val name = cls.toString()
        return name in AMAZON_HOME_ACTIVITIES || name in homesOf(context)
    }

    private fun homesOf(context: Context): Set<String> = declaredHomes ?: runCatching {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName == AMAZON_HOME_PACKAGE }
            .map { it.activityInfo.name }
            .toSet()
    }.getOrDefault(emptySet()).also { declaredHomes = it }

    fun canManage(context: Context): Boolean =
        context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED

    fun isEnabled(context: Context): Boolean =
        enabledServices(context).any { it == serviceId(context) || it == shortServiceId(context) }

    fun status(context: Context): Status = when {
        isEnabled(context) -> Status.ON
        canManage(context) -> Status.OFF
        else -> Status.NEEDS_PERMISSION
    }

    /** Turns the watcher on or off and remembers the choice. Returns false without the ADB grant. */
    fun setWanted(context: Context, wanted: Boolean): Boolean {
        prefs(context).edit().putBoolean(KEY_WANTED, wanted).apply()
        if (!canManage(context)) return false
        return runCatching {
            val others = enabledServices(context).filter { it != serviceId(context) && it != shortServiceId(context) }
            val list = if (wanted) others + serviceId(context) else others
            val cr = context.contentResolver
            Settings.Secure.putString(cr, SETTING_SERVICES, list.joinToString(":"))
            if (wanted) Settings.Secure.putInt(cr, SETTING_ENABLED, 1)
        }.isSuccess
    }

    /** After an update, a restart or on resume: put the watcher back if the user wants it on. */
    fun ensure(context: Context) {
        if (!prefs(context).getBoolean(KEY_WANTED, false) || !canManage(context)) return
        if (!isEnabled(context)) setWanted(context, true)
    }

    private fun enabledServices(context: Context): List<String> =
        (Settings.Secure.getString(context.contentResolver, SETTING_SERVICES) ?: "")
            .split(':').filter { it.isNotBlank() && it != "null" }

    private fun serviceId(context: Context) = "${context.packageName}/${HomeWatchService::class.java.name}"
    private fun shortServiceId(context: Context) = "${context.packageName}/.home.HomeWatchService"
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** BOOT_COMPLETED: Android sometimes drops accessibility services; put ours back. */
    class BootReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) runCatching { ensure(context) }
        }
    }
}
