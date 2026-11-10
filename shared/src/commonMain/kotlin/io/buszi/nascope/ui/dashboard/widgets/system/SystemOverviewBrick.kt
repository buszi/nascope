package io.buszi.nascope.ui.dashboard.widgets.system

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.buszi.nascope.ui.dashboard.framework.BrickSpan
import io.buszi.nascope.ui.dashboard.framework.DashboardBrick
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import org.koin.core.annotation.Single

/**
 * Brick registration for the System Overview widget.
 *
 * Configured with [BrickSpan.Adaptive] to occupy 2 columns on wide 3/4-column grids,
 * or 1 column when space is constrained, seamlessly filling row remainders without gaps.
 */
@Single
data object SystemOverviewBrick : DashboardBrick {
    override val id: DashboardBrickId = DashboardBrickId.SystemOverview
    override val span: BrickSpan = BrickSpan.Adaptive(compactSpan = 1, expandedSpan = 2)

    @Composable
    override fun Content(modifier: Modifier) {
        SystemOverviewWidget(modifier = modifier)
    }
}
