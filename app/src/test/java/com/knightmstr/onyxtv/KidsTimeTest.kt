package com.knightmstr.onyxtv

import com.knightmstr.onyxtv.kids.KidsTime
import com.knightmstr.onyxtv.kids.KidsTime.Lock
import com.knightmstr.onyxtv.kids.KidsTime.Rules
import com.knightmstr.onyxtv.kids.KidsTime.Usage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KidsTimeTest {

    private val min = 60_000L

    @Test
    fun `ticks add time only while the TV is in use`() {
        var u = Usage("2026-11-28", 0, 0)
        u = KidsTime.tick(u, "2026-11-28", 1_000_000, inUse = true) // first tick: nothing to add yet
        assertEquals(0L, u.usedMs)
        u = KidsTime.tick(u, "2026-11-28", 1_000_000 + min, inUse = true)
        assertEquals(min, u.usedMs)
        u = KidsTime.tick(u, "2026-11-28", 1_000_000 + 2 * min, inUse = false)
        assertEquals(min, u.usedMs)
    }

    @Test
    fun `a long gap (TV asleep) never counts as hours`() {
        var u = Usage("2026-11-28", 10 * min, 1_000)
        u = KidsTime.tick(u, "2026-11-28", 1_000 + 5 * 60 * min, inUse = true)
        assertEquals(10 * min + KidsTime.MAX_TICK_MS, u.usedMs)
    }

    @Test
    fun `a new day starts from zero, and a clock reset doesn't count`() {
        var u = Usage("2026-11-27", 90 * min, 5_000)
        u = KidsTime.tick(u, "2026-11-28", 6_000, inUse = true)
        assertEquals(0L, u.usedMs)
        u = Usage("2026-11-28", 5 * min, 9_000_000)
        u = KidsTime.tick(u, "2026-11-28", 1_000, inUse = true) // elapsed went backwards (restart)
        assertEquals(5 * min, u.usedMs)
    }

    @Test
    fun `daily limit locks when used up`() {
        val r = Rules(60, false, 20, 7)
        assertNull(KidsTime.lock(59 * min, r, 15, 0, 0))
        assertEquals(Lock.TIME_UP, KidsTime.lock(60 * min, r, 15, 0, 0))
        assertNull(KidsTime.lock(600 * min, Rules(0, false, 20, 7), 15, 0, 0))
    }

    @Test
    fun `bedtime crosses midnight and wins over time left`() {
        val r = Rules(120, true, 20, 7)
        assertEquals(Lock.BEDTIME, KidsTime.lock(0, r, 21, 0, 0))
        assertEquals(Lock.BEDTIME, KidsTime.lock(0, r, 3, 0, 0))
        assertNull(KidsTime.lock(0, r, 7, 0, 0))
        assertNull(KidsTime.lock(0, r, 19, 0, 0))
    }

    @Test
    fun `a parent's extra time unlocks both until it runs out`() {
        val r = Rules(60, true, 20, 7)
        assertNull(KidsTime.lock(90 * min, r, 21, nowWall = 1_000, overrideUntilWall = 2_000))
        assertEquals(Lock.BEDTIME, KidsTime.lock(90 * min, r, 21, nowWall = 2_000, overrideUntilWall = 2_000))
    }

    @Test
    fun `minutes left and limit options`() {
        assertEquals(15, KidsTime.minutesLeft(45 * min, 60))
        assertEquals(0, KidsTime.minutesLeft(90 * min, 60))
        assertNull(KidsTime.minutesLeft(0, 0))
        assertEquals(30, KidsTime.nextLimit(0))
        assertEquals(0, KidsTime.nextLimit(240))
    }
}
