package com.knightmstr.onyxtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.knightmstr.onyxtv.home.Platform
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.knightmstr.onyxtv.media.NowPlayingRepository
import com.knightmstr.onyxtv.ui.theme.DarkSurface
import com.knightmstr.onyxtv.ui.theme.FireAmber
import com.knightmstr.onyxtv.ui.theme.TextPrimary
import com.knightmstr.onyxtv.ui.theme.TextSecondary

/**
 * Explains the one-time "notification access" permission needed for Now playing.
 * Many Fire OS builds hide that settings screen, so the adb command is always shown too.
 */
@Composable
fun NowPlayingHelpDialog(
    onOpenSystemSettings: () -> Boolean,
    onDismiss: () -> Unit
) {
    var settingsMissing by remember { mutableStateOf(false) }
    val first = rememberInitialFocus()
    val context = LocalContext.current
    val names = Platform.names(context)
    val restricted = Platform.needsRestrictedSettingsStep(context)

    TvDialogFrame(
        title = "Turn on Now playing",
        subtitle = "One-time permission to see what other apps are playing",
        accentColor = FireAmber,
        onDismiss = onDismiss,
        width = 560.dp
    ) {
        TvMenuItem(
            title = "Open notification access settings",
            subtitle = if (settingsMissing) "This ${names.device} doesn't have that screen — use the command below"
            else "Then switch Onyx TV on",
            icon = Icons.AutoMirrored.Rounded.OpenInNew,
            iconTint = FireAmber,
            modifier = Modifier.focusRequester(first),
            onClick = { if (onOpenSystemSettings()) onDismiss() else settingsMissing = true }
        )
        if (restricted) {
            Text(
                text = "Switch greyed out? Android blocks this for apps installed outside the Play Store until you " +
                    "allow it: Settings → Apps → Onyx TV → ⋮ (or Options) → Allow restricted settings. Then try again.",
                fontSize = 12.sp,
                color = Color(0xFFFBBF24)
            )
        }
        Text(
            text = "Or run this once from a computer on the same Wi-Fi (adb connect <TV IP> first):",
            fontSize = 12.sp,
            color = TextSecondary
        )
        Text(
            text = NowPlayingRepository.ADB_GRANT_COMMAND,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(8.dp))
                .padding(10.dp)
        )
        Text(
            text = "Onyx TV never reads your notifications; Android just requires this permission to see media sessions.",
            fontSize = 11.sp,
            color = Color(0xFF9CA3AF)
        )
    }
}
