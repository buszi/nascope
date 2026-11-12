package io.buszi.nascope.ui.dashboard.widgets.gpu

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.buszi.nascope.ui.dashboard.framework.BrickSpan
import io.buszi.nascope.ui.dashboard.framework.DashboardBrick
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId

/**
 * Brick registration for the GPU widget.
 *
 * Two columns like most bricks. Nine bricks of two columns would leave a gap in the last row, so the process list,
 * which reads well wide, takes the full last row. No details: the card shows everything Glances reports.
 */
data object GpuBrick : DashboardBrick {
    override val id: DashboardBrickId = DashboardBrickId.Gpu
    override val span: BrickSpan = BrickSpan.Double

    @Composable
    override fun Content(modifier: Modifier) {
        GpuWidget(modifier = modifier)
    }
}
