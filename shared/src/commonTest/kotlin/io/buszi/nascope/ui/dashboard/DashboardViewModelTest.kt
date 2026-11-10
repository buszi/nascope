package io.buszi.nascope.ui.dashboard

import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.ui.dashboard.framework.BrickRegistry
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewBrick
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DashboardViewModelTest {

    @Test
    fun dashboardViewModelExposesActiveBricksFromRegistry() {
        val brick = SystemOverviewBrick()
        val registry = BrickRegistry(brick)
        val viewModel = DashboardViewModel(registry)

        val state = viewModel.viewState.value
        assertEquals(1, state.bricks.size)
        assertEquals(DashboardBrickId.SystemOverview, state.bricks.first().id)
    }

    @Test
    fun systemOverviewViewModelEmitsReadyStateWithTelemetryData() {
        val telemetryStateHolder = TelemetryStateHolder()
        val viewModel = SystemOverviewViewModel(telemetryStateHolder)

        val state = viewModel.viewState.value
        assertEquals(WidgetState.Ready, state.widgetState)
        val data = state.data
        assertNotNull(data)
        assertEquals("alpha-node-01.local", data.hostname)
        assertEquals(8, data.cpuCores)
    }
}
