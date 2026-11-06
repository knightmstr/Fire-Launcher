package com.example.firelauncher.ui.components

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.firelauncher.data.AppModel
import com.example.firelauncher.ui.theme.DarkSurface
import com.example.firelauncher.ui.theme.DarkSurfaceVariant
import com.example.firelauncher.ui.theme.FavoriteYellow
import com.example.firelauncher.ui.theme.TextPrimary

/**
 * 16:9 App card built using tv-material Surface.
 * On D-pad focus:
 * - Scales to 1.12
 * - White 2dp border
 * - Accent-color tinted elevation glow
 * - Shows small yellow dot if favorited
 * - Handles OK click to launch, and Long-press OK to toggle favorite
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppCard(
    app: AppModel,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val accentColor = Color(app.accentColor)
    val cardShape = RoundedCornerShape(12.dp)
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .width(200.dp)
            .aspectRatio(16f / 9f)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onFocus()
                }
            },
        shape = ClickableSurfaceDefaults.shape(
            shape = cardShape,
            focusedShape = cardShape
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1.12f
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = cardShape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = cardShape
            )
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevationColor = accentColor.copy(alpha = 0.85f),
                elevation = 20.dp
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DarkSurface,
            focusedContainerColor = DarkSurfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                // If it's a system shortcut, render clean icon + title
                app.isSystemShortcut -> {
                    SystemShortcutContent(app = app, accentColor = accentColor)
                }

                // If app has Leanback banner (320x180), show the banner
                app.banner != null -> {
                    BannerContent(banner = app.banner, label = app.label)
                }

                // Standard app without banner: Icon centered + title at bottom
                else -> {
                    IconFallbackContent(icon = app.icon, label = app.label, accentColor = accentColor)
                }
            }

            // Yellow favorite indicator dot in top-right corner
            if (isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(FavoriteYellow)
                )
            }
        }
    }
}

@Composable
private fun SystemShortcutContent(
    app: AppModel,
    accentColor: Color
) {
    val iconVector: ImageVector = when (app.label.lowercase()) {
        "wi-fi" -> Icons.Rounded.Wifi
        "display" -> Icons.Rounded.DisplaySettings
        "sound" -> Icons.Rounded.VolumeUp
        "bluetooth" -> Icons.Rounded.Bluetooth
        else -> Icons.Rounded.Settings
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.25f),
                        DarkSurface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = iconVector,
                contentDescription = app.label,
                tint = accentColor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BannerContent(
    banner: Drawable,
    label: String
) {
    val bitmap = remember(banner) {
        try {
            banner.toBitmap(width = 320, height = 180)
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun IconFallbackContent(
    icon: Drawable,
    label: String,
    accentColor: Color
) {
    val bitmap = remember(icon) {
        try {
            icon.toBitmap(width = 96, height = 96)
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.15f),
                        DarkSurface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = label,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
