package com.knightmstr.firelauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.knightmstr.firelauncher.data.AppModel
import com.knightmstr.firelauncher.media.NowPlaying
import com.knightmstr.firelauncher.ui.theme.DarkSurface
import com.knightmstr.firelauncher.ui.theme.DarkSurfaceVariant
import com.knightmstr.firelauncher.ui.theme.FireAmber
import com.knightmstr.firelauncher.ui.theme.FireOrange
import com.knightmstr.firelauncher.ui.theme.TextPrimary
import com.knightmstr.firelauncher.ui.theme.TextSecondary

/** Row of round one-press app shortcuts shown in the header. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun QuickButtonsBar(
    apps: List<AppModel>,
    onClick: (AppModel) -> Unit,
    onLongClick: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (apps.isEmpty()) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        apps.forEach { app ->
            val bitmap = remember(app.packageName, app.icon) {
                runCatching { app.icon.toBitmap(96, 96).asImageBitmap() }.getOrNull()
            }
            Surface(
                onClick = { onClick(app) },
                onLongClick = { onLongClick(app) },
                // Same size and style as the round icon buttons next to it (TopIconButton)
                modifier = Modifier.size(40.dp),
                shape = ClickableSurfaceDefaults.shape(CircleShape),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.15f),
                border = ClickableSurfaceDefaults.border(
                    border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), shape = CircleShape),
                    focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = CircleShape)
                ),
                glow = ClickableSurfaceDefaults.glow(
                    focusedGlow = Glow(elevationColor = FireOrange.copy(alpha = 0.7f), elevation = 14.dp)
                ),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = DarkSurface.copy(alpha = 0.85f),
                    focusedContainerColor = DarkSurfaceVariant
                )
            ) {
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        Image(bitmap = bitmap, contentDescription = app.label, modifier = Modifier.size(22.dp))
                    } else {
                        Text(app.label.take(1), color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Small "now playing" window: the playing app's icon and name, then what's playing, then
 * previous / play-pause / next. Selecting the left part opens the app.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NowPlayingCard(
    nowPlaying: NowPlaying,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appIcon = remember(nowPlaying.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(nowPlaying.packageName).toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }
    val windowShape = RoundedCornerShape(18.dp)
    val infoShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .background(Color(0x66090C14), windowShape)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // App icon, app name and what's playing; OK opens the app
        Surface(
            onClick = onOpenApp,
            shape = ClickableSurfaceDefaults.shape(infoShape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f)), shape = infoShape)
            ),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.White.copy(alpha = 0.12f)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(9.dp))
                    )
                } else {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = FireAmber, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.width(210.dp)) {
                    Text(
                        text = nowPlaying.appLabel + if (nowPlaying.isPlaying) "  ·  Playing" else "  ·  Paused",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = nowPlaying.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!nowPlaying.subtitle.isNullOrBlank()) {
                        Text(
                            text = nowPlaying.subtitle,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        MediaButton(Icons.Rounded.SkipPrevious, "Previous", onPrevious)
        MediaButton(
            if (nowPlaying.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            if (nowPlaying.isPlaying) "Pause" else "Play",
            onPlayPause
        )
        MediaButton(Icons.Rounded.SkipNext, "Next", onNext)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MediaButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(34.dp),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.15f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = CircleShape)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = DarkSurfaceVariant
        )
    ) {
        Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = TextPrimary, modifier = Modifier.size(20.dp))
        }
    }
}
