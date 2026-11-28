package com.knightmstr.onyxtv.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.knightmstr.onyxtv.kids.KidsTime
import com.knightmstr.onyxtv.ui.theme.TextSecondary

private val KidsPink = Color(0xFFF472B6)

/** Shown over the home screen in Kids mode when screen time is used up or it's bedtime. */
@Composable
fun KidsLockDialog(lock: KidsTime.Lock, onParent: () -> Unit) {
    val first = rememberInitialFocus()
    TvDialogFrame(
        title = if (lock == KidsTime.Lock.BEDTIME) "It's bedtime" else "Time's up for today",
        subtitle = if (lock == KidsTime.Lock.BEDTIME) "The TV will be ready again in the morning."
        else "That's all the screen time for today. See you tomorrow!",
        accentColor = KidsPink,
        // Back does nothing here: only a parent can close this
        onDismiss = {},
        width = 520.dp
    ) {
        Text(
            "A parent can allow more time with the Kids mode PIN.",
            fontSize = 13.sp,
            color = TextSecondary
        )
        TvMenuItem(
            title = "Parent: enter PIN",
            icon = Icons.Rounded.Lock,
            iconTint = KidsPink,
            modifier = Modifier.focusRequester(first),
            onClick = onParent
        )
    }
}

/** After the parent's PIN on the lock screen. */
@Composable
fun KidsExtendDialog(
    onExtend: (minutes: Int) -> Unit,
    onLeaveKidsMode: () -> Unit,
    onDismiss: () -> Unit
) {
    val first = rememberInitialFocus()
    TvDialogFrame(
        title = "Allow more time?",
        subtitle = "Extra time also works during bedtime.",
        accentColor = KidsPink,
        onDismiss = onDismiss
    ) {
        TvMenuItem(
            title = "15 more minutes",
            icon = Icons.Rounded.Add,
            iconTint = KidsPink,
            modifier = Modifier.focusRequester(first),
            onClick = { onExtend(15) }
        )
        TvMenuItem(title = "30 more minutes", icon = Icons.Rounded.Add, iconTint = KidsPink, onClick = { onExtend(30) })
        TvMenuItem(title = "1 more hour", icon = Icons.Rounded.Add, iconTint = KidsPink, onClick = { onExtend(60) })
        TvMenuItem(
            title = "Leave Kids mode",
            subtitle = "Back to the normal home screen",
            icon = Icons.AutoMirrored.Rounded.ExitToApp,
            iconTint = Color(0xFF9CA3AF),
            onClick = onLeaveKidsMode
        )
    }
}
