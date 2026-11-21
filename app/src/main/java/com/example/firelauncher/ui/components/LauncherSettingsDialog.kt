package com.example.firelauncher.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import com.example.firelauncher.data.LauncherSettings
import com.example.firelauncher.ui.theme.FireAmber
import com.example.firelauncher.ui.theme.FireOrange
import com.example.firelauncher.ui.theme.TextSecondary

/**
 * In-launcher settings. Every row cycles through its options with OK, so nothing needs a keyboard.
 */
@Composable
fun LauncherSettingsDialog(
    settings: LauncherSettings,
    isCelsius: Boolean,
    hiddenAppCount: Int,
    onUpdate: ((LauncherSettings) -> LauncherSettings) -> Unit,
    onToggleUnit: () -> Unit,
    onDismiss: () -> Unit
) {
    val firstItem = rememberInitialFocus()

    TvDialogFrame(
        title = "Launcher Settings",
        subtitle = "Press OK to change a value • Back to close",
        accentColor = Color(0xFF10B981),
        onDismiss = onDismiss
    ) {
        TvMenuItem(
            title = "Change wallpaper every",
            icon = Icons.Rounded.Wallpaper,
            iconTint = FireAmber,
            trailing = LauncherSettings.describeMinutes(settings.wallpaperIntervalMinutes, zeroLabel = "Never"),
            modifier = Modifier.focusRequester(firstItem),
            onClick = {
                onUpdate {
                    it.copy(
                        wallpaperIntervalMinutes = LauncherSettings.nextOption(
                            LauncherSettings.WALLPAPER_INTERVAL_OPTIONS, it.wallpaperIntervalMinutes
                        )
                    )
                }
            }
        )

        TvMenuItem(
            title = "Screensaver after",
            subtitle = "Clock and wallpaper when the remote is idle",
            icon = Icons.Rounded.Bedtime,
            iconTint = Color(0xFFA78BFA),
            trailing = LauncherSettings.describeMinutes(settings.screensaverTimeoutMinutes),
            onClick = {
                onUpdate {
                    it.copy(
                        screensaverTimeoutMinutes = LauncherSettings.nextOption(
                            LauncherSettings.SCREENSAVER_TIMEOUT_OPTIONS, it.screensaverTimeoutMinutes
                        )
                    )
                }
            }
        )

        TvMenuItem(
            title = "Temperature unit",
            icon = Icons.Rounded.Thermostat,
            iconTint = FireOrange,
            trailing = if (isCelsius) "°C" else "°F",
            onClick = onToggleUnit
        )

        TvMenuItem(
            title = "Show hidden apps row",
            subtitle = when (hiddenAppCount) {
                0 -> "No apps hidden"
                1 -> "1 app hidden"
                else -> "$hiddenAppCount apps hidden"
            },
            icon = Icons.Rounded.Visibility,
            iconTint = TextSecondary,
            trailing = if (settings.showHiddenApps) "On" else "Off",
            onClick = { onUpdate { it.copy(showHiddenApps = !it.showHiddenApps) } }
        )
    }
}
