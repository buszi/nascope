package io.buszi.nascope.ui.dashboard.widgets.alerts

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.model.telemetry.AlertState
import io.buszi.nascope.domain.model.telemetry.GlancesAlert
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveAlertsUseCase
import io.buszi.nascope.domain.telemetry.ObserveAlertsUseCase.Data
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class AlertsDetailsViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val states = MutableStateFlow<TelemetryState<Data>>(TelemetryState.Loading)
    private val viewModel by lazy {
        val observeAlerts = mockk<ObserveAlertsUseCase> { every { this@mockk() } returns states }
        AlertsDetailsViewModel(observeAlerts, mockk<RefreshTelemetryUseCase>())
    }

    @Test
    fun showsActiveAlertsAndTheFullHistoryWithFigures() = runTest {
        val active = GlancesAlert("CPU_USER", AlertState.Critical, 100, null, 90.0, 99.0, 95.0, null, listOf("ffmpeg"))
        val ended = GlancesAlert("LOAD", AlertState.Careful, 10, 70, 1.1, 1.6, 1.3, null, emptyList())
        turbineScope {
            val turbine = viewModel.viewState.testIn(this)
            runCurrent()
            states.value = TelemetryState.Ready(Data(listOf(active), listOf(ended), Instant.fromEpochSeconds(460)), false)
            runCurrent()
            val last = turbine.cancelAndConsumeItems().last()
            assertEquals(WidgetState.Ready, last.widgetState)
            assertEquals("6m", last.alerts.active.single().duration)
            assertEquals(listOf("ffmpeg"), last.alerts.active.single().topProcesses)
            assertEquals("1m", last.alerts.history.single().duration)
            assertEquals("1.1", last.alerts.history.single().min)
        }
    }
}
