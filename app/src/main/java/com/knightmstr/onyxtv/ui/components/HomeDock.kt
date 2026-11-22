package com.knightmstr.onyxtv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.knightmstr.onyxtv.data.AppModel
import com.knightmstr.onyxtv.ui.theme.FireAmber
import com.knightmstr.onyxtv.ui.theme.FireOrange
import com.knightmstr.onyxtv.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/** How long the "Hold OK…" hint stays after moving to a tile before it fades away. */
private const val HINT_VISIBLE_MS = 3500L

/**
 * The dock along the bottom of the home (after couch-tv's Fire TV layout): favourites on a soft
 * translucent bar, a round All Apps button at the end, and the focused app's name underneath.
 * Scrolls sideways when there are more favourites than fit.
 */
@Composable
fun HomeDock(
    apps: List<AppModel>,
    allAppsTile: AppModel,
    favorites: Set<String>,
    focusedApp: AppModel?,
    hint: String?,
    firstItemFocusRequester: FocusRequester,
    onAppFocused: (AppModel) -> Unit,
    onAppClicked: (AppModel) -> Unit,
    onAppLongClicked: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val edge = LocalScreenEdge.current
    val maxWidth = LocalConfiguration.current.screenWidthDp.dp - edge * 2
    val tileHeight = LocalCardWidth.current * 9f / 16f
    val focusedInDock = focusedApp?.takeIf { f ->
        f.uniqueKey == allAppsTile.uniqueKey || apps.any { it.uniqueKey == f.uniqueKey }
    }

    // The hint shows for a few seconds each time focus lands on a new tile, then fades
    var hintVisible by remember { mutableStateOf(true) }
    LaunchedEffect(focusedInDock?.uniqueKey) {
        hintVisible = true
        delay(HINT_VISIBLE_MS)
        hintVisible = false
    }
    val hintAlpha by animateFloatAsState(if (hintVisible) 1f else 0f, tween(600), label = "dockHint")

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                // Barely-there bar: the tiles stand on the wallpaper, the bar just groups them
                .background(Color(0x14000000), RoundedCornerShape(26.dp))
                .padding(horizontal = 6.dp)
        ) {
            LazyRow(
                // Room for the focused tile's zoom and glow inside the bar
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy((LocalCardWidth.current.value / 10f).coerceAtLeast(12f).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(apps, key = { _, app -> "dock_${app.uniqueKey}" }) { index, app ->
                    AppCard(
                        app = app,
                        // Everything in the dock is a favourite, so no favourite dots here
                        isFavorite = false,
                        modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
                        onFocus = { onAppFocused(app) },
                        onClick = { onAppClicked(app) },
                        onLongClick = { onAppLongClicked(app) }
                    )
                }
                item(key = "dock_all_apps") {
                    AllAppsButton(
                        size = tileHeight,
                        modifier = if (apps.isEmpty()) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
                        onFocus = { onAppFocused(allAppsTile) },
                        onClick = { onAppClicked(allAppsTile) }
                    )
                }
            }
        }

        // Focused app's name, and a short hint that fades; fixed height so the dock never jumps
        Column(
            modifier = Modifier.height(44.dp).padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = focusedInDock?.label.orEmpty(),
                style = TextStyle(shadow = Shadow(Color(0x99000000), blurRadius = 12f)),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (focusedInDock != null && hint != null) {
                Text(
                    text = hint,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    modifier = Modifier.alpha(hintAlpha)
                )
            }
        }
    }
}

/** Round "more apps" button at the end of the dock, so it doesn't look like one of your apps. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AllAppsButton(
    size: androidx.compose.ui.unit.Dp,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .onFocusChanged { if (it.isFocused) onFocus() },
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.12f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)), shape = CircleShape),
            focusedBorder = Border(BorderStroke(2.5.dp, Color.White), shape = CircleShape)
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(elevationColor = FireOrange.copy(alpha = 0.6f), elevation = 14.dp)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.10f),
            focusedContainerColor = Color.White.copy(alpha = 0.22f)
        )
    ) {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = "All Apps",
                tint = FireAmber,
                modifier = Modifier.size(size * 0.42f)
            )
        }
    }
}
