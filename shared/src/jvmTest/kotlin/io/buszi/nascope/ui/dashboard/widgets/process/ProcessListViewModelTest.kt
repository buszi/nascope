package io.buszi.nascope.ui.dashboard.widgets.process

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.telemetry.ProcessCount
import io.buszi.nascope.domain.model.telemetry.ProcessUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveProcessesUseCase
import io.buszi.nascope.domain.telemetry.ObserveProcessesUseCase.Data
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.ProcessUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessListViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessListViewModel.ViewAction.SelectSortBy
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessListViewModel.ViewAction.ToggleSortBy
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessListViewModel.ViewState
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessListViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val observeProcesses = mockk<ObserveProcessesUseCase>()
    private val refreshTelemetry = mockk<RefreshTelemetryUseCase>()
    private val states = MutableStateFlow<TelemetryState<Data>>(TelemetryState.Loading)

    private val viewModel by lazy {
        every { observeProcesses() } returns states
        ProcessListViewModel(observeProcesses = observeProcesses, refreshTelemetry = refreshTelemetry)
    }

    @Test
    fun processListViewModelStartsLoadingThenShowsProcessesSortedByCpuWithCounts() = runTest {
        viewModel
        assertEquals(ViewState(), viewModel.viewState.value)

        states.value = ready(listOf(low, high))
        runCurrent()

        assertEquals(
            ViewState(
                processes = listOf(high, low).map(ProcessUi::from),
                totalProcessCount = 184,
                runningProcessCount = 3,
                sortBy = ProcessSortBy.Cpu,
                overallStatus = StatusLevel.Operational,
                widgetState = WidgetState.Ready,
            ),
            viewModel.viewState.value,
        )
    }

    @Test
    fun processListViewModelResortsTheLatestProcessesWhenTheSortChanges() = runTest {
        viewModel
        states.value = ready(listOf(low, high))
        runCurrent()

        viewModel.onViewAction(SelectSortBy(ProcessSortBy.Memory))
        assertEquals(listOf(low.pid, high.pid), viewModel.viewState.value.processes.map { it.pid })
        viewModel.onViewAction(ToggleSortBy)
        assertEquals(ProcessSortBy.Cpu, viewModel.viewState.value.sortBy)
        assertEquals(listOf(high.pid, low.pid), viewModel.viewState.value.processes.map { it.pid })

        // a poll keeps the chosen sort order
        viewModel.onViewAction(SelectSortBy(ProcessSortBy.Memory))
        states.value = ready(listOf(high, low))
        runCurrent()
        assertEquals(listOf(low.pid, high.pid), viewModel.viewState.value.processes.map { it.pid })
    }

    @Test
    fun processListViewModelLeavesOutCountsWithoutProcessCountData() = runTest {
        viewModel
        states.value = TelemetryState.Ready(Data(listOf(high), null), isStale = false)
        runCurrent()

        assertNull(viewModel.viewState.value.totalProcessCount)
        assertNull(viewModel.viewState.value.runningProcessCount)
        assertEquals(WidgetState.Ready, viewModel.viewState.value.widgetState)
    }

    @Test
    fun processListViewModelReportsCriticalWhenAProcessIsAbove80Percent() = runTest {
        viewModel
        states.value = ready(listOf(high.copy(cpuPercent = 95f)))
        runCurrent()

        assertEquals(StatusLevel.Critical, viewModel.viewState.value.overallStatus)
    }

    @Test
    fun processListViewModelMarksStaleKeepsRowsAndMapsFailures() = runTest {
        viewModel
        states.value = ready(listOf(high), isStale = true)
        runCurrent()
        assertEquals(WidgetState.Stale, viewModel.viewState.value.widgetState)

        states.value = TelemetryState.Failed(TelemetryFailure.PluginUnavailable)
        runCurrent()
        assertEquals(WidgetState.Unavailable, viewModel.viewState.value.widgetState)
        assertEquals(1, viewModel.viewState.value.processes.size)

        states.value = TelemetryState.Failed(TelemetryFailure.Connection(ConnectionFailure.Timeout))
        runCurrent()
        assertIs<WidgetState.Offline>(viewModel.viewState.value.widgetState)

        states.value = TelemetryState.Loading
        runCurrent()
        assertEquals(ViewState(), viewModel.viewState.value)
    }

    @Test
    fun processListViewModelRetryRefreshesTelemetry() = runTest {
        every { refreshTelemetry() } just runs

        viewModel.onViewAction(OnRetry)

        verifySequence {
            observeProcesses()
            refreshTelemetry()
        }
    }

    private companion object {
        val high = ProcessUsage(1042, "qemu-system-x86", "libvirt", 28.4f, 4.2f, 1_400_000_000L, "R", null)
        val low = ProcessUsage(3410, "postgres", "postgres", 4.5f, 15.8f, 2_000_000_000L, "S", null)

        fun ready(processes: List<ProcessUsage>, isStale: Boolean = false): TelemetryState<Data> =
            TelemetryState.Ready(Data(processes, ProcessCount(184, 3, 150, 600)), isStale)
    }
}
