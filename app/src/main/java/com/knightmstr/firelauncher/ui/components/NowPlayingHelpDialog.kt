package com.knightmstr.firelauncher.ui.components

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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.knightmstr.firelauncher.media.NowPlayingRepository
import com.knightmstr.firelauncher.ui.theme.DarkSurface
import com.knightmstr.firelauncher.ui.theme.FireAmber
import com.knightmstr.firelauncher.ui.theme.TextPrimary
import com.knightmstr.firelauncher.ui.theme.TextSecondary

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

    TvDialogFrame(
        title = "Turn on Now playing",
        subtitle = "One-time permission to see what other apps are playing",
        accentColor = FireAmber,
        onDismiss = onDismiss,
        width = 560.dp
    ) {
        TvMenuItem(
            title = "Open notification access settings",
            subtitle = if (settingsMissing) "This Fire TV doesn't have that screen — use the command below"
            else "Then switch FireLauncher on",
            icon = Icons.AutoMirrored.Rounded.OpenInNew,
            iconTint = FireAmber,
            modifier = Modifier.focusRequester(first),
            onClick = { if (onOpenSystemSettings()) onDismiss() else settingsMissing = true }
        )
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
            text = "FireLauncher never reads your notifications; Android just requires this permission to see media sessions.",
            fontSize = 11.sp,
            color = Color(0xFF9CA3AF)
        )
    }
}
