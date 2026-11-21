package com.example.firelauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.firelauncher.data.AppModel
import com.example.firelauncher.ui.theme.FavoriteYellow
import com.example.firelauncher.ui.theme.FireAmber
import com.example.firelauncher.ui.theme.FireOrange
import com.example.firelauncher.ui.theme.TextSecondary

/**
 * App options, opened by long-pressing OK on a card or pressing the remote's ☰ Menu button.
 *
 * Move left/right keep the menu open so a favourite can be nudged several places in a row.
 */
@Composable
fun AppContextMenuDialog(
    app: AppModel,
    isFavorite: Boolean,
    favoriteIndex: Int,
    favoriteCount: Int,
    onLaunchApp: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveFavorite: (delta: Int) -> Unit,
    onRename: () -> Unit,
    onToggleHidden: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onDismiss: () -> Unit
) {
    val firstItem = rememberInitialFocus()
    val subtitle = if (app.isRenamed) "${app.originalLabel} • ${app.packageName}" else app.packageName

    TvDialogFrame(
        title = app.label,
        subtitle = subtitle,
        accentColor = Color(app.accentColor),
        onDismiss = onDismiss
    ) {
        TvMenuItem(
            title = "Open",
            icon = Icons.Rounded.PlayArrow,
            iconTint = FireAmber,
            modifier = Modifier.focusRequester(firstItem),
            onClick = { onLaunchApp(); onDismiss() }
        )

        TvMenuItem(
            title = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
            subtitle = if (isFavorite) null else "Pin to the top row",
            icon = if (isFavorite) Icons.Rounded.StarOutline else Icons.Rounded.Star,
            iconTint = FavoriteYellow,
            onClick = { onToggleFavorite(); onDismiss() }
        )

        if (isFavorite && favoriteCount > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TvMenuItem(
                    title = "Move left",
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    iconTint = FavoriteYellow,
                    enabled = favoriteIndex > 0,
                    modifier = Modifier.weight(1f),
                    onClick = { onMoveFavorite(-1) }
                )
                TvMenuItem(
                    title = "Move right",
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    iconTint = FavoriteYellow,
                    enabled = favoriteIndex in 0 until favoriteCount - 1,
                    modifier = Modifier.weight(1f),
                    onClick = { onMoveFavorite(1) }
                )
            }
        }

        TvMenuItem(
            title = "Rename",
            subtitle = if (app.isRenamed) "Currently renamed from ${app.originalLabel}" else "Give this app a shorter name",
            icon = Icons.Rounded.Edit,
            iconTint = Color(0xFF60A5FA),
            onClick = { onRename(); onDismiss() }
        )

        TvMenuItem(
            title = if (app.isHidden) "Unhide app" else "Hide app",
            subtitle = if (app.isHidden) "Show it in All Apps again" else "Restore later from Launcher Settings",
            icon = if (app.isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
            iconTint = TextSecondary,
            onClick = { onToggleHidden(); onDismiss() }
        )

        TvMenuItem(
            title = "Force Stop & App Info",
            subtitle = "Fire OS settings: force stop, clear cache, manage data",
            icon = Icons.Rounded.Settings,
            iconTint = FireOrange,
            onClick = { onOpenAppDetails(); onDismiss() }
        )
    }
}
