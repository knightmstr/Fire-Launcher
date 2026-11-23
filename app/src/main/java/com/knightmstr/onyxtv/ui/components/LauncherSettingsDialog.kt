package com.knightmstr.onyxtv.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import com.knightmstr.onyxtv.data.CardShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.rounded.Crop
import coil.compose.AsyncImage
import com.knightmstr.onyxtv.data.WallpaperFit
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.knightmstr.onyxtv.data.LauncherSettings
import com.knightmstr.onyxtv.home.HomeWatch
import com.knightmstr.onyxtv.screensaver.ScreensaverTakeover
import com.knightmstr.onyxtv.data.LocalPhotoSource
import com.knightmstr.onyxtv.data.WallpaperSource
import com.knightmstr.onyxtv.ui.theme.FireAmber
import com.knightmstr.onyxtv.ui.theme.FireOrange
import com.knightmstr.onyxtv.ui.theme.TextSecondary

/** Read-only facts the settings screen displays next to each option. */
data class SettingsStatus(
    val isCelsius: Boolean,
    val hiddenAppCount: Int,
    val localPhotoCount: Int,
    val uploadedPhotoCount: Int,
    val customUrlCount: Int,
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
    onUploadPhotos: () -> Unit,
    onClearUploads: () -> Unit,
    onManageWallpaperUrls: () -> Unit,
    wallpaperPreviewUrl: String?,
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

    // Which settings groups are expanded. Home screen opens by default; the rest start collapsed.
    val expanded = remember { mutableStateMapOf("Home screen" to true) }
    val isOpen: (String) -> Boolean = { expanded[it] == true }
    val toggle: (String) -> Unit = { expanded[it] = !(expanded[it] ?: false) }

    TvDialogFrame(
        title = "Launcher Settings",
        subtitle = "OK to change • Left/Right on sliders • Back to close",
        accentColor = Color(0xFF10B981),
        onDismiss = onDismiss,
        width = 540.dp
    ) {
        CollapsibleSectionHeader(
            title = "Home screen",
            icon = Icons.Rounded.Dashboard,
            expanded = isOpen("Home screen"),
            onToggle = { toggle("Home screen") },
            modifier = Modifier.focusRequester(firstItem)
        )
        if (isOpen("Home screen")) {
        TvMenuItem(
            title = "Home layout",
            subtitle = settings.homeLayout.description,
            icon = Icons.Rounded.Dashboard,
            iconTint = Color(0xFF34D399),
            trailing = settings.homeLayout.title,
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
        // Live preview: two sample app tiles at the chosen size and shape.
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 2.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Icons.Rounded.PhotoLibrary to Color(0xFF38BDF8),
                Icons.Rounded.MusicNote to Color(0xFFF472B6)
            ).forEach { (sampleIcon, tint) ->
                Box(
                    modifier = Modifier
                        .width(settings.tileWidthDp.dp)
                        .aspectRatio(16f / 9f)
                        .clip(tilePreviewCorner(settings.cardShape, settings.tileWidthDp))
                        .background(Color(0xFF2A3140)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = sampleIcon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size((settings.tileWidthDp * 0.3f).dp)
                    )
                }
            }
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

        }
        CollapsibleSectionHeader(
            title = "Wallpaper",
            icon = Icons.Rounded.Wallpaper,
            expanded = isOpen("Wallpaper"),
            onToggle = { toggle("Wallpaper") }
        )
        if (isOpen("Wallpaper")) {
        TvMenuItem(
            title = "Wallpapers from",
            subtitle = when (settings.wallpaperSource) {
                WallpaperSource.REDDIT -> "Downloads new wallpapers from Reddit"
                WallpaperSource.CUSTOM_URL ->
                    if (status.customUrlCount > 0) "${status.customUrlCount} link${if (status.customUrlCount == 1) "" else "s"} added"
                    else "No links yet — add some (using Reddit)"
                WallpaperSource.MY_PHOTOS ->
                    if (status.localPhotoCount > 0) "${status.localPhotoCount} photos found"
                    else "None found: add a \"${LocalPhotoSource.ALBUM_NAME}\" album (using Reddit)"
            },
            icon = Icons.Rounded.PhotoLibrary,
            iconTint = Color(0xFF34D399),
            trailing = settings.wallpaperSource.title,
            onClick = { onUpdate { it.copy(wallpaperSource = it.wallpaperSource.next()) } }
        )
        if (settings.wallpaperSource == WallpaperSource.CUSTOM_URL) {
            TvMenuItem(
                title = "Manage photo links",
                subtitle = "Paste image URLs to use as wallpapers",
                icon = Icons.Rounded.Link,
                iconTint = Color(0xFF34D399),
                trailing = if (status.customUrlCount > 0) "${status.customUrlCount}" else null,
                onClick = onManageWallpaperUrls
            )
        }
        TvMenuItem(
            title = "Photo fit",
            subtitle = settings.wallpaperFit.description,
            icon = Icons.Rounded.Crop,
            iconTint = Color(0xFF34D399),
            trailing = settings.wallpaperFit.title,
            onClick = { onUpdate { it.copy(wallpaperFit = it.wallpaperFit.next()) } }
        )
        if (wallpaperPreviewUrl != null) {
            // Live preview of the current wallpaper in the chosen fit.
            AsyncImage(
                model = wallpaperPreviewUrl,
                contentDescription = "Wallpaper fit preview",
                contentScale = settings.wallpaperFit.toContentScale(),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(220.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF000000))
            )
        }
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
            title = "Upload photos over Wi-Fi",
            subtitle = "Open a web address on your phone — no cable or adb",
            icon = Icons.Rounded.CloudUpload,
            iconTint = Color(0xFF34D399),
            onClick = onUploadPhotos
        )
        if (status.uploadedPhotoCount > 0) {
            TvMenuItem(
                title = "Clear uploaded photos",
                subtitle = "Removes the ${status.uploadedPhotoCount} photo${if (status.uploadedPhotoCount == 1) "" else "s"} uploaded here (not your gallery)",
                icon = Icons.Rounded.DeleteSweep,
                iconTint = Color(0xFFF87171),
                onClick = onClearUploads
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

        }
        CollapsibleSectionHeader(
            title = "Screensaver",
            icon = Icons.Rounded.Bedtime,
            expanded = isOpen("Screensaver"),
            onToggle = { toggle("Screensaver") }
        )
        if (isOpen("Screensaver")) {
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

        }
        CollapsibleSectionHeader(
            title = "Fire TV",
            icon = Icons.Rounded.Home,
            expanded = isOpen("Fire TV"),
            onToggle = { toggle("Fire TV") }
        )
        if (isOpen("Fire TV")) {
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

        }
        CollapsibleSectionHeader(
            title = "Kids mode",
            icon = Icons.Rounded.ChildCare,
            expanded = isOpen("Kids mode"),
            onToggle = { toggle("Kids mode") }
        )
        if (isOpen("Kids mode")) {
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

        }
        CollapsibleSectionHeader(
            title = "About",
            icon = Icons.Rounded.SystemUpdate,
            expanded = isOpen("About"),
            onToggle = { toggle("About") }
        )
        if (isOpen("About")) {
        TvMenuItem(
            title = "App updates",
            subtitle = status.updateSummary,
            icon = Icons.Rounded.SystemUpdate,
            iconTint = Color(0xFF38BDF8),
            trailing = "v${status.installedVersion}",
            onClick = onOpenUpdates
        )
        TvMenuItem(
            title = "Update channel",
            subtitle = settings.updateChannel.description,
            icon = Icons.Rounded.Science,
            iconTint = Color(0xFFA78BFA),
            trailing = settings.updateChannel.title,
            onClick = { onUpdate { it.copy(updateChannel = it.updateChannel.next()) } }
        )
        }
    }
}

