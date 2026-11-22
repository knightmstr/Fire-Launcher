package com.knightmstr.onyxtv.media

import android.service.notification.NotificationListenerService

/**
 * Empty notification listener. Android only lets apps read other apps' media sessions
 * (title, artist, play/pause) while one of their notification listeners is enabled,
 * so this exists purely to hold that one-time permission. It never reads notifications.
 *
 * Grant on Fire TV (if Settings doesn't offer it):
 *   adb shell cmd notification allow_listener com.knightmstr.onyxtv/com.knightmstr.onyxtv.media.MediaListenerService
 */
class MediaListenerService : NotificationListenerService()
