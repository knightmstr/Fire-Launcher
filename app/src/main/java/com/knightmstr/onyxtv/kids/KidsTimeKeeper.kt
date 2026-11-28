package com.knightmstr.onyxtv.kids

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.SystemClock
import com.knightmstr.onyxtv.MainActivity
import com.knightmstr.onyxtv.data.LauncherSettings
import com.knightmstr.onyxtv.home.SelfAdb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Kids mode screen time and bedtime on the device: stores the rules and today's usage, ticks
 * about once a minute while Kids mode is on, and when time runs out brings Onyx TV forward,
 * where a "Time's up" screen waits for the parent's PIN.
 *
 * Like the rest of Kids mode this locks the launcher, not the whole device. Kids mode's own
 * rules (PIN, allowed apps) live in DataStore; these are kept in plain SharedPreferences so the
 * alarm receiver can read and write them quickly without a coroutine.
 */
object KidsTimeKeeper {

    private const val PREFS = "kids_time"
    private const val K_ENABLED = "kids_enabled_mirror"
    private const val K_LIMIT = "daily_limit_min"
    private const val K_BED_ON = "bedtime_on"
    private const val K_BED_START = "bedtime_start"
    private const val K_BED_END = "bedtime_end"
    private const val K_DAY = "usage_day"
    private const val K_USED = "usage_ms"
    private const val K_LAST = "last_tick_elapsed"
    private const val K_OVERRIDE = "override_until"
    private const val TICK_MS = 60_000L
    private const val WINDOW_MS = 20_000L

    data class Status(
        val rules: KidsTime.Rules,
        val usedMs: Long,
        val lock: KidsTime.Lock?
    )

    private val _status = MutableStateFlow<Status?>(null)
    val status: StateFlow<Status?> = _status.asStateFlow()

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun rules(c: Context): KidsTime.Rules = prefs(c).let {
        KidsTime.Rules(
            dailyLimitMinutes = it.getInt(K_LIMIT, 0),
            bedtimeOn = it.getBoolean(K_BED_ON, false),
            bedtimeStartHour = it.getInt(K_BED_START, 20),
            bedtimeEndHour = it.getInt(K_BED_END, 7)
        )
    }

    fun setRules(c: Context, rules: KidsTime.Rules) {
        prefs(c).edit()
            .putInt(K_LIMIT, rules.dailyLimitMinutes.coerceAtLeast(0))
            .putBoolean(K_BED_ON, rules.bedtimeOn)
            .putInt(K_BED_START, LauncherSettings.clampHour(rules.bedtimeStartHour))
            .putInt(K_BED_END, LauncherSettings.clampHour(rules.bedtimeEndHour))
            .apply()
        refresh(c)
    }

    /** Called when Kids mode turns on or off (and at start-up), so ticking follows it. */
    fun onKidsModeChanged(c: Context, enabled: Boolean) {
        prefs(c).edit().putBoolean(K_ENABLED, enabled).apply()
        if (enabled) schedule(c) else cancel(c)
        refresh(c)
    }

    /** A parent's PIN-approved extra time (both bedtime and the daily limit). */
    fun extend(c: Context, minutes: Int) {
        prefs(c).edit().putLong(K_OVERRIDE, System.currentTimeMillis() + minutes * 60_000L).apply()
        refresh(c)
    }

    /** Recomputes the lock from stored usage without adding time. */
    fun refresh(c: Context): Status {
        val p = prefs(c)
        val today = today()
        val used = if (p.getString(K_DAY, null) == today) p.getLong(K_USED, 0L) else 0L
        val rules = rules(c)
        val lock = if (p.getBoolean(K_ENABLED, false)) {
            KidsTime.lock(used, rules, hourNow(), System.currentTimeMillis(), p.getLong(K_OVERRIDE, 0L))
        } else null
        return Status(rules, used, lock).also { _status.value = it }
    }

    /** One tick: count time if the TV is in use, then enforce. Runs in the alarm receiver. */
    internal suspend fun tick(c: Context) {
        val p = prefs(c)
        if (!p.getBoolean(K_ENABLED, false)) return
        val inUse = c.getSystemService(PowerManager::class.java)?.isInteractive == true
        val before = KidsTime.Usage(p.getString(K_DAY, "") ?: "", p.getLong(K_USED, 0L), p.getLong(K_LAST, 0L))
        val after = KidsTime.tick(before, today(), SystemClock.elapsedRealtime(), inUse)
        p.edit().putString(K_DAY, after.day).putLong(K_USED, after.usedMs).putLong(K_LAST, after.lastTickElapsed).apply()
        val status = refresh(c)
        if (status.lock != null && inUse) bringForward(c)
    }

    /** Shows Onyx TV (and its "Time's up" screen) over whatever app is playing. */
    private suspend fun bringForward(c: Context) {
        if (MainActivity.isInFront) return
        val launched = runCatching {
            c.startActivity(
                Intent(c, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        }.isSuccess
        // Newer Fire OS can block starting an activity from the background; pressing Home works
        // then (Home is set to open Onyx TV), through the TV's own ADB if it's on.
        kotlinx.coroutines.delay(1500)
        if (!launched || !MainActivity.isInFront) SelfAdb.run(c, SelfAdb.Command.HOME, approvalWaitMs = 3_000)
    }

    fun schedule(c: Context) {
        val alarms = c.getSystemService(AlarmManager::class.java) ?: return
        alarms.setWindow(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + TICK_MS,
            WINDOW_MS,
            pending(c)
        )
    }

    private fun cancel(c: Context) {
        c.getSystemService(AlarmManager::class.java)?.cancel(pending(c))
    }

    private fun pending(c: Context): PendingIntent = PendingIntent.getBroadcast(
        c, 1, Intent(c, KidsTimeReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun hourNow(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    /** After a restart alarms are gone; start ticking again if Kids mode is on. */
    fun resumeAfterBoot(c: Context) {
        prefs(c).edit().putLong(K_LAST, 0L).apply()
        if (prefs(c).getBoolean(K_ENABLED, false)) schedule(c)
    }

    internal fun isEnabled(c: Context) = prefs(c).getBoolean(K_ENABLED, false)
}

/** The Kids mode tick. Not exported: only our own alarm starts it. */
class KidsTimeReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        scope.launch {
            try {
                KidsTimeKeeper.tick(app)
            } finally {
                // Keep ticking while Kids mode is on
                if (KidsTimeKeeper.isEnabled(app)) KidsTimeKeeper.schedule(app)
                pending.finish()
            }
        }
    }
}