/** A tappable header for a group of settings; OK toggles the group open or closed. */
@Composable
private fun CollapsibleSectionHeader(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvMenuItem(
        title = title,
        icon = icon,
        iconTint = Color(0xFF9CA3AF),
        trailing = if (expanded) "Open" else "Closed",
        modifier = modifier,
        onClick = onToggle
    )
}

/** Maps the user's wallpaper fit choice to a Compose [ContentScale]. */
fun WallpaperFit.toContentScale(): ContentScale = when (this) {
    WallpaperFit.FILL -> ContentScale.Crop
    WallpaperFit.FIT -> ContentScale.Fit
    WallpaperFit.STRETCH -> ContentScale.FillBounds
}

/** Corner shape for a tile-size preview, matching AppCard's shape scaling (DESIGN_WIDTH = 200). */
private fun tilePreviewCorner(shape: CardShape, widthDp: Int): RoundedCornerShape = when (shape) {
    CardShape.ROUNDED -> RoundedCornerShape((12 * widthDp / 200f).dp)
    CardShape.SQUARE -> RoundedCornerShape((2 * widthDp / 200f).dp)
    CardShape.SOFT -> RoundedCornerShape((26 * widthDp / 200f).dp)
    CardShape.PILL -> RoundedCornerShape(percent = 50)
}
