package com.knightmstr.onyxtv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.knightmstr.onyxtv.ui.theme.DarkSurface
import com.knightmstr.onyxtv.ui.theme.DarkSurfaceVariant
import com.knightmstr.onyxtv.ui.theme.TextPrimary
import com.knightmstr.onyxtv.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Full-screen dimmed dialog with a centred, scrollable panel. Shared by every launcher dialog
 * (app options, rename, settings) so they look and behave the same with the D-pad.
 * Dismissed with the remote's Back button.
 */
@Composable
fun TvDialogFrame(
    title: String,
    subtitle: String?,
    accentColor: Color,
    onDismiss: () -> Unit,
    width: Dp = 480.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val panelShape = RoundedCornerShape(20.dp)

    // Opening a dialog with a long-press of OK leaves the key physically held down. Its release
    // would then "click" whatever the dialog focused first (e.g. Open). So ignore OK/Enter key-ups
    // until this dialog has seen a fresh key-down of its own.
    val confirmArmed = remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xDD07090F))
                .onPreviewKeyEvent { event -> swallowCarriedOverConfirm(event, confirmArmed) },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(width)
                    .heightIn(max = 480.dp)
                    .clip(panelShape)
                    .background(Color(0xFF141824))
                    .border(BorderStroke(1.5.dp, accentColor.copy(alpha = 0.6f)), panelShape)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.size(4.dp))
                content()
            }
        }
    }
}

private val CONFIRM_KEYS = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.ButtonA)

/** Returns true (consume) for a confirm-key release whose press happened before the dialog opened. */
private fun swallowCarriedOverConfirm(event: KeyEvent, armed: MutableState<Boolean>): Boolean {
    if (event.key !in CONFIRM_KEYS) return false
    return when (event.type) {
        KeyEventType.KeyDown -> {
            // Auto-repeat downs from the original long-press don't count as a new press
            if (event.nativeKeyEvent.repeatCount == 0) armed.value = true
            !armed.value
        }
        KeyEventType.KeyUp -> !armed.value
        else -> false
    }
}

/** Moves D-pad focus into a dialog as soon as it opens. */
@Composable
fun rememberInitialFocus(): FocusRequester {
    val requester = remember { FocusRequester() }
    LaunchedEffect(requester) {
        delay(50)
        runCatching { requester.requestFocus() }
    }
    return requester
}

/**
 * A focusable menu row: tinted icon, title, optional subtitle and an optional trailing value
 * (used by settings to show the current option, e.g. "10 min").
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMenuItem(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: String? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape = shape),
            focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = shape)
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(elevationColor = iconTint.copy(alpha = 0.4f), elevation = 10.dp)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DarkSurface,
            focusedContainerColor = DarkSurfaceVariant,
            disabledContainerColor = DarkSurface.copy(alpha = 0.5f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = if (enabled) 0.15f else 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) iconTint else iconTint.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) TextPrimary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = trailing,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconTint
                )
            }
        }
    }
}

/**
 * A settings row with a slider: focus it, then press Left / Right on the remote to change the
 * value in [step]s. Changes apply straight away so the result can be seen behind the dialog.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSliderItem(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    value: Int,
    range: IntRange,
    step: Int,
    valueLabel: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val shape = RoundedCornerShape(12.dp)
    var focused by remember { mutableStateOf(false) }
    val fraction = ((value - range.first).toFloat() / (range.last - range.first)).coerceIn(0f, 1f)

    Surface(
        onClick = {},
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { onValueChange((value - step).coerceAtLeast(range.first)); true }
                    Key.DirectionRight -> { onValueChange((value + step).coerceAtMost(range.last)); true }
                    else -> false
                }
            },
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape = shape),
            focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = shape)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DarkSurface,
            focusedContainerColor = DarkSurfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(
                        text = if (focused) "◀ ▶ to adjust" else subtitle.orEmpty(),
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
                Text(valueLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = iconTint)
            }
            // Track
            Box(
                modifier = Modifier
                    .padding(start = 48.dp, top = 10.dp)
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(iconTint)
                )
            }
        }
    }
}
