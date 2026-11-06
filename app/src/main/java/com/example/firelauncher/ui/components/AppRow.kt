package com.example.firelauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.firelauncher.data.AppModel
import com.example.firelauncher.ui.theme.TextPrimary

/**
 * A horizontal row of apps with a category header title (e.g. "Favorites", "All Apps", "System").
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppRow(
    title: String,
    apps: List<AppModel>,
    favorites: Set<String>,
    modifier: Modifier = Modifier,
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
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

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
