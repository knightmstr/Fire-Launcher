package com.knightmstr.firelauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
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
import com.knightmstr.firelauncher.home.HomeWatch
import com.knightmstr.firelauncher.ui.theme.DarkSurface
import com.knightmstr.firelauncher.ui.theme.FireAmber
import com.knightmstr.firelauncher.ui.theme.TextPrimary
import com.knightmstr.firelauncher.ui.theme.TextSecondary

/** One-time setup for the Home button watcher: an ADB grant, then FireLauncher switches it on. */
@Composable
fun HomeWatchHelpDialog(
    onEnable: () -> Boolean,
    onDismiss: () -> Unit
) {
    var stillMissing by remember { mutableStateOf(false) }
    val first = rememberInitialFocus()

    TvDialogFrame(
        title = "Make Home open FireLauncher",
        subtitle = "One-time setup from your phone (Termux) or a computer",
        accentColor = FireAmber,
        onDismiss = onDismiss,
        width = 580.dp
    ) {
        Text(
            text = "1. Connect with adb (adb connect <TV IP>:5555), then run this once:",
            fontSize = 12.sp,
            color = TextSecondary
        )
        Text(
            text = HomeWatch.GRANT_COMMAND,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(8.dp))
                .padding(10.dp)
        )
        Text(text = "2. Then press Turn on:", fontSize = 12.sp, color = TextSecondary)
        TvMenuItem(
            title = "Turn on",
            subtitle = if (stillMissing) "Not granted yet: run the command above first"
            else "FireLauncher then keeps it on after updates and restarts",
            icon = Icons.Rounded.Home,
            iconTint = FireAmber,
            modifier = Modifier.focusRequester(first),
            onClick = { stillMissing = !onEnable() }
        )
        Text(
            text = "Amazon's home may flash for a moment before FireLauncher comes back. " +
                "FireLauncher only sees which Amazon screen opened, never what's on screen.",
            fontSize = 11.sp,
            color = Color(0xFF9CA3AF)
        )
    }
}
