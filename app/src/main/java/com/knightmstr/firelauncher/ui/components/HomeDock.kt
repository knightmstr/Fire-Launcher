package com.knightmstr.firelauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.knightmstr.firelauncher.data.AppModel
import com.knightmstr.firelauncher.ui.theme.TextSecondary

/**
 * The dock along the bottom of the home (after couch-tv's Fire TV layout): favourites with the
 * All Apps tile at the end, on a soft translucent bar, and the focused app's name underneath.
 * Scrolls sideways when there are more favourites than fit.
 */
@Composable
fun HomeDock(
    apps: List<AppModel>,
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
    val maxWidth = (LocalConfiguration.current.screenWidthDp.dp - edge * 2)
    val dockShape = RoundedCornerShape(26.dp)
    val focusedInDock = focusedApp?.takeIf { f -> apps.any { it.uniqueKey == f.uniqueKey } }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .background(Color(0x8C0B0F19), dockShape)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), dockShape)
                .padding(horizontal = 6.dp)
        ) {
            LazyRow(
                // Room for the focused tile's zoom and glow inside the bar
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy((LocalCardWidth.current.value / 10f).coerceAtLeast(12f).dp)
            ) {
                itemsIndexed(apps, key = { _, app -> "dock_${app.uniqueKey}" }) { index, app ->
                    AppCard(
                        app = app,
                        isFavorite = app.packageName in favorites,
                        modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
                        onFocus = { onAppFocused(app) },
                        onClick = { onAppClicked(app) },
                        onLongClick = { onAppLongClicked(app) }
                    )
                }
            }
        }

        // Focused app's name and a short hint, in a fixed-height slot so the dock never jumps
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
                Text(text = hint, fontSize = 10.sp, color = TextSecondary, maxLines = 1)
            }
        }
    }
}
