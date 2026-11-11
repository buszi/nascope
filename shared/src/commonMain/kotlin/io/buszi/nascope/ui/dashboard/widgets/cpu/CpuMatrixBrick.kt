package io.buszi.nascope.ui.dashboard.widgets.cpu

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.buszi.nascope.ui.dashboard.framework.BrickSpan
import io.buszi.nascope.ui.dashboard.framework.BrickDetails
import io.buszi.nascope.ui.dashboard.framework.DashboardBrick
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId

/**
 * Brick registration for the CPU Matrix widget.
 *
 * Configured with [BrickSpan.Adaptive] to occupy 2 columns on wide 3/4-column grids,
 * or 1 column when space is constrained, seamlessly filling row remainders without gaps.
 */
data object CpuMatrixBrick : DashboardBrick {
    override val id: DashboardBrickId = DashboardBrickId.CpuMatrix
    override val span: BrickSpan = BrickSpan.Adaptive(compactSpan = 2, expandedSpan = 2)

    override val details: BrickDetails = BrickDetails { onClose, modifier ->
        CpuDetailsScreen(onClose = onClose, modifier = modifier)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        CpuMatrixWidget(modifier = modifier)
    }
}
