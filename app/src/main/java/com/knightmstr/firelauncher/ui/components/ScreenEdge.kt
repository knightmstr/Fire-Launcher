package com.knightmstr.firelauncher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Side margin for text and the first card in each row. Rows themselves run edge to edge, so the
 * home fills the whole TV; only their starting position uses this margin.
 */
val LocalScreenEdge = compositionLocalOf { 32.dp }

/** 3.5% of the screen width (≈34dp on a 1080p TV), kept between 24dp and 64dp. */
@Composable
fun rememberScreenEdge(): Dp {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return remember(widthDp) { (widthDp * 0.035f).dp.coerceIn(24.dp, 64.dp) }
}

/**
 * When D-pad focus moves along an edge-to-edge row, keep the focused card at least
 * [LocalScreenEdge] away from the screen edges instead of letting it sit flush against them.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeepFocusInsideEdges(content: @Composable () -> Unit) {
    val edgePx = with(LocalDensity.current) { LocalScreenEdge.current.toPx() }
    val spec = remember(edgePx) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                val beforeStart = offset - edgePx                           // < 0: too far left
                val pastEnd = offset + size - (containerSize - edgePx)      // > 0: too far right
                return when {
                    size > containerSize - 2 * edgePx -> beforeStart          // bigger than the space: align start
                    beforeStart < 0 -> beforeStart
                    pastEnd > 0 -> pastEnd
                    else -> 0f
                }
            }
        }
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec, content = content)
}
