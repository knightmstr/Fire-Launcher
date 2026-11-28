package com.knightmstr.onyxtv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Link
import androidx.compose.runtime.Composable
import com.knightmstr.onyxtv.data.WallpaperLinks
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.knightmstr.onyxtv.ui.WallpaperUrlStatus
import com.knightmstr.onyxtv.ui.theme.DarkSurfaceVariant
import com.knightmstr.onyxtv.ui.theme.FireAmber
import com.knightmstr.onyxtv.ui.theme.FireOrange
import com.knightmstr.onyxtv.ui.theme.TextPrimary
import com.knightmstr.onyxtv.ui.theme.TextSecondary

/** Add or remove image URLs used as wallpapers. Each added link is checked before it's saved. */
@Composable
fun WallpaperUrlsDialog(
    urls: List<String>,
    status: WallpaperUrlStatus,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val fieldFocus = rememberInitialFocus()
    val fieldShape = RoundedCornerShape(12.dp)

    // Clear the field after a link is accepted.
    LaunchedEffect(status) {
        if (status is WallpaperUrlStatus.Added) text = ""
    }

    TvDialogFrame(
        title = "Photo links",
        subtitle = "A picture link, a Drive photo or folder, Dropbox / OneDrive, a Reddit post, or a subreddit like r/EarthPorn",
        accentColor = Color(0xFF34D399),
        onDismiss = onDismiss,
        width = 580.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceVariant, fieldShape)
                .border(BorderStroke(1.5.dp, FireOrange.copy(alpha = 0.7f)), fieldShape)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it.take(500) },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(FireOrange),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onAdd(text) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(fieldFocus)
            )
        }

        val statusText: String? = when (status) {
            is WallpaperUrlStatus.Checking -> "Checking the link…"
            is WallpaperUrlStatus.Added -> "Added ${status.what}."
            is WallpaperUrlStatus.Failed -> status.message
            WallpaperUrlStatus.Idle -> null
        }
        if (statusText != null) {
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = if (status is WallpaperUrlStatus.Failed) Color(0xFFFBBF24) else TextSecondary
            )
        }

        TvMenuItem(
            title = "Add link",
            icon = Icons.Rounded.Add,
            iconTint = FireAmber,
            enabled = text.isNotBlank() && status !is WallpaperUrlStatus.Checking,
            onClick = { onAdd(text) }
        )

        Text(
            text = "You're responsible for the links you add. Onyx TV shows whatever the link points to and doesn't check it for safety.",
            fontSize = 11.sp,
            color = Color(0xFF9CA3AF)
        )

        if (urls.isEmpty()) {
            Text(
                text = "No links yet.",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            urls.forEach { url ->
                val subreddit = WallpaperLinks.subredditOf(url)
                val folder = WallpaperLinks.driveFolderOf(url)
                TvMenuItem(
                    title = when {
                        subreddit != null -> "r/$subreddit"
                        folder != null -> "Google Drive folder"
                        else -> url
                    },
                    subtitle = when {
                        subreddit != null -> "New pictures from this subreddit"
                        folder != null -> "Every photo in it, including ones you add later"
                        else -> null
                    },
                    icon = Icons.Rounded.Link,
                    iconTint = TextSecondary,
                    trailing = "Remove",
                    onClick = { onRemove(url) }
                )
            }
        }
    }
}
