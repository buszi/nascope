package io.buszi.nascope.ui.dashboard.widgets.process

import app.cash.turbine.turbineScope
import io.buszi.nascope.data.telemetry.ProcessInfo
import io.buszi.nascope.data.telemetry.ProcessListData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.ProcessStateUi
import io.buszi.nascope.ui.common.model.ProcessUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.SortField
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnNextPage
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnPrevPage
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnQueryChange
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnSelectSort
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnSelectStateFilter
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnToggleProcess
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessDetailsViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val telemetryStateHolder = mockk<TelemetryStateHolder>()

    private val viewModel by lazy {
        ProcessDetailsViewModel(
            telemetryStateHolder = telemetryStateHolder,
        )
    }

    @Test
    fun processDetailsViewModelEmitsReadyStateWithTelemetryData() = runTest {
        turbineScope {
            every { telemetryStateHolder.processList } returns MutableStateFlow(processData)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                listOf(
                    initialViewState,
                    readyViewState,
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                telemetryStateHolder.processList
            }
        }
    }

    @Test
    fun processDetailsViewModelSortsByCpuDescendingByDefault() = runTest {
        turbineScope {
            every { telemetryStateHolder.processList } returns MutableStateFlow(processData)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                listOf("p10", "p9", "p8"),
                viewModel.viewState.value.results.take(3).map { it.name },
            )

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun processDetailsViewModelTogglesSortDirectionOnSameField() = runTest {
        turbineScope {
            every { telemetryStateHolder.processList } returns MutableStateFlow(processData)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            viewModel.onViewAction(OnSelectSort(SortField.Cpu))
            assertEquals(false, viewModel.viewState.value.sortDescending)
            assertEquals("p1", viewModel.viewState.value.results.first().name)

            viewModel.onViewAction(OnSelectSort(SortField.Cpu))
            assertEquals(true, viewModel.viewState.value.sortDescending)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun processDetailsViewModelUsesDefaultDirectionWhenSwitchingField() = runTest {
        turbineScope {
            every { telemetryStateHolder.processList } returns MutableStateFlow(processData)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            viewModel.onViewAction(OnSelectSort(SortField.Name))
            assertEquals(false, viewModel.viewState.value.sortDescending)
            assertEquals(listOf("p1", "p10", "p2"), viewModel.viewState.value.results.take(3).map { it.name })

            viewModel.onViewAction(OnSelectSort(SortField.Pid))
            assertEquals(listOf(101, 102, 103), viewModel.viewState.value.results.take(3).map { it.pid })

            viewModel.onViewAction(OnSelectSort(SortField.Memory))
            assertEquals(true, viewModel.viewState.value.sortDescending)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun processDetailsViewModelSearchesNameUserAndPid() = runTest {
        turbineScope {
            every { telemetryStateHolder.processList } returns MutableStateFlow(processData)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            viewModel.onViewAction(OnQueryChange("P10"))
            assertEquals(listOf("p10"), viewModel.viewState.value.results.map { it.name })

            viewModel.onViewAction(OnQueryChange("  ROOT "))
            assertEquals(5, viewModel.viewState.value.results.size)

            viewModel.onViewAction(OnQueryChange("105"))
            assertEquals(listOf("p5"), viewModel.viewState.value.results.map { it.name })

            viewModel.onViewAction(OnQueryChange(""))
            assertEquals(10, viewModel.viewState.value.results.size)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun processDetailsViewModelFiltersByState() = runTest {
        turbineScope {
            every { telemetryStateHolder.processList } returns MutableStateFlow(processData)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            viewModel.onViewAction(OnSelectStateFilter(ProcessStateUi.Zombie))
            assertEquals(listOf("p3"), viewModel.viewState.value.results.map { it.name })

            assertEquals(
                listOf(ProcessStateUi.Running, ProcessStateUi.Sleeping, ProcessStateUi.Zombie),
                viewModel.viewState.value.availableStates,
            )

            viewModel.onViewAction(OnSelectStateFilter(null))
            assertEquals(10, viewModel.viewState.value.results.size)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun processDetailsViewModelPaginatesAndResetsPageOnQueryChange() = runTest {
        turbineScope {
            every { telemetryStateHolder.processList } returns MutableStateFlow(processData)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(2, viewModel.viewState.value.pageCount)
            assertEquals(8, viewModel.viewState.value.pageProcesses.size)

            viewModel.onViewAction(OnNextPage)
            assertEquals(1, viewModel.viewState.value.page)
            assertEquals(2, viewModel.viewState.value.pageProcesses.size)

            // Cannot go past the last page
            viewModel.onViewAction(OnNextPage)
            assertEquals(1, viewModel.viewState.value.page)

            viewModel.onViewAction(OnPrevPage)
            viewModel.onViewAction(OnPrevPage)
            assertEquals(0, viewModel.viewState.value.page)

            viewModel.onViewAction(OnNextPage)
            viewModel.onViewAction(OnQueryChange("p"))
            assertEquals(0, viewModel.viewState.value.page)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun processDetailsViewModelClampsPageWhenProcessesShrink() = runTest {
        turbineScope {
            val processFlow = MutableStateFlow(processData)
            every { telemetryStateHolder.processList } returns processFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnNextPage)

            processFlow.value = processData.copy(processes = processData.processes.take(3))
            runCurrent()

            assertEquals(0, viewModel.viewState.value.page)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun processDetailsViewModelTogglesExpandedProcess() = runTest {
        turbineScope {
            val processFlow = MutableStateFlow(processData)
            every { telemetryStateHolder.processList } returns processFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            viewModel.onViewAction(OnToggleProcess(101))
            assertEquals(101, viewModel.viewState.value.expandedPid)

            viewModel.onViewAction(OnToggleProcess(101))
            assertNull(viewModel.viewState.value.expandedPid)

            viewModel.onViewAction(OnToggleProcess(102))
            processFlow.value = processData.copy(processes = processData.processes.filter { it.pid != 102 })
            runCurrent()
            assertNull(viewModel.viewState.value.expandedPid)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun processDetailsViewStateComputesAggregates() {
        val state = readyViewState

        assertEquals(55f, state.totalCpuPercent, absoluteTolerance = 0.01f)
        assertEquals(5.5f, state.totalMemoryPercent, absoluteTolerance = 0.01f)
        assertEquals(StatusLevel.Operational, state.overallStatus)
        assertEquals(1, state.countOf(ProcessStateUi.Zombie))
    }

    companion object {
        // p1..p10: cpu = index, memory = index / 10, pid = 100 + index; p3 is a zombie,
        // p1 and p2 and p5 and p7 and p9 are owned by root, the rest by app.
        private val processData = ProcessListData(
            processes = (1..10).map { index ->
                ProcessInfo(
                    pid = 100 + index,
                    name = "p$index",
                    user = if (index in setOf(1, 2, 5, 7, 9)) "root" else "app",
                    cpuPercent = index.toFloat(),
                    memoryPercent = index / 10f,
                    memoryRssBytes = index * 1_000_000L,
                    status = if (index == 3) "Z" else if (index % 2 == 0) "R" else "S",
                )
            },
            totalProcessCount = 184,
            runningProcessCount = 5,
        )

        private val initialViewState = ViewState()

        private val readyViewState = ViewState(
            processes = processData.processes.map(ProcessUi::from),
            totalProcessCount = 184,
            runningProcessCount = 5,
            widgetState = WidgetState.Ready,
        )
    }
}
