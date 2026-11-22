package com.knightmstr.firelauncher.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.knightmstr.firelauncher.data.LauncherSettings
import com.knightmstr.firelauncher.home.HomeWatch
import com.knightmstr.firelauncher.screensaver.ScreensaverTakeover
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
    val updateSummary: String,
    val homeWatch: HomeWatch.Status,
    val screensaverTakeover: ScreensaverTakeover.Status,
    val keepAwake: Boolean,
    val needsRestart: Boolean
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
    onHomeWatch: () -> Unit,
    onScreensaverTakeover: () -> Unit,
    onKeepAwake: () -> Unit,
    onDismiss: () -> Unit
) {
    val firstItem = rememberInitialFocus()
    val usingPhotos = settings.wallpaperSource == WallpaperSource.MY_PHOTOS

    TvDialogFrame(
        title = "Launcher Settings",
        subtitle = "OK to change • Left/Right on sliders • Back to close",
        accentColor = Color(0xFF10B981),
        onDismiss = onDismiss,
        width = 540.dp
    ) {
        SettingsSection("Home screen", first = true)
        TvMenuItem(
            title = "Home layout",
            subtitle = settings.homeLayout.description,
            icon = Icons.Rounded.Dashboard,
            iconTint = Color(0xFF34D399),
            trailing = settings.homeLayout.title,
            modifier = Modifier.focusRequester(firstItem),
            onClick = { onUpdate { it.copy(homeLayout = it.homeLayout.next()) } }
        )
        TvSliderItem(
            title = "App tile size",
            subtitle = "Smaller tiles fit more apps on screen",
            icon = Icons.Rounded.AspectRatio,
            iconTint = Color(0xFF60A5FA),
            value = settings.tileWidthDp,
            range = LauncherSettings.TILE_WIDTH_RANGE,
            step = LauncherSettings.TILE_WIDTH_STEP,
            // Shown as a step number, e.g. "6 / 9"
            valueLabel = LauncherSettings.TILE_WIDTH_RANGE.let { r ->
                "${(settings.tileWidthDp - r.first) / LauncherSettings.TILE_WIDTH_STEP + 1} / " +
                    "${(r.last - r.first) / LauncherSettings.TILE_WIDTH_STEP + 1}"
            },
            onValueChange = { w -> onUpdate { it.copy(tileWidthDp = w) } }
        )
        TvMenuItem(
            title = "App tile shape",
            subtitle = "Rounded, square, extra rounded or pill",
            icon = Icons.Rounded.RoundedCorner,
            iconTint = Color(0xFF60A5FA),
            trailing = settings.cardShape.title,
            onClick = { onUpdate { it.copy(cardShape = it.cardShape.next()) } }
        )
        TvMenuItem(
            title = "Show All Apps row on home",
            subtitle = "Off: open the All Apps tile for a full-screen grid",
            icon = Icons.Rounded.Apps,
            iconTint = FireAmber,
            trailing = if (settings.showAllAppsRow) "On" else "Off",
            onClick = { onUpdate { it.copy(showAllAppsRow = !it.showAllAppsRow) } }
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
        TvMenuItem(
            title = "Temperature unit",
            icon = Icons.Rounded.Thermostat,
            iconTint = FireOrange,
            trailing = if (status.isCelsius) "°C" else "°F",
            onClick = onToggleUnit
        )

        SettingsSection("Wallpaper")
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

        SettingsSection("Screensaver")
        TvMenuItem(
            title = "Use as Fire TV screensaver",
            subtitle = when {
                status.screensaverTakeover == ScreensaverTakeover.Status.ON && status.needsRestart ->
                    "On: restart the Fire TV once to replace Amazon's"
                status.screensaverTakeover == ScreensaverTakeover.Status.ON ->
                    "Replaces Amazon's screensaver: same wallpaper, clock and weather"
                status.screensaverTakeover == ScreensaverTakeover.Status.OFF -> "Off: Amazon's screensaver"
                else -> "Needs the one-time adb setup"
            },
            icon = Icons.Rounded.Bedtime,
            iconTint = Color(0xFFA78BFA),
            trailing = when (status.screensaverTakeover) {
                ScreensaverTakeover.Status.ON -> "On"
                ScreensaverTakeover.Status.OFF -> "Off"
                ScreensaverTakeover.Status.NEEDS_PERMISSION -> "Set up"
            },
            onClick = onScreensaverTakeover
        )
        TvMenuItem(
            title = "Start after (on the home screen)",
            subtitle = "Inside other apps, Fire TV's own Screensaver start time applies",
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
            title = "Dim at night",
            subtitle = "Darker picture and clock from 10pm to 6am",
            icon = Icons.Rounded.Bedtime,
            iconTint = Color(0xFFA78BFA),
            trailing = if (settings.nightDim) "On" else "Off",
            onClick = { onUpdate { it.copy(nightDim = !it.nightDim) } }
        )
        TvMenuItem(
            title = "Keep Fire TV awake",
            subtitle = if (status.keepAwake) "Never sleeps on a timer; the screensaver keeps showing"
            else "Off: Fire TV sleeps after its sleep timer",
            icon = Icons.Rounded.LightMode,
            iconTint = FireAmber,
            trailing = if (status.keepAwake) "On" else "Off",
            onClick = onKeepAwake
        )

        SettingsSection("Fire TV")
        TvMenuItem(
            title = "Home button opens Onyx TV",
            subtitle = when (status.homeWatch) {
                HomeWatch.Status.ON -> "Brings Onyx TV back when Home opens Amazon's home"
                HomeWatch.Status.OFF -> "Off: Home shows Amazon's home"
                HomeWatch.Status.NEEDS_PERMISSION -> "Needs a one-time setup with adb"
            },
            icon = Icons.Rounded.Home,
            iconTint = FireAmber,
            trailing = when (status.homeWatch) {
                HomeWatch.Status.ON -> "On"
                HomeWatch.Status.OFF -> "Off"
                HomeWatch.Status.NEEDS_PERMISSION -> "Set up"
            },
            onClick = onHomeWatch
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

        SettingsSection("Kids mode")
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

        SettingsSection("About")
        TvMenuItem(
            title = "App updates",
            subtitle = status.updateSummary,
            icon = Icons.Rounded.SystemUpdate,
            iconTint = Color(0xFF38BDF8),
            trailing = "v${status.installedVersion}",
            onClick = onOpenUpdates
        )
    }
}

/** Small heading that starts a group of related settings. */
@Composable
private fun SettingsSection(title: String, first: Boolean = false) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = TextSecondary,
        modifier = Modifier.padding(start = 6.dp, top = if (first) 0.dp else 14.dp, bottom = 2.dp)
    )
}
