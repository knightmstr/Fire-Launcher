package com.knightmstr.onyxtv.home

import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Which kind of TV Onyx TV is running on, and the words and packages that differ.
 *
 * Fire TV keeps exactly the behaviour it always had; the Google TV / Android TV parts only take
 * effect when one of those is detected. Detection uses the system's own markers (Amazon's Fire TV
 * hardware feature, or which stock home launcher is installed), never anything a user sets.
 */
object Platform {

    enum class Kind { FIRE_TV, GOOGLE_TV, ANDROID_TV }

    const val AMAZON_HOME = "com.amazon.tv.launcher"
    const val GOOGLE_TV_HOME = "com.google.android.apps.tv.launcherx"
    const val ANDROID_TV_HOME = "com.google.android.tvlauncher"
    const val LEANBACK_HOME = "com.google.android.leanbacklauncher"

    /** Stock home screens the Home button watcher swaps for Onyx TV (see res/xml/home_watch.xml). */
    val STOCK_HOMES = setOf(AMAZON_HOME, GOOGLE_TV_HOME, ANDROID_TV_HOME, LEANBACK_HOME)

    /** Text that changes by platform. */
    data class Names(
        /** "Fire TV", "Google TV", "Android TV": the device, in settings and help. */
        val device: String,
        /** Whose home / screensaver it is: "Amazon's", "Google's". */
        val owner: String,
        /** How to reveal Developer options on this platform. */
        val developerOptionsHint: String,
        /** Where to switch on ADB. */
        val adbHint: String,
        /** Where the system screensaver start time lives. */
        val screensaverTimerPath: String,
        /** Where "install unknown apps" lives. */
        val unknownAppsPath: String
    )

    /** Pure: decide the platform from what the system reports. */
    fun kindFrom(manufacturer: String?, hasFireTvFeature: Boolean, installed: Set<String>): Kind = when {
        hasFireTvFeature || manufacturer.equals("Amazon", ignoreCase = true) || AMAZON_HOME in installed -> Kind.FIRE_TV
        GOOGLE_TV_HOME in installed -> Kind.GOOGLE_TV
        else -> Kind.ANDROID_TV
    }

    fun namesFor(kind: Kind): Names = when (kind) {
        Kind.FIRE_TV -> Names(
            device = "Fire TV",
            owner = "Amazon's",
            developerOptionsHint = "Hidden? Settings → My Fire TV → About, press OK on the device name 7 times",
            adbHint = "Turn on ADB debugging",
            screensaverTimerPath = "Display & Sounds → Screensaver",
            unknownAppsPath = "Settings → My Fire TV → Developer options → Install unknown apps"
        )
        Kind.GOOGLE_TV -> Names(
            device = "Google TV",
            owner = "Google's",
            developerOptionsHint = "Hidden? Settings → System → About, press OK on Android TV OS build 7 times",
            adbHint = "Turn on USB debugging (and Wireless debugging for the computer method)",
            screensaverTimerPath = "System → Energy → Ambient mode / screensaver",
            unknownAppsPath = "Settings → Apps → Security & restrictions → Unknown sources"
        )
        Kind.ANDROID_TV -> Names(
            device = "Android TV",
            owner = "the TV's",
            developerOptionsHint = "Hidden? Settings → Device Preferences → About, press OK on Build 7 times",
            adbHint = "Turn on USB debugging (or Network debugging)",
            screensaverTimerPath = "Device Preferences → Screen saver",
            unknownAppsPath = "Settings → Device Preferences → Security & restrictions → Unknown sources"
        )
    }

    @Volatile private var cached: Kind? = null

    fun kind(context: Context): Kind = cached ?: run {
        val pm = context.packageManager
        val installed = runCatching {
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
                .map { it.activityInfo.packageName }.toSet()
        }.getOrDefault(emptySet())
        kindFrom(Build.MANUFACTURER, pm.hasSystemFeature("amazon.hardware.fire_tv"), installed)
    }.also { cached = it }

    fun names(context: Context): Names = namesFor(kind(context))

    fun isFireTv(context: Context) = kind(context) == Kind.FIRE_TV

    /**
     * Android 13+ blocks sideloaded apps from switching on notification access (and
     * accessibility) in Settings until "Allow restricted settings" is chosen in the app's info.
     * Fire OS doesn't have that step.
     */
    fun needsRestrictedSettingsStep(context: Context) =
        !isFireTv(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
