package io.buszi.nascope.ui.dashboard.widgets.alerts

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.buszi.nascope.ui.dashboard.framework.BrickDetails
import io.buszi.nascope.ui.dashboard.framework.BrickSpan
import io.buszi.nascope.ui.dashboard.framework.DashboardBrick
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId

/** Brick registration for the Glances alert log. Shown only on a server that runs the `alert` plugin. */
data object AlertsBrick : DashboardBrick {
    override val id: DashboardBrickId = DashboardBrickId.Alerts
    override val span: BrickSpan = BrickSpan.Double
    override val requiredPlugins: Set<String> = setOf("alert")

    override val details: BrickDetails = BrickDetails { onClose, modifier ->
        AlertsDetailsScreen(onClose = onClose, modifier = modifier)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        AlertsWidget(modifier = modifier)
    }
}
