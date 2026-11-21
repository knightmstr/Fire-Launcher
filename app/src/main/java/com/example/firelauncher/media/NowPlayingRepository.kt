package com.example.firelauncher.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What's playing right now in another app. */
data class NowPlaying(
    val packageName: String,
    val appLabel: String,
    val title: String,
    val subtitle: String?,
    val isPlaying: Boolean
)

/**
 * Watches active media sessions (Spotify, YouTube, Plex…) and exposes the most relevant one.
 * Only runs while the launcher is in the foreground, and only after the user has enabled
 * [MediaListenerService] once.
 */
class NowPlayingRepository(private val context: Context) {

    companion object {
        val ADB_GRANT_COMMAND =
            "adb shell cmd notification allow_listener " +
                "com.example.firelauncher/com.example.firelauncher.media.MediaListenerService"

        private val PAUSED_STATES = setOf(
            PlaybackState.STATE_PAUSED,
            PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_CONNECTING
        )
    }

    private val sessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, MediaListenerService::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _hasAccess = MutableStateFlow(false)
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    private var controller: MediaController? = null
    private var listening = false

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { sessions ->
        selectController(sessions.orEmpty())
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() = refreshSessions()
    }

    fun checkAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
            .also { _hasAccess.value = it }

    fun start() {
        if (listening || sessionManager == null || !checkAccess()) return
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, mainHandler)
            listening = true
            refreshSessions()
        } catch (_: SecurityException) {
            _hasAccess.value = false
        }
    }

    fun stop() {
        if (!listening) return
        runCatching { sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener) }
        controller?.unregisterCallback(controllerCallback)
        controller = null
        listening = false
        _nowPlaying.value = null
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    fun skipNext() { controller?.transportControls?.skipToNext() }
    fun skipPrevious() { controller?.transportControls?.skipToPrevious() }

    private fun refreshSessions() {
        val sessions = try {
            sessionManager?.getActiveSessions(listenerComponent).orEmpty()
        } catch (_: SecurityException) {
            _hasAccess.value = false
            emptyList()
        }
        selectController(sessions)
    }

    /** Prefer a session that is playing, then one that is paused/buffering. */
    private fun selectController(sessions: List<MediaController>) {
        val active = sessions.filter { it.packageName != context.packageName }
        val best = active.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: active.firstOrNull { it.playbackState?.state in PAUSED_STATES }

        if (best?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(controllerCallback)
            controller = best
            best?.registerCallback(controllerCallback, mainHandler)
        }
        publish()
    }

    private fun publish() {
        val c = controller
        val state = c?.playbackState?.state
        if (c == null || state == null || state !in (PAUSED_STATES + PlaybackState.STATE_PLAYING)) {
            _nowPlaying.value = null
            return
        }
        val metadata = c.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.description?.title?.toString()
        val subtitle = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.description?.subtitle?.toString()
        val appLabel = runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0)).toString()
        }.getOrDefault(c.packageName)

        _nowPlaying.value = NowPlaying(
            packageName = c.packageName,
            appLabel = appLabel,
            title = title?.takeIf { it.isNotBlank() } ?: appLabel,
            subtitle = subtitle?.takeIf { it.isNotBlank() },
            isPlaying = state == PlaybackState.STATE_PLAYING
        )
    }
}
