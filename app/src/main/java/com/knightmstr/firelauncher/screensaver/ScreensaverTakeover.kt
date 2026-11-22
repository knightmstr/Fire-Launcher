package com.knightmstr.firelauncher.screensaver

import android.content.Context
import android.provider.Settings
import com.knightmstr.firelauncher.home.HomeWatch

/**
 * Makes Onyx TV's screensaver ([FireDream]) the Fire TV's own screensaver, and optionally
 * stops the Fire TV from going to sleep. Uses the same one-time ADB grant as the Home button
 * watcher (WRITE_SECURE_SETTINGS). Every value changed is remembered and put back when turned off.
 *
 * Settings used are the ones screensaver apps such as Aerial Views document for Fire TV:
 * screensaver_components (+ screensaver_default_component on Fire OS 7.6), screensaver_enabled,
 * and amazon_ambient_enabled = 0 on Fire OS 8 (Amazon's Ambient Experience; needs one restart).
 */
object ScreensaverTakeover {

    enum class Status { ON, OFF, NEEDS_PERMISSION }

    private const val PREFS = "screensaver_takeover"
    private const val KEY_ON = "on"
    private const val KEY_AWAKE = "keep_awake"
    private const val SAVED_PREFIX = "saved_"

    private const val COMPONENTS = "screensaver_components"
    private const val DEFAULT_COMPONENT = "screensaver_default_component"
    private const val ENABLED = "screensaver_enabled"
    private const val AMBIENT = "amazon_ambient_enabled"
    private const val SLEEP_TIMEOUT = "sleep_timeout"

    fun component(context: Context) = "${context.packageName}/${FireDream::class.java.name}"

    fun status(context: Context): Status = when {
        isActive(context) -> Status.ON
        HomeWatch.canManage(context) -> Status.OFF
        else -> Status.NEEDS_PERMISSION
    }

    fun isActive(context: Context): Boolean =
        Settings.Secure.getString(context.contentResolver, COMPONENTS)?.contains(FireDream::class.java.name) == true

    fun keepAwake(context: Context): Boolean = prefs(context).getBoolean(KEY_AWAKE, false)

    /** Turns the takeover on or off. Returns false without the ADB grant. */
    fun setOn(context: Context, on: Boolean): Boolean {
        if (!HomeWatch.canManage(context)) return false
        prefs(context).edit().putBoolean(KEY_ON, on).apply()
        return runCatching {
            if (on) {
                save(context, COMPONENTS); save(context, DEFAULT_COMPONENT); save(context, ENABLED); save(context, AMBIENT)
                put(context, COMPONENTS, component(context))
                put(context, DEFAULT_COMPONENT, component(context))
                put(context, ENABLED, "1")
                // Fire OS 8: Amazon's Ambient Experience replaces screensavers unless it's off
                if (get(context, AMBIENT) != null) put(context, AMBIENT, "0")
            } else {
                restore(context, COMPONENTS); restore(context, DEFAULT_COMPONENT)
                restore(context, ENABLED); restore(context, AMBIENT)
            }
        }.isSuccess
    }

    /** Keep the Fire TV awake (no sleep timer). Returns false without the ADB grant. */
    fun setKeepAwake(context: Context, awake: Boolean): Boolean {
        if (!HomeWatch.canManage(context)) return false
        prefs(context).edit().putBoolean(KEY_AWAKE, awake).apply()
        return runCatching {
            if (awake) {
                save(context, SLEEP_TIMEOUT)
                put(context, SLEEP_TIMEOUT, "-1") // negative: never sleep on a timer
            } else {
                restore(context, SLEEP_TIMEOUT)
            }
        }.isSuccess
    }

    /** True when Fire OS 8's Ambient Experience was just switched off and needs a restart. */
    fun needsRestart(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ON, false) && prefs(context).getString(SAVED_PREFIX + AMBIENT, null) == "1"

    /** After updates and restarts: put our values back if Fire OS reset them. */
    fun ensure(context: Context) {
        if (!HomeWatch.canManage(context)) return
        val p = prefs(context)
        runCatching {
            if (p.getBoolean(KEY_ON, false) && !isActive(context)) {
                put(context, COMPONENTS, component(context))
                put(context, DEFAULT_COMPONENT, component(context))
                put(context, ENABLED, "1")
            }
            if (p.getBoolean(KEY_AWAKE, false) && get(context, SLEEP_TIMEOUT) != "-1") put(context, SLEEP_TIMEOUT, "-1")
        }
    }

    // Remember the value before our first change, so turning off restores Amazon's setting
    private fun save(context: Context, key: String) {
        val p = prefs(context)
        if (!p.contains(SAVED_PREFIX + key)) p.edit().putString(SAVED_PREFIX + key, get(context, key) ?: NONE).apply()
    }

    private fun restore(context: Context, key: String) {
        val p = prefs(context)
        val saved = p.getString(SAVED_PREFIX + key, null) ?: return
        if (saved == NONE) Settings.Secure.putString(context.contentResolver, key, null) else put(context, key, saved)
        p.edit().remove(SAVED_PREFIX + key).apply()
    }

    private fun get(context: Context, key: String): String? = Settings.Secure.getString(context.contentResolver, key)
    private fun put(context: Context, key: String, value: String) {
        Settings.Secure.putString(context.contentResolver, key, value)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private const val NONE = "\u0000none"
}
