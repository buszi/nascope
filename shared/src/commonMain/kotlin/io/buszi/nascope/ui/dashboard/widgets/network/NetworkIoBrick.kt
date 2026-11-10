package io.buszi.nascope.ui.dashboard.widgets.network

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.buszi.nascope.ui.dashboard.framework.BrickSpan
import io.buszi.nascope.ui.dashboard.framework.DashboardBrick
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId

/**
 * Brick registration for the Network I/O widget.
 *
 * Configured with [BrickSpan.Double] to span 2 columns on wide 3/4-column grids,
 * or 2 columns on 2-column mobile grids, providing ample horizontal space for
 * dual throughput panels, live sparklines, and interface selectors.
 */
data object NetworkIoBrick : DashboardBrick {
    override val id: DashboardBrickId = DashboardBrickId.NetworkIo
    override val span: BrickSpan = BrickSpan.Double

    @Composable
    override fun Content(modifier: Modifier) {
        NetworkIoWidget(modifier = modifier)
    }
}
