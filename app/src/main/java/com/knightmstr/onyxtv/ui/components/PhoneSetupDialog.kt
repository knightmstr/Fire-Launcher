package com.knightmstr.onyxtv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.knightmstr.onyxtv.ui.PhoneSetupUi
import com.knightmstr.onyxtv.upload.QrCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.knightmstr.onyxtv.ui.theme.DarkSurface
import com.knightmstr.onyxtv.ui.theme.TextPrimary
import com.knightmstr.onyxtv.ui.theme.TextSecondary

/**
 * "Set up from phone": shows the Wi-Fi address (and QR code) to open on a phone or computer to
 * manage apps and upload wallpaper photos, plus a one-tap "clear all" for uploaded photos.
 * The server runs only while this is open.
 */
@Composable
fun PhoneSetupDialog(
    ui: PhoneSetupUi,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val first = rememberInitialFocus()

    TvDialogFrame(
        title = "Set up from phone",
        subtitle = "Favourites, hide, rename and photos — from a phone on the same Wi-Fi",
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
                val url = ui.url
                val qr by produceState<ImageBitmap?>(initialValue = null, url) {
                    value = withContext(Dispatchers.Default) { QrCode.generate(url, 480) }
                }
                qr?.let { bitmap ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "QR code for the phone setup address",
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .size(200.dp)
                        )
                    }
                }
                Text(
                    text = "Scan this with your phone's camera — or open the address below:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = url,
                    fontSize = 16.sp,
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
                text = "Starting phone setup…",
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
