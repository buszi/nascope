package io.buszi.nascope.ui.dashboard.widgets

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How many rows of [rowHeight], separated by [spacing], fit into [availableHeight].
 *
 * Fixed-height bricks use this instead of a hardcoded top-N count, so a taller brick slot
 * renders more rows instead of hiding content behind an overflow summary.
 */
internal fun rowsThatFit(availableHeight: Dp, rowHeight: Dp, spacing: Dp): Int {
    if (availableHeight <= 0.dp || rowHeight <= 0.dp) return 0
    return ((availableHeight + spacing) / (rowHeight + spacing)).toInt().coerceAtLeast(0)
}

/**
 * Split of a list into the rows actually rendered and the remainder summarised as "+N MORE".
 */
internal data class PreviewRowSplit(
    val shown: Int,
    val overflow: Int,
)

/**
 * Fits [totalItems] into [capacity] rows, giving up one row for the overflow label when the
 * list does not fit entirely.
 */
internal fun previewRowSplit(totalItems: Int, capacity: Int): PreviewRowSplit = when {
    totalItems <= 0 -> PreviewRowSplit(shown = 0, overflow = 0)
    capacity <= 0 -> PreviewRowSplit(shown = 0, overflow = totalItems)
    totalItems <= capacity -> PreviewRowSplit(shown = totalItems, overflow = 0)
    else -> {
        val shown = (capacity - 1).coerceAtLeast(1)
        PreviewRowSplit(shown = shown, overflow = totalItems - shown)
    }
}
