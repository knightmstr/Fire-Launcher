package com.example.firelauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.firelauncher.ui.theme.FireAmber
import com.example.firelauncher.ui.theme.FireOrange
import com.example.firelauncher.ui.theme.TextPrimary
import com.example.firelauncher.ui.theme.TextSecondary

/**
 * Leanback Context Menu Dialog triggered by Long-Press D-pad OK on an app card.
 * Provides instant 1-click access to:
 * 1. Add / Remove from Favorites
 * 2. Force Stop & App Info (Opens Fire OS Settings: android.settings.APPLICATION_DETAILS_SETTINGS)
 * 3. Launch App
 * 4. Cancel
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppContextMenuDialog(
    app: AppModel,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onLaunchApp: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(app.accentColor)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xDD07090F))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = {},
            modifier = Modifier
                .width(460.dp)
                .clickable(enabled = false) {},
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
            border = ClickableSurfaceDefaults.border(
                Border(
                    border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(20.dp)
                )
            ),
            glow = ClickableSurfaceDefaults.glow(
                focusedGlow = Glow(
                    elevationColor = accentColor.copy(alpha = 0.5f),
                    elevation = 24.dp
                )
            ),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color(0xFF141824)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header: App Name, Package, & Dismiss
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = app.label,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = app.packageName,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action 1: Toggle Favorites
                ContextMenuItem(
                    title = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                    subtitle = if (isFavorite) "Remove this app from your top favorites row" else "Pin to top row for instant 1-click access",
                    icon = if (isFavorite) Icons.Rounded.StarOutline else Icons.Rounded.Star,
                    iconTint = FavoriteYellow,
                    onClick = {
                        onToggleFavorite()
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action 2: App Info & Force Stop (Fire OS Settings)
                ContextMenuItem(
                    title = "Force Stop & App Info",
                    subtitle = "Open Fire OS settings to Force Stop, Clear Cache, or manage data",
                    icon = Icons.Rounded.Settings,
                    iconTint = FireOrange,
                    onClick = {
                        onOpenAppDetails()
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action 3: Open Application
                ContextMenuItem(
                    title = "Open Application",
                    subtitle = "Launch ${app.label} directly",
                    icon = Icons.Rounded.PlayArrow,
                    iconTint = FireAmber,
                    onClick = {
                        onLaunchApp()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ContextMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = shape
            )
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevationColor = iconTint.copy(alpha = 0.4f),
                elevation = 10.dp
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DarkSurface,
            focusedContainerColor = DarkSurfaceVariant
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
