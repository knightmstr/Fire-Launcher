package com.knightmstr.onyxtv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.knightmstr.onyxtv.data.AppFolder
import com.knightmstr.onyxtv.data.AppModel
import com.knightmstr.onyxtv.ui.theme.TextSecondary

private val FolderGreen = Color(0xFF34D399)

/** A dock folder opened: its apps as tiles. OK opens one; hold OK for its menu. */
@Composable
fun FolderDialog(
    folder: AppModel,
    onAppClicked: (AppModel) -> Unit,
    onAppLongClicked: (AppModel) -> Unit,
    onDismiss: () -> Unit
) {
    val first = rememberInitialFocus()
    TvDialogFrame(
        title = folder.label,
        subtitle = "${folder.folderApps.size} app${if (folder.folderApps.size == 1) "" else "s"} · hold OK on one for options",
        accentColor = Color(folder.accentColor),
        onDismiss = onDismiss,
        width = 720.dp
    ) {
        // Tiles at a fixed size here, whatever the dock's tile size is set to
        CompositionLocalProvider(LocalCardWidth provides 150.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                folder.folderApps.chunked(4).forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEachIndexed { i, app ->
                            AppCard(
                                app = app,
                                isFavorite = false,
                                modifier = if (rowIndex == 0 && i == 0) Modifier.focusRequester(first) else Modifier,
                                onClick = { onAppClicked(app) },
                                onLongClick = { onAppLongClicked(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Options for a folder tile in the dock. */
@Composable
fun FolderMenuDialog(
    folder: AppModel,
    dockIndex: Int,
    dockCount: Int,
    onOpen: () -> Unit,
    onMove: (Int) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val first = rememberInitialFocus()
    TvDialogFrame(
        title = folder.label,
        subtitle = "Folder · ${folder.folderApps.joinToString(", ") { it.label }}".take(90),
        accentColor = FolderGreen,
        onDismiss = onDismiss
    ) {
        TvMenuItem(
            title = "Open",
            icon = Icons.Rounded.FolderOpen,
            iconTint = FolderGreen,
            modifier = Modifier.focusRequester(first),
            onClick = { onOpen(); onDismiss() }
        )
        if (dockCount > 1) MoveRow("Dock", dockIndex, dockCount, FolderGreen, onMove)
        TvMenuItem(
            title = "Rename folder",
            icon = Icons.Rounded.Edit,
            iconTint = Color(0xFF60A5FA),
            onClick = { onRename(); onDismiss() }
        )
        TvMenuItem(
            title = "Remove folder",
            subtitle = "Its apps go back into the dock where the folder was",
            icon = Icons.Rounded.DeleteOutline,
            iconTint = Color(0xFFF87171),
            onClick = { onDelete(); onDismiss() }
        )
    }
}

/** "Move to a folder": pick an existing folder or start a new one. */
@Composable
fun FolderPickerDialog(
    appLabel: String,
    folders: List<AppFolder>,
    currentFolderId: String?,
    onNewFolder: () -> Unit,
    onPick: (AppFolder) -> Unit,
    onDismiss: () -> Unit
) {
    val first = rememberInitialFocus()
    TvDialogFrame(
        title = "Move $appLabel to…",
        subtitle = "It leaves the dock and sits inside the folder. It stays in All Apps.",
        accentColor = FolderGreen,
        onDismiss = onDismiss
    ) {
        TvMenuItem(
            title = "New folder",
            subtitle = "Rename it afterwards: hold OK on the folder",
            icon = Icons.Rounded.CreateNewFolder,
            iconTint = FolderGreen,
            modifier = Modifier.focusRequester(first),
            onClick = onNewFolder
        )
        val others = folders.filter { it.id != currentFolderId }
        if (others.isEmpty() && folders.isNotEmpty()) {
            Text("No other folders yet.", fontSize = 12.sp, color = TextSecondary)
        }
        others.forEach { f ->
            TvMenuItem(
                title = f.name,
                subtitle = "${f.packages.size} app${if (f.packages.size == 1) "" else "s"}",
                icon = Icons.Rounded.Folder,
                iconTint = FolderGreen,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onPick(f) }
            )
        }
    }
}
