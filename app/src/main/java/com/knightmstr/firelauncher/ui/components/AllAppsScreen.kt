package com.knightmstr.firelauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.knightmstr.firelauncher.data.AppModel
import com.knightmstr.firelauncher.data.AppSortOrder
import com.knightmstr.firelauncher.ui.theme.DarkSurface
import com.knightmstr.firelauncher.ui.theme.DarkSurfaceVariant
import com.knightmstr.firelauncher.ui.theme.FireAmber
import com.knightmstr.firelauncher.ui.theme.TextPrimary
import com.knightmstr.firelauncher.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Full-screen grid of every visible app, opened from the "All Apps" tile.
 * Rows wrap to the screen width (no sideways scrolling); Back closes it.
 * Hold OK / ☰ on an app for the usual options menu.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AllAppsScreen(
    apps: List<AppModel>,
    favorites: Set<String>,
    sortOrder: AppSortOrder,
    onCycleSort: () -> Unit,
    onAppFocused: (AppModel) -> Unit,
    onAppClicked: (AppModel) -> Unit,
    onAppLongClicked: (AppModel) -> Unit,
    onDismiss: () -> Unit
) {
    val firstItem = remember { FocusRequester() }
    val cardWidth = LocalCardWidth.current

    LaunchedEffect(apps.isNotEmpty()) {
        if (apps.isNotEmpty()) {
            delay(50)
            runCatching { firstItem.requestFocus() }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF207090F))
                .padding(horizontal = 48.dp, vertical = 28.dp) // TV overscan-safe margins
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Apps, contentDescription = null, tint = FireAmber, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("All Apps", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Text("${apps.size}", fontSize = 16.sp, color = TextSecondary)
                Spacer(modifier = Modifier.weight(1f))
                SortButton(sortOrder = sortOrder, onClick = onCycleSort)
            }
            Text(
                text = "Hold OK or press ☰ on an app to favorite, rename, hide or move it",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = cardWidth),
                modifier = Modifier.fillMaxWidth().weight(1f),
                // Room for the focused card's 1.1x zoom and glow
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                itemsIndexed(apps, key = { _, app -> app.uniqueKey }) { index, app ->
                    Box(contentAlignment = Alignment.Center) {
                        AppCard(
                            app = app,
                            isFavorite = app.packageName in favorites,
                            modifier = if (index == 0) Modifier.focusRequester(firstItem) else Modifier,
                            onFocus = { onAppFocused(app) },
                            onClick = { onAppClicked(app) },
                            onLongClick = { onAppLongClicked(app) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SortButton(sortOrder: AppSortOrder, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), shape = shape),
            focusedBorder = Border(BorderStroke(1.5.dp, Color.White), shape = shape)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DarkSurface,
            focusedContainerColor = DarkSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, tint = FireAmber, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sort: ${sortOrder.title}", fontSize = 12.sp, color = TextPrimary)
        }
    }
}
