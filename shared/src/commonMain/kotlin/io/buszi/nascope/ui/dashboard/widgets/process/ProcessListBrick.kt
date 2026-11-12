package io.buszi.nascope.ui.dashboard.widgets.process

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.buszi.nascope.ui.dashboard.framework.BrickSpan
import io.buszi.nascope.ui.dashboard.framework.BrickDetails
import io.buszi.nascope.ui.dashboard.framework.DashboardBrick
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId

/**
 * Brick registration for the Top Processes widget.
 *
 * Configured with [BrickSpan.FullRow]: the table reads well wide, and as the last brick it fills the final row, so
 * the two-column bricks before it (an even number of them) leave no gap, adhering to the Zero Empty Spaces policy.
 */
data object ProcessListBrick : DashboardBrick {
    override val id: DashboardBrickId = DashboardBrickId.ProcessList
    override val span: BrickSpan = BrickSpan.FullRow

    override val details: BrickDetails = BrickDetails { onClose, modifier ->
        ProcessDetailsScreen(onClose = onClose, modifier = modifier)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        ProcessListWidget(modifier = modifier)
    }
}
