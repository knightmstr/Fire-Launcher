package com.knightmstr.onyxtv.tools

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.knightmstr.onyxtv.home.SelfAdb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Puts the Fire TV to sleep after a chosen time, even while another app is playing.
 *
 * An alarm wakes [SleepTimerReceiver], which presses the Sleep key through this TV's own ADB
 * ([SelfAdb.Command.SLEEP]). With HDMI-CEC on (Fire TV: Display & Sounds → HDMI CEC Device
 * Control), the TV turns off with it. Needs ADB debugging on, like the Home button setup.
 */
object SleepTimer {

    /** Choices offered on the TV, in minutes; 0 = off. */
    val OPTIONS = listOf(0, 15, 30, 45, 60, 90, 120)

    private const val PREFS = "sleep_timer"
    private const val KEY_ENDS_AT = "ends_at_wall_ms"
    private const val WINDOW_MS = 60_000L

    private val _endsAt = MutableStateFlow<Long?>(null)
    /** Wall-clock time the timer goes off, or null when off. */
    val endsAt: StateFlow<Long?> = _endsAt.asStateFlow()

    /** Reads the saved timer (it's gone after a restart: alarms don't survive one). */
    fun load(context: Context) {
        val at = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_ENDS_AT, 0L)
        _endsAt.value = at.takeIf { it > System.currentTimeMillis() }
    }

    /** Minutes left, rounded up; 0 when off. */
    fun minutesLeft(now: Long = System.currentTimeMillis()): Int {
        val at = _endsAt.value ?: return 0
        return ((at - now + 59_999) / 60_000).toInt().coerceAtLeast(0)
    }

    /** The next option after what's running now (off → 15 → 30 … → 120 → off). */
    fun nextOption(now: Long = System.currentTimeMillis()): Int {
        val left = minutesLeft(now)
        return OPTIONS.firstOrNull { it > left } ?: 0
    }

    fun set(context: Context, minutes: Int) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context)
        alarms.cancel(pending)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (minutes <= 0) {
            prefs.edit().remove(KEY_ENDS_AT).apply()
            _endsAt.value = null
            return
        }
        val delayMs = minutes * 60_000L
        val trigger = SystemClock.elapsedRealtime() + delayMs
        // A one-minute window: precise enough for a sleep timer, and needs no exact-alarm permission.
        alarms.setWindow(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, WINDOW_MS, pending)
        val endsAt = System.currentTimeMillis() + delayMs
        prefs.edit().putLong(KEY_ENDS_AT, endsAt).apply()
        _endsAt.value = endsAt
    }

    internal fun cleared(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ENDS_AT).apply()
        _endsAt.value = null
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, SleepTimerReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

/** Fires when the sleep timer ends. Not exported: only our own alarm can start it. */
class SleepTimerReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        SleepTimer.cleared(app)
        val pending = goAsync()
        scope.launch {
            try {
                SelfAdb.run(app, SelfAdb.Command.SLEEP, approvalWaitMs = 5_000)
            } finally {
                pending.finish()
            }
        }
    }
}
