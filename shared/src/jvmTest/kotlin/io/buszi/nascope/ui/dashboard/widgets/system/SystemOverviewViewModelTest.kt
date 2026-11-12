package io.buszi.nascope.ui.dashboard.widgets.system

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.telemetry.LoadAverage
import io.buszi.nascope.domain.model.telemetry.SystemInfo
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveSystemOverviewUseCase
import io.buszi.nascope.domain.telemetry.ObserveSystemOverviewUseCase.Data
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.model.LoadAverageUi
import io.buszi.nascope.ui.common.model.SystemOverviewUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewViewModel.ViewState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class SystemOverviewViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val observeSystemOverview = mockk<ObserveSystemOverviewUseCase>()
    private val refreshTelemetry = mockk<RefreshTelemetryUseCase>()
    private val states = MutableStateFlow<TelemetryState<Data>>(TelemetryState.Loading)

    private val viewModel by lazy {
        every { observeSystemOverview() } returns states
        SystemOverviewViewModel(
            observeSystemOverview = observeSystemOverview,
            refreshTelemetry = refreshTelemetry,
        )
    }

    private suspend fun kotlinx.coroutines.test.TestScope.viewStates(
        vararg steps: TelemetryState<Data>,
    ): List<ViewState> {
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
    fun systemOverviewViewModelStartsLoadingThenShowsTheMappedData() = runTest {
        assertEquals(
            listOf(ViewState(), readyViewState),
            viewStates(TelemetryState.Ready(data, isStale = false)),
        )
    }

    @Test
    fun systemOverviewViewModelKeepsTheContentAndMarksItStale() = runTest {
        assertEquals(
            listOf(ViewState(), readyViewState, readyViewState.copy(widgetState = WidgetState.Stale)),
            viewStates(TelemetryState.Ready(data, false), TelemetryState.Ready(data, true)),
        )
    }

    @Test
    fun systemOverviewViewModelShowsUnavailableWhenThePluginIsMissing() = runTest {
        assertEquals(
            listOf(ViewState(), ViewState(widgetState = WidgetState.Unavailable)),
            viewStates(TelemetryState.Failed(TelemetryFailure.PluginUnavailable)),
        )
    }

    @Test
    fun systemOverviewViewModelShowsOfflineForConnectionFailuresAndErrorForUnreadableData() = runTest {
        val states = viewStates(
            TelemetryState.Failed(TelemetryFailure.Connection(ConnectionFailure.Timeout)),
            TelemetryState.Failed(TelemetryFailure.InvalidResponse("bad")),
        )

        assertIs<WidgetState.Offline>(states[1].widgetState)
        assertIs<WidgetState.Error>(states[2].widgetState)
    }

    @Test
    fun systemOverviewViewModelDropsTheOldContentWhenLoadingAgain() = runTest {
        assertEquals(
            listOf(ViewState(), readyViewState, ViewState()),
            viewStates(TelemetryState.Ready(data, false), TelemetryState.Loading),
        )
    }

    @Test
    fun systemOverviewViewModelLeavesOutMissingSecondaryData() = runTest {
        val states = viewStates(TelemetryState.Ready(data.copy(uptime = null, load = null), false))

        assertEquals(null, states.last().overview.uptime)
        assertEquals(null, states.last().overview.load)
        assertEquals(WidgetState.Ready, states.last().widgetState)
    }

    @Test
    fun systemOverviewViewModelRetryRefreshesTelemetry() = runTest {
        every { refreshTelemetry() } just runs

        viewModel.onViewAction(OnRetry)

        verifySequence {
            observeSystemOverview()
            refreshTelemetry()
        }
    }

    companion object {
        private val data = Data(
            system = SystemInfo("alpha-node-01.local", "Linux", "6.8.0-45-generic", "Ubuntu 24.04.1 LTS", "64bit", null),
            uptime = 14.hours * 24 + 6.hours + 32.minutes,
            load = LoadAverage(0.42f, 0.58f, 0.71f, 8),
        )

        private val readyViewState = ViewState(
            overview = SystemOverviewUi(
                hostname = "alpha-node-01.local",
                osName = "Ubuntu 24.04.1 LTS",
                kernelRelease = "Linux 6.8.0-45-generic · 64bit",
                uptime = "14d 06h 32m",
                load = LoadAverageUi(0.42f, 0.58f, 0.71f, 8),
                platform = HostPlatform.Ubuntu,
            ),
            widgetState = WidgetState.Ready,
        )
    }
}
