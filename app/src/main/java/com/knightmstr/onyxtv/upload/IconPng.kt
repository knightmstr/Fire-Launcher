package com.knightmstr.onyxtv.upload

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import java.io.ByteArrayOutputStream

/** Renders an app icon to a small PNG for the phone-setup page. */
object IconPng {

    private const val SIZE_PX = 96

    fun render(drawable: Drawable): ByteArray? = try {
        val bitmap = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        // Draw a copy so changing bounds can't disturb the icon the TV is showing.
        val copy = drawable.constantState?.newDrawable()?.mutate() ?: drawable
        copy.setBounds(0, 0, SIZE_PX, SIZE_PX)
        copy.draw(Canvas(bitmap))
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            bitmap.recycle()
            out.toByteArray()
        }
    } catch (e: Exception) {
        null
    }
}
