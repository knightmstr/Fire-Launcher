package com.knightmstr.onyxtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

/** One choice in a [TvChoiceItem]. [description] shows in the row while the choice is highlighted. */
data class TvChoice<T>(val value: T, val label: String, val description: String? = null)

/**
 * A settings row with a small drop-down: OK opens a compact list right under the current value,
 * Up/Down moves, OK picks, Back closes without changing anything. The row's description follows
 * the highlighted choice, so the list itself stays as small as its text.
 */
@Composable
fun <T> TvChoiceItem(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    choices: List<TvChoice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    /** Shown instead of the selected choice's label (e.g. "42 min left"). */
    currentLabel: String? = null
) {
    var open by remember { mutableStateOf(false) }
    var highlighted by remember { mutableStateOf(selected) }
    var rowHeight by remember { mutableIntStateOf(0) }
    val rowFocus = remember { FocusRequester() }
    val density = LocalDensity.current
    val current = choices.firstOrNull { it.value == selected }

    fun close(pick: T?) {
        open = false
        if (pick != null && pick != selected) onSelect(pick)
        runCatching { rowFocus.requestFocus() }
    }

    Box(modifier = Modifier.fillMaxWidth().onSizeChanged { rowHeight = it.height }) {
        TvMenuItem(
            title = title,
            subtitle = if (open) choices.firstOrNull { it.value == highlighted }?.description ?: subtitle else subtitle,
            icon = icon,
            iconTint = iconTint,
            trailing = currentLabel ?: current?.label ?: "",
            trailingIcon = if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            enabled = enabled,
            modifier = modifier.focusRequester(rowFocus),
            onClick = {
                highlighted = selected
                open = true
            }
        )
        if (open) {
            val gap = with(density) { 4.dp.roundToPx() }
            val inset = with(density) { 14.dp.roundToPx() }
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(-inset, rowHeight - gap * 3),
                onDismissRequest = { close(null) },
                properties = PopupProperties(focusable = true)
            ) {
                ChoiceList(
                    choices = choices,
                    selected = selected,
                    accent = iconTint,
                    onHighlight = { highlighted = it },
                    onPick = { close(it) },
                    onBack = { close(null) }
                )
            }
        }
    }
}

/**
 * The compact list on its own, for anchoring under any control (e.g. the All Apps sort button).
 * Place it inside a Box with the anchor; [anchorHeightPx] puts it just below.
 */
@Composable
fun <T> TvChoicePopup(
    choices: List<TvChoice<T>>,
    selected: T,
    accent: Color,
    anchorHeightPx: Int,
    onHighlight: (T) -> Unit = {},
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
    alignment: Alignment = Alignment.TopEnd
) {
    val gap = with(LocalDensity.current) { 4.dp.roundToPx() }
    Popup(
        alignment = alignment,
        offset = IntOffset(0, anchorHeightPx + gap),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        ChoiceList(choices, selected, accent, onHighlight, onPick, onDismiss)
    }
}

@Composable
private fun <T> ChoiceList(
    choices: List<TvChoice<T>>,
    selected: T,
    accent: Color,
    onHighlight: (T) -> Unit,
    onPick: (T) -> Unit,
    onBack: () -> Unit
) {
    val requesters = remember(choices.size) { List(choices.size) { FocusRequester() } }
    LaunchedEffect(Unit) {
        delay(30)
        val i = choices.indexOfFirst { it.value == selected }.coerceAtLeast(0)
        runCatching { requesters[i].requestFocus() }
    }
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .background(Color(0xFF0F172A), shape)
            .border(1.dp, accent.copy(alpha = 0.7f), shape)
            .padding(3.dp)
    ) {
        choices.forEachIndexed { i, choice ->
            var focused by remember { mutableStateOf(false) }
            val isCurrent = choice.value == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(requesters[i])
                    .onFocusChanged {
                        focused = it.isFocused
                        if (it.isFocused) onHighlight(choice.value)
                    }
                    .onPreviewKeyEvent { e ->
                        val ok = e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter
                        when {
                            ok && e.type == KeyEventType.KeyUp -> { onPick(choice.value); true }
                            ok -> true
                            e.key == Key.Back && e.type == KeyEventType.KeyUp -> { onBack(); true }
                            e.key == Key.Back -> true
                            // Keep focus inside the list
                            e.key == Key.DirectionLeft || e.key == Key.DirectionRight -> true
                            e.key == Key.DirectionUp && i == 0 -> true
                            e.key == Key.DirectionDown && i == choices.lastIndex -> true
                            else -> false
                        }
                    }
                    .focusable()
                    .background(if (focused) accent else Color.Transparent, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
                    if (isCurrent) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Current",
                            tint = if (focused) Color(0xFF04342C) else accent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = choice.label,
                    fontSize = 13.sp,
                    fontWeight = if (isCurrent || focused) FontWeight.Bold else FontWeight.Medium,
                    color = if (focused) Color(0xFF04342C) else Color(0xFFE2E8F0),
                    maxLines = 1
                )
            }
        }
    }
}
