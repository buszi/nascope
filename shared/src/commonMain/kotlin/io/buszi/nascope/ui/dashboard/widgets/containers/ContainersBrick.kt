package io.buszi.nascope.ui.dashboard.widgets.containers

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.buszi.nascope.ui.dashboard.framework.BrickSpan
import io.buszi.nascope.ui.dashboard.framework.BrickDetails
import io.buszi.nascope.ui.dashboard.framework.DashboardBrick
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId

/**
 * Brick registration for the Containers (Docker / Podman) widget.
 *
 * Configured with [BrickSpan.Double] to span 2 columns on 2-column mobile grids,
 * or 2 columns on 4-column desktop grids, pairing cleanly with other 2-cell bricks
 * while adhering to the Zero Empty Spaces policy.
 */
data object ContainersBrick : DashboardBrick {
    override val id: DashboardBrickId = DashboardBrickId.Containers
    override val span: BrickSpan = BrickSpan.Double

    override val details: BrickDetails = BrickDetails { onClose, modifier ->
        ContainersDetailsScreen(onClose = onClose, modifier = modifier)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        ContainersWidget(modifier = modifier)
    }
}
