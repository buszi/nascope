package io.buszi.nascope.ui.dashboard.widgets.sensors

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.buszi.nascope.ui.dashboard.framework.BrickSpan
import io.buszi.nascope.ui.dashboard.framework.DashboardBrick
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId

/**
 * Brick registration for the Sensors & Thermals widget.
 *
 * Configured with [BrickSpan.Double] to span 2 columns on wide 3/4-column grids,
 * or 2 columns on 2-column mobile grids, providing ample horizontal space for
 * CPU temperature gauge, drive thermometers, and fan RPM duty-cycle meters.
 */
data object SensorsThermalsBrick : DashboardBrick {
    override val id: DashboardBrickId = DashboardBrickId.SensorsThermals
    override val span: BrickSpan = BrickSpan.Double

    @Composable
    override fun Content(modifier: Modifier) {
        SensorsThermalsWidget(modifier = modifier)
    }
}
