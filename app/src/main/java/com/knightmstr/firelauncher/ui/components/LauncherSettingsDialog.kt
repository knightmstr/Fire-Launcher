package com.knightmstr.firelauncher.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.knightmstr.firelauncher.data.LauncherSettings
import com.knightmstr.firelauncher.data.LocalPhotoSource
import com.knightmstr.firelauncher.data.WallpaperSource
import com.knightmstr.firelauncher.ui.theme.FireAmber
import com.knightmstr.firelauncher.ui.theme.FireOrange
import com.knightmstr.firelauncher.ui.theme.TextSecondary

/** Read-only facts the settings screen displays next to each option. */
data class SettingsStatus(
    val isCelsius: Boolean,
    val hiddenAppCount: Int,
    val localPhotoCount: Int,
    val hasPhotoPermission: Boolean,
    val hasKidsPin: Boolean,
    val kidsAppCount: Int,
    val hasNowPlayingAccess: Boolean,
    val installedVersion: String,
    val updateSummary: String
)

/**
 * In-launcher settings. Every row cycles or toggles with OK, so nothing needs a keyboard.
 * Only reachable outside Kids mode (or after entering the PIN).
 */
@Composable
fun LauncherSettingsDialog(
    settings: LauncherSettings,
    status: SettingsStatus,
    onUpdate: ((LauncherSettings) -> LauncherSettings) -> Unit,
    onToggleUnit: () -> Unit,
    onRequestPhotoPermission: () -> Unit,
    onEnableKidsMode: () -> Unit,
    onChangeKidsPin: () -> Unit,
    onOpenNowPlayingHelp: () -> Unit,
    onOpenUpdates: () -> Unit,
    onDismiss: () -> Unit
) {
    val firstItem = rememberInitialFocus()
    val usingPhotos = settings.wallpaperSource == WallpaperSource.MY_PHOTOS

    TvDialogFrame(
        title = "Launcher Settings",
        subtitle = "Press OK to change a value • Back to close",
        accentColor = Color(0xFF10B981),
        onDismiss = onDismiss,
        width = 540.dp
    ) {
        TvMenuItem(
            title = "Wallpapers from",
            subtitle = when {
                !usingPhotos -> "Downloads new wallpapers from Reddit"
                status.localPhotoCount > 0 -> "${status.localPhotoCount} photos found"
                else -> "None found: add a \"${LocalPhotoSource.ALBUM_NAME}\" album (using Reddit)"
            },
            icon = Icons.Rounded.PhotoLibrary,
            iconTint = Color(0xFF34D399),
            trailing = settings.wallpaperSource.title,
            modifier = Modifier.focusRequester(firstItem),
            onClick = {
                onUpdate {
                    it.copy(
                        wallpaperSource = if (it.wallpaperSource == WallpaperSource.MY_PHOTOS) WallpaperSource.REDDIT
                        else WallpaperSource.MY_PHOTOS
                    )
                }
            }
        )

        if (usingPhotos && !status.hasPhotoPermission) {
            TvMenuItem(
                title = "Allow photo access",
                subtitle = "Needed to read the \"${LocalPhotoSource.ALBUM_NAME}\" album",
                icon = Icons.Rounded.Lock,
                iconTint = Color(0xFF34D399),
                onClick = onRequestPhotoPermission
            )
        }

        TvMenuItem(
            title = "Show All Apps row on home",
            subtitle = "Off: open the All Apps tile for a full-screen grid",
            icon = Icons.Rounded.Apps,
            iconTint = FireAmber,
            trailing = if (settings.showAllAppsRow) "On" else "Off",
            onClick = { onUpdate { it.copy(showAllAppsRow = !it.showAllAppsRow) } }
        )

        TvMenuItem(
            title = "App card size",
            subtitle = "Smaller cards fit more apps on screen",
            icon = Icons.Rounded.AspectRatio,
            iconTint = Color(0xFF60A5FA),
            trailing = settings.cardSize.title,
            onClick = { onUpdate { it.copy(cardSize = it.cardSize.next()) } }
        )

        TvMenuItem(
            title = "Change wallpaper every",
            icon = Icons.Rounded.Wallpaper,
            iconTint = FireAmber,
            trailing = LauncherSettings.describeMinutes(settings.wallpaperIntervalMinutes, zeroLabel = "Never"),
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
            title = "Now playing controls",
            subtitle = if (status.hasNowPlayingAccess) "Shows what's playing with pause / skip"
            else "Needs a one-time permission",
            icon = Icons.Rounded.MusicNote,
            iconTint = FireAmber,
            trailing = if (status.hasNowPlayingAccess) "On" else "Set up",
            onClick = onOpenNowPlayingHelp
        )

        TvMenuItem(
            title = "Kids mode",
            subtitle = when (status.kidsAppCount) {
                0 -> "Pick apps first: hold OK on an app → Allow in Kids mode"
                1 -> "1 app allowed • PIN needed to leave"
                else -> "${status.kidsAppCount} apps allowed • PIN needed to leave"
            },
            icon = Icons.Rounded.ChildCare,
            iconTint = Color(0xFFF472B6),
            trailing = "Turn on",
            enabled = status.kidsAppCount > 0,
            onClick = onEnableKidsMode
        )

        if (status.hasKidsPin) {
            TvMenuItem(
                title = "Change Kids mode PIN",
                icon = Icons.Rounded.Lock,
                iconTint = Color(0xFFF472B6),
                onClick = onChangeKidsPin
            )
        }

        TvMenuItem(
            title = "App updates",
            subtitle = status.updateSummary,
            icon = Icons.Rounded.SystemUpdate,
            iconTint = Color(0xFF38BDF8),
            trailing = "v${status.installedVersion}",
            onClick = onOpenUpdates
        )

        TvMenuItem(
            title = "Temperature unit",
            icon = Icons.Rounded.Thermostat,
            iconTint = FireOrange,
            trailing = if (status.isCelsius) "°C" else "°F",
            onClick = onToggleUnit
        )

        TvMenuItem(
            title = "Show hidden apps row",
            subtitle = when (status.hiddenAppCount) {
                0 -> "No apps hidden"
                1 -> "1 app hidden"
                else -> "${status.hiddenAppCount} apps hidden"
            },
            icon = Icons.Rounded.Visibility,
            iconTint = TextSecondary,
            trailing = if (settings.showHiddenApps) "On" else "Off",
            onClick = { onUpdate { it.copy(showHiddenApps = !it.showHiddenApps) } }
        )
    }
}
