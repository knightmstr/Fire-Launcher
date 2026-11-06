package com.example.firelauncher.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.palette.graphics.Palette

/**
 * Utility for extracting and clamping accent colors from app banners or icons.
 */
object PaletteHelper {

    private const val DEFAULT_ACCENT_COLOR = 0xFFFF9900.toInt() // Fire OS warm orange

    /**
     * Extracts a dominant or vibrant accent color from the provided drawable,
     * and clamps HSV saturation and brightness so it is never washed-out or near-black.
     */
    fun extractClampedAccentColor(drawable: Drawable?): Int {
        if (drawable == null) return DEFAULT_ACCENT_COLOR
        val bitmap = drawableToBitmap(drawable) ?: return DEFAULT_ACCENT_COLOR

        return try {
            val palette = Palette.from(bitmap)
                .maximumColorCount(16)
                .generate()

            val rawColor = palette.vibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: palette.lightVibrantSwatch?.rgb
                ?: palette.darkVibrantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb
                ?: DEFAULT_ACCENT_COLOR

            clampColor(rawColor)
        } catch (_: Exception) {
            DEFAULT_ACCENT_COLOR
        }
    }

    /**
     * Clamps the HSV saturation and value (brightness) so the color remains vibrant,
     * vivid, and clearly visible on dark TV backgrounds without turning washed-out or near-black.
     */
    fun clampColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)

        // Saturation clamped between 0.55 and 0.95 (prevents dull washed-out pastel/white/grey)
        hsv[1] = hsv[1].coerceIn(0.55f, 0.95f)

        // Value (brightness) clamped between 0.50 and 0.88 (prevents dark near-black or blinding white)
        hsv[2] = hsv[2].coerceIn(0.50f, 0.88f)

        return Color.HSVToColor(hsv)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.coerceAtMost(160) else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.coerceAtMost(90) else 96

        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }
}
