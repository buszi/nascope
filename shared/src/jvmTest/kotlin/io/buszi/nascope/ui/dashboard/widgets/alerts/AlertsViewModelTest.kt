package io.buszi.nascope.ui.dashboard.widgets.alerts

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.model.telemetry.AlertState
import io.buszi.nascope.domain.model.telemetry.GlancesAlert
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveAlertsUseCase
import io.buszi.nascope.domain.telemetry.ObserveAlertsUseCase.Data
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.model.AlertsUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.alerts.AlertsViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.alerts.AlertsViewModel.ViewState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifySequence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class AlertsViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val observeAlerts = mockk<ObserveAlertsUseCase>()
    private val refreshTelemetry = mockk<RefreshTelemetryUseCase>()
    private val states = MutableStateFlow<TelemetryState<Data>>(TelemetryState.Loading)

    private val viewModel by lazy {
        every { observeAlerts() } returns states
        AlertsViewModel(observeAlerts = observeAlerts, refreshTelemetry = refreshTelemetry)
    }

    private val warning = GlancesAlert("MEMSWAP", AlertState.Warning, 1_000, null, 72.0, 75.0, 73.0, "High swap", emptyList())
    private val data = Data(listOf(warning), emptyList(), Instant.fromEpochSeconds(1_000 + 600))
    private val ready = ViewState(alerts = AlertsUi.from(data), widgetState = WidgetState.Ready)

    private suspend fun TestScope.viewStates(vararg steps: TelemetryState<Data>): List<ViewState> {
        var result: List<ViewState> = emptyList()
        turbineScope {
            val turbine = viewModel.viewState.testIn(this)
            runCurrent()
            steps.forEach {
                states.value = it
                runCurrent()
            }
            result = turbine.cancelAndConsumeItems()
        }
        return result
    }

    @Test
    fun startsLoadingThenShowsTheMappedAlertsWithTheirDuration() = runTest {
        val states = viewStates(TelemetryState.Ready(data, isStale = false))
        assertEquals(listOf(ViewState(), ready), states)
        assertEquals("10m", states.last().alerts.active.single().duration)
    }

    @Test
    fun noAlertsIsReadyAndEmptyAllClear() = runTest {
        val last = viewStates(TelemetryState.Ready(Data(emptyList(), emptyList(), null), false)).last()
        assertEquals(AlertsUi(), last.alerts)
        assertEquals(WidgetState.Ready, last.widgetState)
    }

    @Test
    fun staleDataKeepsTheContent() = runTest {
        assertEquals(
            listOf(ViewState(), ready, ready.copy(widgetState = WidgetState.Stale)),
            viewStates(TelemetryState.Ready(data, false), TelemetryState.Ready(data, true)),
        )
    }

    @Test
    fun aMissingPluginIsUnavailable() = runTest {
        assertEquals(
            listOf(ViewState(), ViewState(widgetState = WidgetState.Unavailable)),
            viewStates(TelemetryState.Failed(TelemetryFailure.PluginUnavailable)),
        )
    }

    @Test
    fun retryRefreshesTelemetry() = runTest {
        every { refreshTelemetry() } just runs
        viewModel.onViewAction(OnRetry)
        verifySequence {
            observeAlerts()
            refreshTelemetry()
        }
    }
}
