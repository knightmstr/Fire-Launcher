package com.knightmstr.onyxtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.knightmstr.onyxtv.ui.PhotoUploadUi
import com.knightmstr.onyxtv.ui.theme.DarkSurface
import com.knightmstr.onyxtv.ui.theme.TextPrimary
import com.knightmstr.onyxtv.ui.theme.TextSecondary

/**
 * Shows the Wi-Fi address to open on a phone or computer to upload wallpaper photos, plus a
 * one-tap "clear all" for the photos uploaded this way. The server runs only while this is open.
 */
@Composable
fun PhotoUploadDialog(
    ui: PhotoUploadUi,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val first = rememberInitialFocus()

    TvDialogFrame(
        title = "Upload photos over Wi-Fi",
        subtitle = "No cable needed — open the address below on a phone on the same Wi-Fi",
        accentColor = Color(0xFF10B981),
        onDismiss = onDismiss,
        width = 560.dp
    ) {
        when {
            ui.error != null -> Text(
                text = ui.error,
                fontSize = 13.sp,
                color = Color(0xFFFBBF24)
            )

            ui.running && ui.url != null -> {
                Text(
                    text = "On your phone's browser, go to:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = ui.url,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
                Text(
                    text = "Phone and TV must be on the same Wi-Fi. The address works only while this screen is open.",
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF)
                )
            }

            else -> Text(
                text = "Starting the uploader…",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        Text(
            text = if (ui.count > 0) "${ui.count} uploaded photo${if (ui.count == 1) "" else "s"} in use"
            else "No uploaded photos yet",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (ui.count > 0) {
            TvMenuItem(
                title = "Clear all uploaded photos",
                subtitle = "Removes only photos uploaded here, not your gallery",
                icon = Icons.Rounded.DeleteSweep,
                iconTint = Color(0xFFF87171),
                modifier = Modifier.focusRequester(first),
                onClick = onClearAll
            )
        }
    }
}
