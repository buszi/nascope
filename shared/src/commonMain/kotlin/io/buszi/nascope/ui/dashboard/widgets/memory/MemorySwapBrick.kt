package io.buszi.nascope.ui.dashboard.widgets.memory

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.buszi.nascope.ui.dashboard.framework.BrickSpan
import io.buszi.nascope.ui.dashboard.framework.DashboardBrick
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId

/**
 * Brick registration for the Memory & Swap widget.
 *
 * Configured with [BrickSpan.Double] to occupy 2 columns on wide 3/4-column grids,
 * or 2 columns on 2-column mobile grids, providing ample horizontal room for
 * the tiered allocation bar and memory classification metrics.
 */
data object MemorySwapBrick : DashboardBrick {
    override val id: DashboardBrickId = DashboardBrickId.MemorySwap
    override val span: BrickSpan = BrickSpan.Double

    @Composable
    override fun Content(modifier: Modifier) {
        MemorySwapWidget(modifier = modifier)
    }
}
