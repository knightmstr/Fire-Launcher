package com.knightmstr.firelauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.knightmstr.firelauncher.data.PinHasher
import com.knightmstr.firelauncher.ui.theme.DarkSurface
import com.knightmstr.firelauncher.ui.theme.DarkSurfaceVariant
import com.knightmstr.firelauncher.ui.theme.TextPrimary
import com.knightmstr.firelauncher.ui.theme.TextSecondary

private val KidsAccent = Color(0xFFF472B6)

/**
 * D-pad PIN keypad (the Fire TV remote has no number keys). Calls [onComplete]
 * as soon as [PinHasher.PIN_LENGTH] digits are entered.
 */
@Composable
fun PinEntryDialog(
    title: String,
    subtitle: String?,
    error: String?,
    onComplete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var entered by remember { mutableStateOf("") }
    val firstKey = rememberInitialFocus()

    fun press(key: String) {
        when (key) {
            "⌫" -> entered = entered.dropLast(1)
            else -> if (entered.length < PinHasher.PIN_LENGTH) {
                entered += key
                if (entered.length == PinHasher.PIN_LENGTH) {
                    val pin = entered
                    entered = ""
                    onComplete(pin)
                }
            }
        }
    }

    TvDialogFrame(title = title, subtitle = subtitle, accentColor = KidsAccent, onDismiss = onDismiss, width = 360.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().height(28.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(PinHasher.PIN_LENGTH) { i ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (i < entered.length) KidsAccent else DarkSurfaceVariant)
                )
            }
        }
        Text(
            text = error ?: " ",
            fontSize = 12.sp,
            color = Color(0xFFF87171),
            modifier = Modifier.fillMaxWidth()
        )
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Box(modifier = Modifier.width(76.dp))
                        } else {
                            PinKey(
                                key = key,
                                modifier = if (key == "1") Modifier.focusRequester(firstKey) else Modifier,
                                onClick = { press(key) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Two-step "enter, then confirm" PIN creation. */
@Composable
fun CreatePinDialog(onCreated: (String) -> Unit, onDismiss: () -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    PinEntryDialog(
        title = if (first == null) "Choose a Kids mode PIN" else "Enter the PIN again",
        subtitle = "${PinHasher.PIN_LENGTH} digits. Needed to leave Kids mode.",
        error = error,
        onComplete = { pin ->
            val firstPin = first
            when {
                firstPin == null -> { first = pin; error = null }
                firstPin == pin -> onCreated(pin)
                else -> { first = null; error = "PINs didn't match, start again" }
            }
        },
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PinKey(key: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        modifier = modifier.width(76.dp).height(48.dp),
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = shape)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DarkSurface,
            focusedContainerColor = KidsAccent.copy(alpha = 0.35f)
        )
    ) {
        Box(modifier = Modifier.size(76.dp, 48.dp), contentAlignment = Alignment.Center) {
            if (key == "⌫") {
                Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Delete", tint = TextSecondary)
            } else {
                Text(text = key, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
        }
    }
}
