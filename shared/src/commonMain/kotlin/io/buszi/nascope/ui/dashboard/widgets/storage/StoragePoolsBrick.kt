package io.buszi.nascope.ui.dashboard.widgets.storage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.buszi.nascope.ui.dashboard.framework.BrickSpan
import io.buszi.nascope.ui.dashboard.framework.BrickDetails
import io.buszi.nascope.ui.dashboard.framework.DashboardBrick
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId

/**
 * Brick registration for the Storage & Filesystems widget.
 *
 * Configured with [BrickSpan.Double] to occupy 2 columns on wide 3/4-column grids,
 * or 2 columns on 2-column mobile grids, providing ample horizontal space for
 * disk I/O throughput rates, mount paths, capacity readouts, and gauges.
 */
data object StoragePoolsBrick : DashboardBrick {
    override val id: DashboardBrickId = DashboardBrickId.StoragePools
    override val span: BrickSpan = BrickSpan.Double

    override val details: BrickDetails = BrickDetails { onClose, modifier ->
        StorageDetailsScreen(onClose = onClose, modifier = modifier)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        StoragePoolsWidget(modifier = modifier)
    }
}
