package com.knightmstr.firelauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.knightmstr.firelauncher.home.HomeWatch
import com.knightmstr.firelauncher.home.SelfAdb
import com.knightmstr.firelauncher.ui.theme.DarkSurface
import com.knightmstr.firelauncher.ui.theme.FireAmber
import com.knightmstr.firelauncher.ui.theme.TextPrimary
import com.knightmstr.firelauncher.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * One-time setup shared by the Fire TV features that change system settings (Home button,
 * screensaver, keep awake). Easiest: set up on the TV itself (FireLauncher connects to the TV's own
 * ADB and you press Allow). Otherwise the same grant from a phone (Termux) or computer.
 */
@Composable
fun GrantSetupDialog(
    title: String,
    onEnable: () -> Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    var adbOff by remember { mutableStateOf(!SelfAdb.isAdbOn(context)) }
    var stillMissing by remember { mutableStateOf(false) }
    val first = rememberInitialFocus()

    TvDialogFrame(
        title = title,
        subtitle = "One-time setup. It also unlocks the other Fire TV options.",
        accentColor = FireAmber,
        onDismiss = onDismiss,
        width = 600.dp
    ) {
        TvMenuItem(
            title = if (working) "Waiting for Allow on the TV…" else "Set up on this TV",
            subtitle = status ?: if (adbOff) "Turn on ADB debugging first (below)"
            else "Then choose Always allow and OK when the TV asks",
            icon = Icons.Rounded.Tv,
            iconTint = FireAmber,
            modifier = Modifier.focusRequester(first),
            onClick = {
                if (working) return@TvMenuItem
                adbOff = !SelfAdb.isAdbOn(context)
                if (adbOff) {
                    status = "ADB debugging is off: open Developer options below"
                    return@TvMenuItem
                }
                working = true
                status = "Choose Always allow, then OK"
                scope.launch {
                    val result = SelfAdb.grantSelf(context)
                    working = false
                    status = when (result) {
                        SelfAdb.Result.Granted -> if (onEnable()) null else "Granted. Press Turn on below"
                        SelfAdb.Result.AdbOff -> "ADB debugging is off: open Developer options below"
                        SelfAdb.Result.NotAllowed -> "Not allowed. Try again and choose Always allow"
                        is SelfAdb.Result.Failed -> "Didn't work (${result.reason}). Use the phone method below"
                    }
                }
            }
        )
        TvMenuItem(
            title = "Open Developer options",
            subtitle = "Turn on ADB debugging. Hidden? Settings → My Fire TV → About, press OK on the device name 7 times",
            icon = Icons.Rounded.DeveloperMode,
            iconTint = Color(0xFF60A5FA),
            onClick = { SelfAdb.openDeveloperOptions(context) }
        )

        Text(
            text = "Or from a phone (Termux) or computer: adb connect <TV IP>:5555, then run",
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
        TvMenuItem(
            title = "Turn on",
            subtitle = if (stillMissing) "Not granted yet: run the command above first"
            else "After the phone method. FireLauncher keeps it set after updates and restarts",
            icon = Icons.Rounded.Home,
            iconTint = FireAmber,
            onClick = { stillMissing = !onEnable() }
        )
        Text(
            text = "This lets FireLauncher change the Home button, screensaver and sleep settings. " +
                "Turning an option off puts Amazon's setting back.",
            fontSize = 11.sp,
            color = Color(0xFF9CA3AF)
        )
    }
}
