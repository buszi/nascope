package io.buszi.nascope.ui.dashboard.widgets.process

import app.cash.turbine.turbineScope
import io.buszi.nascope.data.telemetry.ProcessInfo
import io.buszi.nascope.data.telemetry.ProcessListData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.components.badges.StatusLevel
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessListViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val telemetryStateHolder = mockk<TelemetryStateHolder>()

    private val viewModel by lazy {
        ProcessListViewModel(
            telemetryStateHolder = telemetryStateHolder,
        )
    }

    @Test
    fun processListViewModelEmitsReadyStateWithTelemetry() = runTest {
        turbineScope {
            val processFlow = MutableStateFlow(testProcessData)
            every { telemetryStateHolder.processList } returns processFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                listOf(
                    initialViewState,
                    readyViewStateCpu,
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                telemetryStateHolder.processList
            }
        }
    }

    @Test
    fun selectSortBySwitchesSortingOrder() = runTest {
        turbineScope {
            val processFlow = MutableStateFlow(testProcessData)
            every { telemetryStateHolder.processList } returns processFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(ProcessSortBy.Cpu, viewModel.viewState.value.sortBy)
            assertEquals("qemu-system-x86", viewModel.viewState.value.processes.first().name)

            viewModel.onViewAction(SelectSortBy(ProcessSortBy.Memory))
            runCurrent()

            assertEquals(ProcessSortBy.Memory, viewModel.viewState.value.sortBy)
            assertEquals("postgres", viewModel.viewState.value.processes.first().name)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleSortBySwitchesBetweenCpuAndMemory() = runTest {
        turbineScope {
            val processFlow = MutableStateFlow(testProcessData)
            every { telemetryStateHolder.processList } returns processFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            viewModel.onViewAction(ToggleSortBy)
            runCurrent()
            assertEquals(ProcessSortBy.Memory, viewModel.viewState.value.sortBy)

            viewModel.onViewAction(ToggleSortBy)
            runCurrent()
            assertEquals(ProcessSortBy.Cpu, viewModel.viewState.value.sortBy)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun criticalProcessResourceTriggersCriticalStatus() = runTest {
        turbineScope {
            val criticalData = testProcessData.copy(
                processes = listOf(
                    testProcess1.copy(cpuPercent = 95.0f),
                ),
            )
            val processFlow = MutableStateFlow(criticalData)
            every { telemetryStateHolder.processList } returns processFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            val states = viewStateTurbine.cancelAndConsumeItems()
            assertEquals(StatusLevel.Critical, states.last().overallStatus)
        }
    }

    @Test
    fun retryInvokesStateHolderRefresh() = runTest {
        turbineScope {
            val processFlow = MutableStateFlow(testProcessData)
            every { telemetryStateHolder.processList } returns processFlow
            every { telemetryStateHolder.refresh() } just runs

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            viewModel.onViewAction(OnRetry)
            runCurrent()

            assertEquals(
                listOf(
                    initialViewState,
                    readyViewStateCpu,
                    readyViewStateCpu.copy(widgetState = WidgetState.Loading),
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                telemetryStateHolder.processList
                telemetryStateHolder.refresh()
            }
        }
    }

    private companion object {
        val testProcess1 = ProcessInfo(
            pid = 1042,
            name = "qemu-system-x86",
            user = "libvirt",
            cpuPercent = 28.4f,
            memoryPercent = 4.2f,
            memoryRssBytes = 1_400_000_000L,
            status = "R",
        )

        val testProcess2 = ProcessInfo(
            pid = 3410,
            name = "postgres",
            user = "postgres",
            cpuPercent = 4.5f,
            memoryPercent = 15.8f,
            memoryRssBytes = 2_000_000_000L,
            status = "S",
        )

        val testProcessData = ProcessListData(
            processes = listOf(testProcess1, testProcess2),
            totalProcessCount = 184,
            runningProcessCount = 3,
        )

        val initialViewState = ViewState()

        val readyViewStateCpu = ViewState(
            processes = listOf(testProcess1, testProcess2),
            totalProcessCount = 184,
            runningProcessCount = 3,
            sortBy = ProcessSortBy.Cpu,
            overallStatus = StatusLevel.Operational,
            widgetState = WidgetState.Ready,
        )
    }
}
