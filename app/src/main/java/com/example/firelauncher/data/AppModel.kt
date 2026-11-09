package com.example.firelauncher.data

import android.graphics.drawable.Drawable

/**
 * Model representing an installed application or a system shortcut on Fire TV.
 */
data class AppModel(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable,
    val banner: Drawable? = null,
    val accentColor: Int = 0xFFFF9900.toInt(),
    val isSystemShortcut: Boolean = false,
    val systemIntentAction: String? = null,
    val launchCount: Int = 0,
    val lastLaunchedTimeMs: Long = 0L
) {
    val uniqueKey: String
        get() = if (isSystemShortcut) "sys:${systemIntentAction ?: label}" else packageName
}
