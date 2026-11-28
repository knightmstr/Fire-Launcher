package com.knightmstr.onyxtv.ui

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Where the home clock goes, and its slow anti-burn-in drift. Pure Kotlin, unit-tested.
 *
 * Smart placement looks at a small copy of the wallpaper and scores five spots (centre and the
 * four corners) for how calm they are: few edges and little contrast (busy detail makes white
 * text hard to read), and not too bright (the clock is white). It only moves when another spot
 * is clearly calmer, so a similar wallpaper doesn't send the clock bouncing around.
 */
object ClockSpot {

    enum class Spot(val horizontalBias: Float, val verticalBias: Float) {
        CENTER(0f, 0f),
        TOP_LEFT(-0.85f, -0.8f),
        TOP_RIGHT(0.85f, -0.8f),
        BOTTOM_LEFT(-0.85f, 0.8f),
        BOTTOM_RIGHT(0.85f, 0.8f)
    }

    /** A spot must be this much calmer than the current one before the clock moves. */
    const val MOVE_MARGIN = 0.15f

    /**
     * Busyness of the part of the image a spot covers (lower is calmer).
     * [pixels] are ARGB, [w] x [h], row by row.
     */
    fun score(pixels: IntArray, w: Int, h: Int, spot: Spot): Float {
        // The clock block covers roughly 40% x 40% of the screen around its spot
        val cx = (0.5f + spot.horizontalBias * 0.3f) * w
        val cy = (0.5f + spot.verticalBias * 0.3f) * h
        val x0 = (cx - w * 0.2f).toInt().coerceIn(0, w - 1)
        val x1 = (cx + w * 0.2f).toInt().coerceIn(x0 + 1, w)
        val y0 = (cy - h * 0.2f).toInt().coerceIn(0, h - 1)
        val y1 = (cy + h * 0.2f).toInt().coerceIn(y0 + 1, h)

        var sum = 0f
        var sumSq = 0f
        var edges = 0f
        var n = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                val l = luma(pixels[y * w + x])
                sum += l
                sumSq += l * l
                if (x + 1 < x1) edges += abs(l - luma(pixels[y * w + x + 1]))
                if (y + 1 < y1) edges += abs(l - luma(pixels[(y + 1) * w + x]))
                n++
            }
        }
        if (n == 0) return Float.MAX_VALUE
        val mean = sum / n
        val std = sqrt((sumSq / n - mean * mean).coerceAtLeast(0f))
        val edgeDensity = edges / n
        val brightness = (mean - 0.55f).coerceAtLeast(0f)
        return edgeDensity * 2.0f + std * 1.0f + brightness * 1.5f
    }

    /** The calmest spot, staying at [current] unless another is clearly better. */
    fun choose(pixels: IntArray, w: Int, h: Int, current: Spot = Spot.CENTER): Spot {
        if (w < 4 || h < 4 || pixels.size < w * h) return current
        val scores = Spot.entries.associateWith { score(pixels, w, h, it) }
        val best = scores.minBy { it.value }
        val now = scores.getValue(current)
        return if (best.value < now * (1f - MOVE_MARGIN)) best.key else current
    }

    /** 0..1 perceived brightness. */
    fun luma(argb: Int): Float {
        val r = (argb shr 16) and 0xff
        val g = (argb shr 8) and 0xff
        val b = argb and 0xff
        return (0.299f * r + 0.587f * g + 0.114f * b) / 255f
    }

    // ---------------------------------------------------------------- drift

    /** Largest drift from the clock's spot, in dp. Small enough not to notice. */
    const val DRIFT_X_DP = 14
    const val DRIFT_Y_DP = 10

    /** A new drift target every this many minutes; the move itself takes a minute. */
    const val DRIFT_PERIOD_MIN = 7

    /** Drift offset (dp) for the period containing [minuteOfEpoch]: the same all period, new next. */
    fun driftFor(minuteOfEpoch: Long): Pair<Int, Int> {
        val r = Random(minuteOfEpoch / DRIFT_PERIOD_MIN)
        return r.nextInt(-DRIFT_X_DP, DRIFT_X_DP + 1) to r.nextInt(-DRIFT_Y_DP, DRIFT_Y_DP + 1)
    }
}
