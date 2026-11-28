package com.knightmstr.onyxtv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ChildCare
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
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.ui.unit.dp
import com.knightmstr.onyxtv.data.AppModel
import com.knightmstr.onyxtv.ui.theme.FavoriteYellow
import com.knightmstr.onyxtv.ui.theme.FireAmber
import com.knightmstr.onyxtv.ui.theme.FireOrange
import com.knightmstr.onyxtv.ui.theme.TextSecondary

/**
 * App options, opened by long-pressing OK on a card or pressing the remote's ☰ Menu button.
 *
 * Move left/right keep the menu open so an app can be nudged several places in a row.
 * Moving within All Apps switches the sort to "My Order".
 */
@Composable
fun AppContextMenuDialog(
    app: AppModel,
    isFavorite: Boolean,
    favoriteIndex: Int,
    favoriteCount: Int,
    allAppsIndex: Int,
    allAppsCount: Int,
    onLaunchApp: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveFavorite: (delta: Int) -> Unit,
    onMoveInAllApps: (delta: Int) -> Unit,
    onToggleKidsAllowed: () -> Unit,
    onRename: () -> Unit,
    onToggleHidden: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onDismiss: () -> Unit,
    /** Name of the dock folder this app is in, if any. */
    folderName: String? = null,
    showFolderOptions: Boolean = true,
    onMoveToFolder: () -> Unit = {},
    onTakeOutOfFolder: () -> Unit = {}
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
            MoveRow("Favorites", favoriteIndex, favoriteCount, FavoriteYellow, onMoveFavorite)
        }

        if (allAppsIndex >= 0 && allAppsCount > 1) {
            MoveRow("All Apps", allAppsIndex, allAppsCount, Color(0xFF60A5FA), onMoveInAllApps)
        }

        if (showFolderOptions && folderName != null) {
            TvMenuItem(
                title = "Take out of $folderName",
                subtitle = "Back into the dock, next to the folder",
                icon = Icons.Rounded.FolderOpen,
                iconTint = Color(0xFF34D399),
                onClick = { onTakeOutOfFolder(); onDismiss() }
            )
        }
        if (showFolderOptions) TvMenuItem(
            title = if (folderName != null) "Move to another folder" else "Move to a folder",
            subtitle = "Group apps into one tile in the dock",
            icon = Icons.Rounded.CreateNewFolder,
            iconTint = Color(0xFF34D399),
            onClick = { onMoveToFolder(); onDismiss() }
        )

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
            title = if (app.isKidsAllowed) "Remove from Kids mode" else "Allow in Kids mode",
            subtitle = if (app.isKidsAllowed) "Kids mode won't show this app" else "Show this app while Kids mode is on",
            icon = Icons.Rounded.ChildCare,
            iconTint = Color(0xFFF472B6),
            onClick = { onToggleKidsAllowed(); onDismiss() }
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

/** "Move left / Move right" pair for one row. Keeps the menu open so it can be pressed repeatedly. */
@Composable
internal fun MoveRow(rowName: String, index: Int, count: Int, tint: Color, onMove: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TvMenuItem(
            title = "$rowName: left",
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            iconTint = tint,
            enabled = index > 0,
            modifier = Modifier.weight(1f),
            onClick = { onMove(-1) }
        )
        TvMenuItem(
            title = "$rowName: right",
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            iconTint = tint,
            enabled = index in 0 until count - 1,
            modifier = Modifier.weight(1f),
            onClick = { onMove(1) }
        )
    }
}
