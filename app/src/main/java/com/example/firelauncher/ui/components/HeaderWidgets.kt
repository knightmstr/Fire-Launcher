package com.example.firelauncher.ui.components

import androidx.compose.foundation.BorderStroke
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
import com.example.firelauncher.data.AppModel
import com.example.firelauncher.media.NowPlaying
import com.example.firelauncher.ui.theme.DarkSurface
import com.example.firelauncher.ui.theme.DarkSurfaceVariant
import com.example.firelauncher.ui.theme.FireAmber
import com.example.firelauncher.ui.theme.TextPrimary
import com.example.firelauncher.ui.theme.TextSecondary

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
                modifier = Modifier.size(44.dp),
                shape = ClickableSurfaceDefaults.shape(CircleShape),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.15f),
                border = ClickableSurfaceDefaults.border(
                    border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), shape = CircleShape),
                    focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = CircleShape)
                ),
                glow = ClickableSurfaceDefaults.glow(
                    focusedGlow = Glow(elevationColor = Color(app.accentColor).copy(alpha = 0.7f), elevation = 12.dp)
                ),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = DarkSurface.copy(alpha = 0.85f),
                    focusedContainerColor = DarkSurfaceVariant
                )
            ) {
                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        Image(bitmap = bitmap, contentDescription = app.label, modifier = Modifier.size(28.dp))
                    } else {
                        Text(app.label.take(1), color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Compact "now playing" chip with previous / play-pause / next controls. */
@Composable
fun NowPlayingCard(
    nowPlaying: NowPlaying,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xAA141824), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = FireAmber, modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.width(170.dp).padding(horizontal = 4.dp)) {
            Text(
                text = nowPlaying.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(nowPlaying.subtitle, nowPlaying.appLabel).joinToString(" • "),
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        MediaButton(Icons.Rounded.SkipPrevious, "Previous", onPrevious)
        MediaButton(
            if (nowPlaying.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            if (nowPlaying.isPlaying) "Pause" else "Play",
            onPlayPause,
            onLongClick = onOpenApp
        )
        MediaButton(Icons.Rounded.SkipNext, "Next", onNext)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MediaButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
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
