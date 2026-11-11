package io.buszi.nascope.ui.dashboard.widgets.process

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.buszi.nascope.ui.dashboard.framework.BrickSpan
import io.buszi.nascope.ui.dashboard.framework.DashboardBrick
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId

/**
 * Brick registration for the Top Processes widget.
 *
 * Configured with [BrickSpan.Double] to span 2 columns on 2-column mobile grids,
 * or 2 columns on 4-column desktop grids, pairing cleanly with other 2-cell bricks
 * while adhering to the Zero Empty Spaces policy.
 */
data object ProcessListBrick : DashboardBrick {
    override val id: DashboardBrickId = DashboardBrickId.ProcessList
    override val span: BrickSpan = BrickSpan.Double

    @Composable
    override fun Content(modifier: Modifier) {
        ProcessListWidget(modifier = modifier)
    }
}
