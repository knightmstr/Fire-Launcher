package com.knightmstr.onyxtv.kids

import com.knightmstr.onyxtv.data.LauncherSettings

/**
 * Pure Kids mode screen-time rules (no Android), so they're unit-tested on the JVM.
 *
 * Screen time is counted while the TV is awake with Kids mode on, in small ticks; a long gap
 * between ticks (the TV was asleep or off) counts only as one short tick, never as hours.
 */
object KidsTime {

    /** Daily limit choices in minutes; 0 = no limit. */
    val LIMIT_OPTIONS = listOf(0, 30, 60, 90, 120, 180, 240)

    /** The most a single tick may add, whatever the gap since the last one. */
    const val MAX_TICK_MS = 2 * 60_000L

    data class Usage(
        /** yyyy-MM-dd the count belongs to; a new day starts again from zero. */
        val day: String,
        val usedMs: Long,
        /** SystemClock.elapsedRealtime() of the last tick, 0 if none yet (or after a restart). */
        val lastTickElapsed: Long
    )

    enum class Lock { TIME_UP, BEDTIME }

    data class Rules(
        val dailyLimitMinutes: Int,
        val bedtimeOn: Boolean,
        val bedtimeStartHour: Int,
        val bedtimeEndHour: Int
    )

    /** Adds the time since the last tick if the TV is in use; resets on a new day. */
    fun tick(u: Usage, today: String, nowElapsed: Long, inUse: Boolean): Usage {
        val base = if (u.day == today) u else Usage(today, 0L, 0L)
        val gap = if (base.lastTickElapsed in 1..nowElapsed) nowElapsed - base.lastTickElapsed else 0L
        val add = if (inUse) gap.coerceAtMost(MAX_TICK_MS) else 0L
        return base.copy(usedMs = base.usedMs + add, lastTickElapsed = nowElapsed)
    }

    /**
     * Why Kids mode is locked right now, or null. A parent's "+30 min" sets [overrideUntilWall],
     * which unlocks both reasons until then.
     */
    fun lock(usedMs: Long, rules: Rules, hourOfDay: Int, nowWall: Long, overrideUntilWall: Long): Lock? {
        if (nowWall < overrideUntilWall) return null
        if (rules.bedtimeOn && LauncherSettings.isNight(hourOfDay, rules.bedtimeStartHour, rules.bedtimeEndHour)) {
            return Lock.BEDTIME
        }
        if (rules.dailyLimitMinutes > 0 && usedMs >= rules.dailyLimitMinutes * 60_000L) return Lock.TIME_UP
        return null
    }

    /** Minutes left today, or null with no limit. */
    fun minutesLeft(usedMs: Long, limitMinutes: Int): Int? =
        if (limitMinutes <= 0) null else ((limitMinutes * 60_000L - usedMs).coerceAtLeast(0) / 60_000L).toInt()

    fun nextLimit(current: Int): Int {
        val i = LIMIT_OPTIONS.indexOf(current)
        return LIMIT_OPTIONS[(i + 1) % LIMIT_OPTIONS.size]
    }
}
