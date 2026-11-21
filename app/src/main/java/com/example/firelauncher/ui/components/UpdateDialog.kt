package com.example.firelauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.firelauncher.update.ReleaseInfo
import com.example.firelauncher.update.UpdateState
import com.example.firelauncher.ui.theme.DarkSurface
import com.example.firelauncher.ui.theme.DarkSurfaceVariant
import com.example.firelauncher.ui.theme.TextPrimary
import com.example.firelauncher.ui.theme.TextSecondary

private val UpdateBlue = Color(0xFF38BDF8)

/** Check / download / install flow for new FireLauncher releases. */
@Composable
fun UpdateDialog(
    state: UpdateState,
    installedVersion: String,
    onCheck: () -> Unit,
    onUpdate: () -> Unit,
    onOpenInstallPermission: () -> Boolean,
    onDismiss: () -> Unit
) {
    val first = rememberInitialFocus()
    var permissionScreenMissing by remember { mutableStateOf(false) }

    TvDialogFrame(
        title = "FireLauncher updates",
        subtitle = "Installed: v$installedVersion",
        accentColor = UpdateBlue,
        onDismiss = onDismiss,
        width = 560.dp
    ) {
        when (state) {
            UpdateState.Idle, UpdateState.Checking -> {
                StatusText("Checking GitHub for a new version…")
                TvMenuItem(
                    title = "Close",
                    icon = Icons.Rounded.SystemUpdate,
                    iconTint = TextSecondary,
                    modifier = Modifier.focusRequester(first),
                    onClick = onDismiss
                )
            }

            is UpdateState.UpToDate -> {
                StatusText("You're on the latest version (v${state.currentVersion}).")
                TvMenuItem(
                    title = "Check again",
                    icon = Icons.Rounded.Refresh,
                    iconTint = UpdateBlue,
                    modifier = Modifier.focusRequester(first),
                    onClick = onCheck
                )
            }

            is UpdateState.Available -> {
                ReleaseHeader(state.release)
                TvMenuItem(
                    title = "Download & install v${state.release.versionName}",
                    subtitle = "${state.release.apkSizeBytes / (1024 * 1024)} MB • Android will ask you to confirm",
                    icon = Icons.Rounded.Download,
                    iconTint = UpdateBlue,
                    modifier = Modifier.focusRequester(first),
                    onClick = onUpdate
                )
                ReleaseNotes(state.release)
            }

            is UpdateState.Downloading -> {
                ReleaseHeader(state.release)
                val pct = state.progress?.let { (it * 100).toInt() }
                StatusText(if (pct != null) "Downloading… $pct%" else "Downloading…")
                ProgressBar(state.progress ?: 0f)
                TvMenuItem(
                    title = "Hide (keeps downloading)",
                    icon = Icons.Rounded.SystemUpdate,
                    iconTint = TextSecondary,
                    modifier = Modifier.focusRequester(first),
                    onClick = onDismiss
                )
            }

            is UpdateState.NeedsInstallPermission -> {
                StatusText(
                    "One-time step: allow FireLauncher to install updates. " +
                        "Turn it on, press Back to return here, then choose Try again."
                )
                TvMenuItem(
                    title = "Open \"Install unknown apps\"",
                    subtitle = if (permissionScreenMissing)
                        "Not available here: Settings → My Fire TV → Developer options → Install unknown apps"
                    else "Switch FireLauncher on",
                    icon = Icons.Rounded.Lock,
                    iconTint = UpdateBlue,
                    modifier = Modifier.focusRequester(first),
                    onClick = { permissionScreenMissing = !onOpenInstallPermission() }
                )
                TvMenuItem(
                    title = "Try again",
                    icon = Icons.Rounded.Refresh,
                    iconTint = TextSecondary,
                    onClick = onUpdate
                )
            }

            is UpdateState.Installing -> {
                ReleaseHeader(state.release)
                StatusText("Confirm on the install screen. FireLauncher will restart on the new version.")
                TvMenuItem(
                    title = "Close",
                    icon = Icons.Rounded.SystemUpdate,
                    iconTint = TextSecondary,
                    modifier = Modifier.focusRequester(first),
                    onClick = onDismiss
                )
            }

            is UpdateState.Failed -> {
                StatusText(state.message, color = Color(0xFFF87171))
                TvMenuItem(
                    title = if (state.release != null) "Try again" else "Check again",
                    icon = Icons.Rounded.Refresh,
                    iconTint = UpdateBlue,
                    modifier = Modifier.focusRequester(first),
                    onClick = if (state.release != null) onUpdate else onCheck
                )
            }
        }
    }
}

@Composable
private fun ReleaseHeader(release: ReleaseInfo) {
    Text(
        text = "Version ${release.versionName} is available",
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    )
}

@Composable
private fun ReleaseNotes(release: ReleaseInfo) {
    if (release.notes.isBlank()) return
    Text(
        text = release.notes,
        fontSize = 11.sp,
        color = TextSecondary,
        maxLines = 12,
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(8.dp))
            .padding(10.dp)
    )
}

@Composable
private fun StatusText(text: String, color: Color = TextSecondary) {
    Text(text = text, fontSize = 13.sp, color = color, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(UpdateBlue, RoundedCornerShape(4.dp))
        )
    }
}

/** Small header chip shown when a new version is available. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun UpdateChip(state: UpdateState, onClick: () -> Unit) {
    val label = when (state) {
        is UpdateState.Available -> "Update ${state.release.versionName}"
        is UpdateState.Downloading -> "Updating ${state.progress?.let { "${(it * 100).toInt()}%" } ?: "…"}"
        is UpdateState.NeedsInstallPermission -> "Update needs permission"
        else -> return
    }
    val shape = RoundedCornerShape(14.dp)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, UpdateBlue.copy(alpha = 0.6f)), shape = shape),
            focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = shape)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DarkSurface.copy(alpha = 0.85f),
            focusedContainerColor = DarkSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = UpdateBlue, modifier = Modifier.size(14.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}
