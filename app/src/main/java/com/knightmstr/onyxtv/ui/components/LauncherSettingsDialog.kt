package com.knightmstr.onyxtv.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Schedule
import com.knightmstr.onyxtv.data.ClockPlacement
import com.knightmstr.onyxtv.data.HomeLayout
import androidx.compose.ui.platform.LocalContext
import com.knightmstr.onyxtv.home.Platform
import com.knightmstr.onyxtv.data.UpdateChannel
import androidx.compose.material.icons.rounded.HourglassBottom
import com.knightmstr.onyxtv.kids.KidsTimeKeeper
import com.knightmstr.onyxtv.kids.KidsTime
import com.knightmstr.onyxtv.tools.SleepTimer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import com.knightmstr.onyxtv.tools.StorageInfo
import com.knightmstr.onyxtv.ui.ToolsUi
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
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
    onPhoneSetup: () -> Unit,
    sleepTimerEndsAt: Long?,
    tools: ToolsUi,
    onSleepTimer: (Int) -> Unit,
    onFreeUpSpace: () -> Unit,
    kidsTime: KidsTimeKeeper.Status?,
    onKidsDailyLimit: (Int) -> Unit,
    onKidsBedtime: (on: Boolean?, start: Int?, end: Int?) -> Unit,
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
    // Device words: "Fire TV" / "Amazon's" on Fire TV, "Google TV" / "Google's" on Google TV
    val names = Platform.names(LocalContext.current)
    val ownerCap = names.owner.replaceFirstChar { it.uppercase() }
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
        TvMenuItem(
            title = "Set up from phone",
            subtitle = "Favourites, hide, rename and photos from your phone — no adb",
            icon = Icons.Rounded.PhoneAndroid,
            iconTint = Color(0xFF34D399),
            modifier = Modifier.focusRequester(firstItem),
            onClick = onPhoneSetup
        )
        SleepTimerItem(endsAt = sleepTimerEndsAt, message = tools.sleepMessage, onClick = onSleepTimer)
        SettingsGroup(expanded = isOpen("Home screen")) {
        CollapsibleSectionHeader(
            title = "Home screen",
            icon = Icons.Rounded.Dashboard,
            expanded = isOpen("Home screen"),
            onToggle = { toggle("Home screen") }
        )
        if (isOpen("Home screen")) {
        TvChoiceItem(
            title = "Home layout",
            subtitle = settings.homeLayout.description,
            icon = Icons.Rounded.Dashboard,
            iconTint = Color(0xFF34D399),
            choices = HomeLayout.entries.map { TvChoice(it, it.title, it.description) },
            selected = settings.homeLayout,
            onSelect = { v -> onUpdate { it.copy(homeLayout = v) } }
        )
        TvChoiceItem(
            title = "Clock position",
            subtitle = settings.clockPlacement.description,
            icon = Icons.Rounded.Schedule,
            iconTint = Color(0xFF34D399),
            choices = ClockPlacement.entries.map { TvChoice(it, it.title, it.description) },
            selected = settings.clockPlacement,
            onSelect = { v -> onUpdate { it.copy(clockPlacement = v) } }
        )
        TvMenuItem(
            title = "Move clock slightly over time",
            subtitle = "A few pixels every few minutes, too small to notice. Protects any screen from burn-in",
            icon = Icons.Rounded.Schedule,
            iconTint = Color(0xFF34D399),
            trailing = if (settings.clockDrift) "On" else "Off",
            onClick = { onUpdate { it.copy(clockDrift = !it.clockDrift) } }
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
        TvChoiceItem(
            title = "App tile shape",
            subtitle = "Rounded, square, extra rounded or pill",
            icon = Icons.Rounded.RoundedCorner,
            iconTint = Color(0xFF60A5FA),
            choices = CardShape.entries.map { TvChoice(it, it.title) },
            selected = settings.cardShape,
            onSelect = { v -> onUpdate { it.copy(cardShape = v) } }
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
        }
        SettingsGroup(expanded = isOpen("Wallpaper")) {
        CollapsibleSectionHeader(
            title = "Wallpaper",
            icon = Icons.Rounded.Wallpaper,
            expanded = isOpen("Wallpaper"),
            onToggle = { toggle("Wallpaper") }
        )
        if (isOpen("Wallpaper")) {
        TvChoiceItem(
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
            choices = WallpaperSource.entries.map { TvChoice(it, it.title) },
            selected = settings.wallpaperSource,
            onSelect = { v -> onUpdate { it.copy(wallpaperSource = v) } }
        )
        if (settings.wallpaperSource == WallpaperSource.CUSTOM_URL) {
            TvMenuItem(
                title = "Manage photo links",
                subtitle = "Picture links, Drive / Dropbox / OneDrive, Reddit posts or subreddits",
                icon = Icons.Rounded.Link,
                iconTint = Color(0xFF34D399),
                trailing = if (status.customUrlCount > 0) "${status.customUrlCount}" else null,
                onClick = onManageWallpaperUrls
            )
        }
        TvChoiceItem(
            title = "Photo fit",
            subtitle = settings.wallpaperFit.description,
            icon = Icons.Rounded.Crop,
            iconTint = Color(0xFF34D399),
            choices = WallpaperFit.entries.map { TvChoice(it, it.title, it.description) },
            selected = settings.wallpaperFit,
            onSelect = { v -> onUpdate { it.copy(wallpaperFit = v) } }
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
        TvChoiceItem(
            title = "Change wallpaper every",
            icon = Icons.Rounded.Wallpaper,
            iconTint = FireAmber,
            choices = LauncherSettings.WALLPAPER_INTERVAL_OPTIONS.map {
                TvChoice(it, LauncherSettings.describeMinutes(it, zeroLabel = "Never"))
            },
            selected = settings.wallpaperIntervalMinutes,
            onSelect = { v -> onUpdate { it.copy(wallpaperIntervalMinutes = v) } }
        )

        }
        }
        SettingsGroup(expanded = isOpen("Screensaver")) {
        CollapsibleSectionHeader(
            title = "Screensaver",
            icon = Icons.Rounded.Bedtime,
            expanded = isOpen("Screensaver"),
            onToggle = { toggle("Screensaver") }
        )
        if (isOpen("Screensaver")) {
        TvChoiceItem(
            title = "Screensaver on the home screen",
            subtitle = if (settings.screensaverTimeoutMinutes > 0) {
                "Onyx's screensaver after this long on the home screen"
            } else {
                "Off: the home screen stays as it is (${names.device}'s own screensaver can still start)"
            },
            icon = Icons.Rounded.Bedtime,
            iconTint = Color(0xFFA78BFA),
            choices = LauncherSettings.SCREENSAVER_TIMEOUT_OPTIONS.map {
                TvChoice(it, LauncherSettings.describeMinutes(it))
            },
            selected = settings.screensaverTimeoutMinutes,
            onSelect = { v -> onUpdate { it.copy(screensaverTimeoutMinutes = v) } }
        )
        TvMenuItem(
            title = "Screensaver inside other apps",
            subtitle = when {
                status.screensaverTakeover == ScreensaverTakeover.Status.ON && status.needsRestart ->
                    "Onyx: restart the ${names.device} once to replace ${names.owner}"
                status.screensaverTakeover == ScreensaverTakeover.Status.ON ->
                    "Onyx's wallpaper, clock and weather. Starts on ${names.device}'s own timer (${names.screensaverTimerPath})"
                status.screensaverTakeover == ScreensaverTakeover.Status.OFF ->
                    "$ownerCap. Starts on ${names.device}'s own timer (${names.screensaverTimerPath})"
                else -> "Needs the one-time adb setup"
            },
            icon = Icons.Rounded.Bedtime,
            iconTint = Color(0xFFA78BFA),
            trailing = when (status.screensaverTakeover) {
                ScreensaverTakeover.Status.ON -> "Onyx"
                ScreensaverTakeover.Status.OFF -> ownerCap
                ScreensaverTakeover.Status.NEEDS_PERMISSION -> "Set up"
            },
            onClick = onScreensaverTakeover
        )
        TvMenuItem(
            title = "Dim at night",
            subtitle = if (settings.nightStartHour == settings.nightEndHour) {
                "Start and end are the same time, so it never dims"
            } else {
                "Darker picture and clock from ${LauncherSettings.describeHour(settings.nightStartHour)} " +
                    "to ${LauncherSettings.describeHour(settings.nightEndHour)}"
            },
            icon = Icons.Rounded.Bedtime,
            iconTint = Color(0xFFA78BFA),
            trailing = if (settings.nightDim) "On" else "Off",
            onClick = { onUpdate { it.copy(nightDim = !it.nightDim) } }
        )
        TvMenuItem(
            title = "Dim the home screen too",
            subtitle = "Darker wallpaper and clock on the home screen during the same hours",
            icon = Icons.Rounded.Bedtime,
            iconTint = Color(0xFFA78BFA),
            trailing = if (settings.homeNightDim) "On" else "Off",
            onClick = { onUpdate { it.copy(homeNightDim = !it.homeNightDim) } }
        )
        if (settings.nightDim || settings.homeNightDim) {
            TvSliderItem(
                title = "Dim from",
                subtitle = "Left/Right to change the hour",
                icon = Icons.Rounded.Bedtime,
                iconTint = Color(0xFFA78BFA),
                value = settings.nightStartHour,
                range = LauncherSettings.HOUR_RANGE,
                step = 1,
                valueLabel = LauncherSettings.describeHour(settings.nightStartHour),
                onValueChange = { h -> onUpdate { it.copy(nightStartHour = h) } }
            )
            TvSliderItem(
                title = "Dim until",
                subtitle = "Left/Right to change the hour",
                icon = Icons.Rounded.LightMode,
                iconTint = Color(0xFFA78BFA),
                value = settings.nightEndHour,
                range = LauncherSettings.HOUR_RANGE,
                step = 1,
                valueLabel = LauncherSettings.describeHour(settings.nightEndHour),
                onValueChange = { h -> onUpdate { it.copy(nightEndHour = h) } }
            )
        }
        TvMenuItem(
            title = "Keep ${names.device} awake",
            subtitle = if (status.keepAwake) "Never sleeps on a timer; the screensaver keeps showing"
            else "Off: ${names.device} sleeps after its sleep timer",
            icon = Icons.Rounded.LightMode,
            iconTint = FireAmber,
            trailing = if (status.keepAwake) "On" else "Off",
            onClick = onKeepAwake
        )

        }
        }
        SettingsGroup(expanded = isOpen("Fire TV")) {
        CollapsibleSectionHeader(
            title = names.device,
            icon = Icons.Rounded.Home,
            expanded = isOpen("Fire TV"),
            onToggle = { toggle("Fire TV") }
        )
        if (isOpen("Fire TV")) {
        TvMenuItem(
            title = "Home button opens Onyx TV",
            subtitle = when (status.homeWatch) {
                HomeWatch.Status.ON -> "Brings Onyx TV back when Home opens ${names.owner} home"
                HomeWatch.Status.OFF -> "Off: Home shows ${names.owner} home"
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
        }
        SettingsGroup(expanded = isOpen("Storage")) {
        CollapsibleSectionHeader(
            title = "Storage",
            icon = Icons.Rounded.Storage,
            expanded = isOpen("Storage"),
            onToggle = { toggle("Storage") }
        )
        if (isOpen("Storage")) {
        val space = tools.space
        TvMenuItem(
            title = "Free space",
            subtitle = space?.let { "${StorageInfo.describe(it.freeBytes)} free of ${StorageInfo.describe(it.totalBytes)}" }
                ?: "Couldn't read storage",
            icon = Icons.Rounded.Storage,
            iconTint = Color(0xFF60A5FA),
            trailing = space?.let { "${(it.freeBytes * 100 / it.totalBytes.coerceAtLeast(1))}%" },
            onClick = {}
        )
        TvMenuItem(
            title = "Free up space",
            subtitle = tools.storageMessage
                ?: "Clears every app's cache. Logins, downloads and settings stay. Needs ADB debugging on",
            icon = Icons.Rounded.CleaningServices,
            iconTint = Color(0xFF60A5FA),
            trailing = if (tools.cleaning) "…" else null,
            enabled = !tools.cleaning,
            onClick = onFreeUpSpace
        )
        }
        }
        SettingsGroup(expanded = isOpen("Kids mode")) {
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
        kidsTime?.let { kt ->
            val r = kt.rules
            val usedMin = (kt.usedMs / 60_000L).toInt()
            TvChoiceItem(
                title = "Daily screen time",
                subtitle = if (r.dailyLimitMinutes > 0) {
                    "In Kids mode. Used today: ${LauncherSettings.describeMinutes(usedMin, zeroLabel = "0 min")}"
                } else "No daily limit",
                icon = Icons.Rounded.HourglassBottom,
                iconTint = Color(0xFFF472B6),
                choices = KidsTime.LIMIT_OPTIONS.map {
                    TvChoice(it, if (it > 0) LauncherSettings.describeMinutes(it) else "Off")
                },
                selected = r.dailyLimitMinutes,
                onSelect = onKidsDailyLimit
            )
            TvMenuItem(
                title = "Bedtime",
                subtitle = if (r.bedtimeOn) {
                    "Kids mode closes from ${LauncherSettings.describeHour(r.bedtimeStartHour)} " +
                        "to ${LauncherSettings.describeHour(r.bedtimeEndHour)}"
                } else "Lock Kids mode overnight",
                icon = Icons.Rounded.Bedtime,
                iconTint = Color(0xFFF472B6),
                trailing = if (r.bedtimeOn) "On" else "Off",
                onClick = { onKidsBedtime(!r.bedtimeOn, null, null) }
            )
            if (r.bedtimeOn) {
                TvSliderItem(
                    title = "Bedtime from",
                    subtitle = "Left/Right to change the hour",
                    icon = Icons.Rounded.Bedtime,
                    iconTint = Color(0xFFF472B6),
                    value = r.bedtimeStartHour,
                    range = LauncherSettings.HOUR_RANGE,
                    step = 1,
                    valueLabel = LauncherSettings.describeHour(r.bedtimeStartHour),
                    onValueChange = { h -> onKidsBedtime(null, h, null) }
                )
                TvSliderItem(
                    title = "Bedtime until",
                    subtitle = "Left/Right to change the hour",
                    icon = Icons.Rounded.LightMode,
                    iconTint = Color(0xFFF472B6),
                    value = r.bedtimeEndHour,
                    range = LauncherSettings.HOUR_RANGE,
                    step = 1,
                    valueLabel = LauncherSettings.describeHour(r.bedtimeEndHour),
                    onValueChange = { h -> onKidsBedtime(null, null, h) }
                )
            }
        }
        if (status.hasKidsPin) {
            TvMenuItem(
                title = "Change Kids mode PIN",
                icon = Icons.Rounded.Lock,
                iconTint = Color(0xFFF472B6),
                onClick = onChangeKidsPin
            )
        }

        }
        }
        SettingsGroup(expanded = isOpen("About")) {
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
        TvChoiceItem(
            title = "Update channel",
            subtitle = settings.updateChannel.description,
            icon = Icons.Rounded.Science,
            iconTint = Color(0xFFA78BFA),
            choices = UpdateChannel.entries.map { TvChoice(it, it.title, it.description) },
            selected = settings.updateChannel,
            onSelect = { v -> onUpdate { it.copy(updateChannel = v) } }
        )
        }
        }
    }
}

/**
 * Wraps a settings group (its header plus revealed rows) so an expanded group reads as one box.
 * Collapsed groups show just their header with no frame.
 */
@Composable
private fun SettingsGroup(
    expanded: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (expanded) {
                    Modifier
                        .background(Color(0x14FFFFFF), shape)
                        .border(BorderStroke(1.dp, Color(0x26FFFFFF)), shape)
                        .padding(4.dp)
                } else {
                    Modifier
                }
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

/**
 * A tappable header for a group of settings; OK toggles the group.
 * Collapsed: grey "Show" with a right-pointing chevron. Expanded: green "Hide" with a down chevron,
 * so it's obvious at a glance which groups are folded away.
 */
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
        iconTint = if (expanded) Color(0xFF10B981) else Color(0xFF9CA3AF),
        trailing = if (expanded) "Hide" else "Show",
        trailingIcon = if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
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

/** Sleep timer row: a drop-down of Off, 15, 30, 45, 60, 90 and 120 minutes. */
@Composable
private fun SleepTimerItem(endsAt: Long?, message: String?, onClick: (Int) -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endsAt) {
        while (endsAt != null) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(15_000)
        }
    }
    val left = endsAt?.let { ((it - now + 59_999) / 60_000).toInt() }?.takeIf { it > 0 }
    TvChoiceItem(
        title = "Sleep timer",
        subtitle = message ?: if (left != null) {
            "${Platform.names(LocalContext.current).device} sleeps in $left min"
        } else {
            "Puts the ${Platform.names(LocalContext.current).device} (and the TV, with HDMI-CEC) to sleep later"
        },
        icon = Icons.Rounded.Snooze,
        iconTint = Color(0xFFA78BFA),
        choices = SleepTimer.OPTIONS.map { TvChoice(it, if (it == 0) "Off" else LauncherSettings.describeMinutes(it)) },
        selected = if (left == null) 0 else -1,
        currentLabel = left?.let { "$it min left" },
        onSelect = onClick
    )
}
