package com.example.firelauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.firelauncher.data.AppModel
import com.example.firelauncher.data.AppSortOrder
import com.example.firelauncher.ui.theme.DarkSurface
import com.example.firelauncher.ui.theme.DarkSurfaceVariant
import com.example.firelauncher.ui.theme.FireAmber
import com.example.firelauncher.ui.theme.TextPrimary
import com.example.firelauncher.ui.theme.TextSecondary

/**
 * A horizontal row of apps with a category header title (e.g. "Favorites", "All Apps", "System")
 * and an optional interactive 'Sort by' trigger menu for the app library.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppRow(
    title: String,
    apps: List<AppModel>,
    favorites: Set<String>,
    modifier: Modifier = Modifier,
    sortOrder: AppSortOrder? = null,
    onOpenSortMenu: (() -> Unit)? = null,
    firstItemFocusRequester: FocusRequester? = null,
    onAppFocused: (AppModel) -> Unit,
    onAppClicked: (AppModel) -> Unit,
    onAppLongClicked: (AppModel) -> Unit
) {
    if (apps.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            if (onOpenSortMenu != null && sortOrder != null) {
                val pillShape = RoundedCornerShape(14.dp)
                Surface(
                    onClick = onOpenSortMenu,
                    shape = ClickableSurfaceDefaults.shape(shape = pillShape),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
                    border = ClickableSurfaceDefaults.border(
                        border = Border(
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            shape = pillShape
                        ),
                        focusedBorder = Border(
                            border = BorderStroke(1.5.dp, Color.White),
                            shape = pillShape
                        )
                    ),
                    glow = ClickableSurfaceDefaults.glow(
                        focusedGlow = Glow(
                            elevationColor = FireAmber.copy(alpha = 0.6f),
                            elevation = 8.dp
                        )
                    ),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = DarkSurface.copy(alpha = 0.8f),
                        focusedContainerColor = DarkSurfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.Sort,
                            contentDescription = "Change sort",
                            tint = FireAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = when (sortOrder) {
                                AppSortOrder.ALPHABETICAL -> "Sort: A–Z ▾"
                                AppSortOrder.MOST_RECENT -> "Sort: Recent ▾"
                                AppSortOrder.MOST_FREQUENT -> "Sort: Frequent ▾"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        TvLazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            itemsIndexed(
                items = apps,
                key = { _, app -> "${title}_${app.uniqueKey}" }
            ) { index, app ->
                val cardModifier = if (index == 0 && firstItemFocusRequester != null) {
                    Modifier.focusRequester(firstItemFocusRequester)
                } else {
                    Modifier
                }

                AppCard(
                    app = app,
                    isFavorite = favorites.contains(app.packageName),
                    modifier = cardModifier,
                    onFocus = { onAppFocused(app) },
                    onClick = { onAppClicked(app) },
                    onLongClick = { onAppLongClicked(app) }
                )
            }
        }
    }
}
