package com.knightmstr.onyxtv

import com.knightmstr.onyxtv.ui.ClockSpot
import com.knightmstr.onyxtv.ui.ClockSpot.Spot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class ClockSpotTest {

    private val w = 64
    private val h = 36

    /** A calm dark image with a busy, bright patch over the given spot(s). */
    private fun image(vararg busy: Spot, seed: Int = 1): IntArray {
        val r = Random(seed)
        val px = IntArray(w * h) { 0xFF101820.toInt() }
        for (s in busy) {
            val cx = ((0.5f + s.horizontalBias * 0.3f) * w).toInt()
            val cy = ((0.5f + s.verticalBias * 0.3f) * h).toInt()
            for (y in (cy - 9).coerceAtLeast(0) until (cy + 9).coerceAtMost(h)) for (x in (cx - 14).coerceAtLeast(0) until (cx + 14).coerceAtMost(w)) {
                val v = r.nextInt(256)
                px[y * w + x] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
            }
        }
        return px
    }

    @Test
    fun `busy centre sends the clock to a calm corner`() {
        val spot = ClockSpot.choose(image(Spot.CENTER), w, h, Spot.CENTER)
        assertTrue(spot != Spot.CENTER)
    }

    @Test
    fun `picks the one calm corner`() {
        val px = image(Spot.CENTER, Spot.TOP_LEFT, Spot.TOP_RIGHT, Spot.BOTTOM_LEFT)
        assertEquals(Spot.BOTTOM_RIGHT, ClockSpot.choose(px, w, h, Spot.CENTER))
    }

    @Test
    fun `stays put when nowhere is clearly calmer`() {
        val flat = IntArray(w * h) { 0xFF202020.toInt() }
        assertEquals(Spot.TOP_LEFT, ClockSpot.choose(flat, w, h, Spot.TOP_LEFT))
        assertEquals(Spot.CENTER, ClockSpot.choose(flat, w, h, Spot.CENTER))
    }

    @Test
    fun `bright areas count against a spot`() {
        val px = IntArray(w * h) { 0xFF202020.toInt() }
        // whole left half bright white, right half dark: the clock (white) should go right
        for (y in 0 until h) for (x in 0 until w / 2) px[y * w + x] = 0xFFF5F5F5.toInt()
        val spot = ClockSpot.choose(px, w, h, Spot.TOP_LEFT)
        assertTrue(spot == Spot.TOP_RIGHT || spot == Spot.BOTTOM_RIGHT)
    }

    @Test
    fun `bad input keeps the current spot`() {
        assertEquals(Spot.CENTER, ClockSpot.choose(IntArray(3), w, h, Spot.CENTER))
        assertEquals(Spot.TOP_RIGHT, ClockSpot.choose(IntArray(0), 0, 0, Spot.TOP_RIGHT))
    }

    @Test
    fun `drift is small, steady within a period and changes between periods`() {
        val seen = HashSet<Pair<Int, Int>>()
        for (m in 0L until 7L * 200) {
            val (x, y) = ClockSpot.driftFor(m)
            assertTrue(abs(x) <= ClockSpot.DRIFT_X_DP && abs(y) <= ClockSpot.DRIFT_Y_DP)
            if (m % ClockSpot.DRIFT_PERIOD_MIN != 0L) assertEquals(ClockSpot.driftFor(m - 1), x to y)
            seen += x to y
        }
        assertTrue("drift should move around", seen.size > 50)
    }

    @Test
    fun `luma`() {
        assertTrue(abs(ClockSpot.luma(0xFF000000.toInt()) - 0f) < 0.001f)
        assertTrue(abs(ClockSpot.luma(0xFFFFFFFF.toInt()) - 1f) < 0.001f)
    }
}
