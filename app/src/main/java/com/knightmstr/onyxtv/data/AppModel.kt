package com.knightmstr.onyxtv.data

import android.graphics.drawable.Drawable

/**
 * Model representing an installed application or a system shortcut on Fire TV.
 */
data class AppModel(
    val packageName: String,
    val activityName: String,
    val label: String,
    /** Name reported by the app itself; differs from [label] when the user renamed it. */
    val originalLabel: String = label,
    val icon: Drawable,
    val banner: Drawable? = null,
    val accentColor: Int = 0xFFFF9900.toInt(),
    val isSystemShortcut: Boolean = false,
    val systemIntentAction: String? = null,
    val launchCount: Int = 0,
    val lastLaunchedTimeMs: Long = 0L,
    val isHidden: Boolean = false,
    /** Allowed while Kids mode is on. */
    val isKidsAllowed: Boolean = false,
    /** For a dock folder tile: the apps inside, in order. */
    val folderApps: List<AppModel> = emptyList()
) {
    /** A dock folder: its [packageName] is the favourites key `folder:<id>`. */
    val isFolder: Boolean
        get() = packageName.startsWith(AppFolders.KEY_PREFIX)

    val isRenamed: Boolean
        get() = label != originalLabel

    val uniqueKey: String
        get() = if (isSystemShortcut) "sys:${systemIntentAction ?: label}" else packageName
}
