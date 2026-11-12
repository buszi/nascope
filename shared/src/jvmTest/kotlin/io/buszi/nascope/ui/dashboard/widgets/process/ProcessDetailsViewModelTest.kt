package io.buszi.nascope.ui.dashboard.widgets.process

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.telemetry.ProcessCount
import io.buszi.nascope.domain.model.telemetry.ProcessUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.model.telemetry.ProgramUsage
import io.buszi.nascope.domain.telemetry.ObserveAvailablePluginsUseCase
import io.buszi.nascope.domain.telemetry.ObserveProcessesUseCase
import io.buszi.nascope.domain.telemetry.ObserveProgramsUseCase
import io.buszi.nascope.domain.telemetry.ObserveProcessesUseCase.Data
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.ProcessStateUi
import io.buszi.nascope.ui.common.model.ProcessUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.Mode
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.SortField
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnSelectMode
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnToggleProgram
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnNextPage
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnPrevPage
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnQueryChange
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnSelectSort
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnSelectStateFilter
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnTogglePin
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnToggleProcess
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.Pin
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
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

    private val observeProcesses = mockk<ObserveProcessesUseCase>()
    private val refreshTelemetry = mockk<RefreshTelemetryUseCase>()

    private val observePrograms = mockk<ObserveProgramsUseCase>()
    private val plugins = MutableStateFlow<Set<String>?>(null)
    private val observeAvailablePlugins = mockk<ObserveAvailablePluginsUseCase> { every { this@mockk() } returns plugins }

    private val viewModel by lazy {
        ProcessDetailsViewModel(
            observeProcesses = observeProcesses,
            observePrograms = observePrograms,
            observeAvailablePlugins = observeAvailablePlugins,
            refreshTelemetry = refreshTelemetry,
        )
    }

    @Test
    fun processDetailsViewModelEmitsReadyStateWithTelemetryData() = runTest {
        turbineScope {
            every { observeProcesses() } returns MutableStateFlow<TelemetryState<Data>>(processData)

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
                observeProcesses()
            }
        }
    }

    @Test
    fun processDetailsViewModelSortsByCpuDescendingByDefault() = runTest {
        turbineScope {
            every { observeProcesses() } returns MutableStateFlow<TelemetryState<Data>>(processData)

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
            every { observeProcesses() } returns MutableStateFlow<TelemetryState<Data>>(processData)

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
            every { observeProcesses() } returns MutableStateFlow<TelemetryState<Data>>(processData)

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
            every { observeProcesses() } returns MutableStateFlow<TelemetryState<Data>>(processData)

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
            every { observeProcesses() } returns MutableStateFlow<TelemetryState<Data>>(processData)

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
            every { observeProcesses() } returns MutableStateFlow<TelemetryState<Data>>(processData)

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
            val processFlow = MutableStateFlow<TelemetryState<Data>>(processData)
            every { observeProcesses() } returns processFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnNextPage)

            processFlow.value = ready(processUsages.take(3))
            runCurrent()

            assertEquals(0, viewModel.viewState.value.page)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun processDetailsViewModelTogglesExpandedProcess() = runTest {
        turbineScope {
            val processFlow = MutableStateFlow<TelemetryState<Data>>(processData)
            every { observeProcesses() } returns processFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            viewModel.onViewAction(OnToggleProcess(101))
            assertEquals(101, viewModel.viewState.value.expandedPid)

            viewModel.onViewAction(OnToggleProcess(101))
            assertNull(viewModel.viewState.value.expandedPid)

            viewModel.onViewAction(OnToggleProcess(102))
            processFlow.value = ready(processUsages.filter { it.pid != 102 })
            runCurrent()
            assertNull(viewModel.viewState.value.expandedPid)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun processDetailsViewModelLeavesOutCountsWhenProcessCountIsMissingAndKeepsMissingFieldsNull() = runTest {
        turbineScope {
            val usage = processUsages.first().copy(user = null, memoryRssBytes = null, statusCode = null)
            every { observeProcesses() } returns MutableStateFlow<TelemetryState<Data>>(
                TelemetryState.Ready(Data(listOf(usage), null), false),
            )

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            val state = viewModel.viewState.value
            assertNull(state.totalProcessCount)
            assertNull(state.runningProcessCount)
            assertEquals(ProcessUi.MissingUser, state.processes.single().user)
            assertNull(state.processes.single().memoryRssBytes)
            assertEquals(ProcessStateUi.Unknown, state.processes.single().state)
            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun processDetailsViewModelMarksStaleKeepsTheRowsAndMapsFailures() = runTest {
        turbineScope {
            val flow = MutableStateFlow<TelemetryState<Data>>(ready(processUsages, isStale = true))
            every { observeProcesses() } returns flow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            assertEquals(WidgetState.Stale, viewModel.viewState.value.widgetState)

            flow.value = TelemetryState.Failed(TelemetryFailure.PluginUnavailable)
            runCurrent()
            assertEquals(WidgetState.Unavailable, viewModel.viewState.value.widgetState)
            assertEquals(10, viewModel.viewState.value.processes.size)

            flow.value = TelemetryState.Failed(TelemetryFailure.Connection(ConnectionFailure.Timeout))
            runCurrent()
            assert(viewModel.viewState.value.widgetState is WidgetState.Offline)

            flow.value = TelemetryState.Loading
            runCurrent()
            assertEquals(ViewState(), viewModel.viewState.value)
            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun processDetailsViewModelPinsAndUnpinsAProcessAndKeepsItOutsideTheFilters() = runTest {
        turbineScope {
            val states = MutableStateFlow<TelemetryState<Data>>(processData)
            every { observeProcesses() } returns states
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            viewModel.onViewAction(OnTogglePin(105))
            viewModel.onViewAction(OnQueryChange("no-such-process"))
            runCurrent()
            val pinnedWhileSearching = viewModel.viewState.value
            assertEquals(Pin(105, "p5"), pinnedWhileSearching.pins.single())
            assertEquals(0, pinnedWhileSearching.results.size)
            assertEquals(105, pinnedWhileSearching.pinned.single().process?.pid)
            assertEquals(true, pinnedWhileSearching.isPinned(105))

            viewModel.onViewAction(OnTogglePin(105))
            runCurrent()
            assertEquals(emptyList(), viewModel.viewState.value.pins)
            viewStateTurbine.cancelAndConsumeItems()
        }
    }

    @Test
    fun processDetailsViewModelKeepsAPinnedProcessAsEndedWhenItDisappears() = runTest {
        turbineScope {
            val states = MutableStateFlow<TelemetryState<Data>>(processData)
            every { observeProcesses() } returns states
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnTogglePin(105))
            runCurrent()

            states.value = ready(processUsages.filterNot { it.pid == 105 })
            runCurrent()

            val pinned = viewModel.viewState.value.pinned.single()
            assertEquals(Pin(105, "p5"), pinned.pin)
            assertNull(pinned.process)
            viewStateTurbine.cancelAndConsumeItems()
        }
    }

    @Test
    fun processDetailsViewModelIgnoresPinningAProcessItDoesNotKnow() = runTest {
        turbineScope {
            every { observeProcesses() } returns MutableStateFlow<TelemetryState<Data>>(processData)
            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            viewModel.onViewAction(OnTogglePin(9999))
            runCurrent()

            assertEquals(emptyList(), viewModel.viewState.value.pins)
            viewStateTurbine.cancelAndConsumeItems()
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

    // Programs

    private val phpWorkers = ProgramUsage("php-fpm", 12, 40f, 6f, 600_000_000, 24, "www-data", "S")
    private val postgres = ProgramUsage("postgres", 3, 10f, 12f, 900_000_000, 12, null, "S")
    private val programStates = MutableStateFlow<TelemetryState<ObserveProgramsUseCase.Data>>(TelemetryState.Loading)
    private var programsCollectors = 0

    private fun programsSetup() {
        every { observeProcesses() } returns MutableStateFlow<TelemetryState<Data>>(processData)
        every { observePrograms() } returns programStates.onStart { programsCollectors++ }.onCompletion { programsCollectors-- }
    }

    @Test
    fun theProgramsToggleIsHiddenUntilTheServerListsProgramlistAndSelectingProgramsIsIgnoredBefore() = runTest {
        programsSetup()
        viewModel
        runCurrent()
        assertEquals(false, viewModel.viewState.value.programsAvailable)
        viewModel.onViewAction(OnSelectMode(Mode.Programs))
        runCurrent()
        assertEquals(Mode.Processes, viewModel.viewState.value.mode)
        assertEquals(0, programsCollectors)

        plugins.value = setOf("processlist", "programlist")
        runCurrent()
        assertEquals(true, viewModel.viewState.value.programsAvailable)
        plugins.value = setOf("processlist") // e.g. another server
        runCurrent()
        assertEquals(false, viewModel.viewState.value.programsAvailable)
    }

    @Test
    fun programsAreCollectedOnlyWhileProgramsIsSelected() = runTest {
        programsSetup()
        plugins.value = setOf("programlist")
        viewModel
        runCurrent()
        assertEquals(0, programsCollectors) // nothing polls programlist in PROCESSES mode

        viewModel.onViewAction(OnSelectMode(Mode.Programs))
        runCurrent()
        assertEquals(1, programsCollectors)
        assertEquals(WidgetState.Loading, viewModel.viewState.value.programsWidgetState)
        programStates.value = TelemetryState.Ready(ObserveProgramsUseCase.Data(listOf(phpWorkers, postgres), 4), false)
        runCurrent()
        assertEquals(WidgetState.Ready, viewModel.viewState.value.programsWidgetState)
        assertEquals(listOf("php-fpm", "postgres"), viewModel.viewState.value.programs.map { it.name })
        assertEquals(10f, viewModel.viewState.value.programs.first().cpuMachinePercent) // 40% of 4 cores

        viewModel.onViewAction(OnSelectMode(Mode.Processes))
        runCurrent()
        assertEquals(0, programsCollectors)
    }

    @Test
    fun programsModeSearchSortAndPagingWorkAndTheSearchSurvivesModeSwitches() = runTest {
        programsSetup()
        plugins.value = setOf("programlist")
        viewModel
        runCurrent()
        viewModel.onViewAction(OnSelectMode(Mode.Programs))
        val many = (1..10).map { ProgramUsage("p$it", it, it.toFloat(), 1f, null, null, null, null) } + phpWorkers
        programStates.value = TelemetryState.Ready(ObserveProgramsUseCase.Data(many, null), false)
        runCurrent()

        var state = viewModel.viewState.value
        assertEquals(11, state.resultCount)
        assertEquals(2, state.pageCount)
        assertEquals("php-fpm", state.pagePrograms.first().name) // CPU 40 on top, descending
        viewModel.onViewAction(OnNextPage)
        assertEquals(3, viewModel.viewState.value.pagePrograms.size)

        viewModel.onViewAction(OnSelectSort(SortField.Count))
        assertEquals("php-fpm", viewModel.viewState.value.pagePrograms.first().name) // 12 procs
        viewModel.onViewAction(OnSelectSort(SortField.Name))
        assertEquals("p1", viewModel.viewState.value.pagePrograms.first().name)

        viewModel.onViewAction(OnQueryChange("php"))
        assertEquals(listOf("php-fpm"), viewModel.viewState.value.programResults.map { it.name })
        assertEquals(0, viewModel.viewState.value.page)
        viewModel.onViewAction(OnSelectMode(Mode.Processes))
        assertEquals("php", viewModel.viewState.value.query)
        viewModel.onViewAction(OnSelectMode(Mode.Programs))
        assertEquals("php", viewModel.viewState.value.query)
    }

    @Test
    fun theSortFallsBackToCpuWhenItsColumnDoesNotExistInTheOtherMode() = runTest {
        programsSetup()
        plugins.value = setOf("programlist")
        viewModel
        runCurrent()
        viewModel.onViewAction(OnSelectSort(SortField.Pid))
        viewModel.onViewAction(OnSelectMode(Mode.Programs))
        assertEquals(SortField.Cpu, viewModel.viewState.value.sortField)
        assertEquals(true, viewModel.viewState.value.sortDescending)
        viewModel.onViewAction(OnSelectSort(SortField.Count))
        viewModel.onViewAction(OnSelectMode(Mode.Processes))
        assertEquals(SortField.Cpu, viewModel.viewState.value.sortField)
    }

    @Test
    fun pinsAreUnaffectedByProgramsAndAUnavailableProgramlistFallsBackToProcesses() = runTest {
        programsSetup()
        plugins.value = setOf("programlist")
        viewModel
        runCurrent()
        viewModel.onViewAction(OnTogglePin(viewModel.viewState.value.processes.first().pid))
        val pins = viewModel.viewState.value.pins
        assertEquals(1, pins.size)

        viewModel.onViewAction(OnSelectMode(Mode.Programs))
        programStates.value = TelemetryState.Failed(TelemetryFailure.PluginUnavailable)
        runCurrent()
        assertEquals(WidgetState.Unavailable, viewModel.viewState.value.programsWidgetState)
        assertEquals(pins, viewModel.viewState.value.pins)

        plugins.value = emptySet()
        runCurrent()
        assertEquals(Mode.Processes, viewModel.viewState.value.mode)
        assertEquals(pins, viewModel.viewState.value.pins)
        assertEquals(0, programsCollectors)
    }

    @Test
    fun programRowsExpandOneAtATime() = runTest {
        programsSetup()
        plugins.value = setOf("programlist")
        viewModel
        runCurrent()
        viewModel.onViewAction(OnSelectMode(Mode.Programs))
        programStates.value = TelemetryState.Ready(ObserveProgramsUseCase.Data(listOf(phpWorkers, postgres), null), false)
        runCurrent()
        viewModel.onViewAction(OnToggleProgram("php-fpm"))
        assertEquals("php-fpm", viewModel.viewState.value.expandedProgram)
        viewModel.onViewAction(OnToggleProgram("postgres"))
        assertEquals("postgres", viewModel.viewState.value.expandedProgram)
        viewModel.onViewAction(OnToggleProgram("postgres"))
        assertNull(viewModel.viewState.value.expandedProgram)
        assertEquals("—", viewModel.viewState.value.programs.first { it.name == "postgres" }.user)
    }

    companion object {
        // p1..p10: cpu = index, memory = index / 10, pid = 100 + index; p3 is a zombie,
        // p1 and p2 and p5 and p7 and p9 are owned by root, the rest by app.
        private val processUsages = (1..10).map { index ->
            ProcessUsage(
                pid = 100 + index,
                name = "p$index",
                user = if (index in setOf(1, 2, 5, 7, 9)) "root" else "app",
                cpuPercent = index.toFloat(),
                memoryPercent = index / 10f,
                memoryRssBytes = index * 1_000_000L,
                statusCode = if (index == 3) "Z" else if (index % 2 == 0) "R" else "S",
                commandLine = null,
            )
        }

        private fun ready(processes: List<ProcessUsage>, isStale: Boolean = false): TelemetryState<Data> =
            TelemetryState.Ready(Data(processes, ProcessCount(184, 5, 170, 900)), isStale)

        private val processData = ready(processUsages)

        private val initialViewState = ViewState()

        private val readyViewState = ViewState(
            processes = processUsages.map(ProcessUi::from),
            totalProcessCount = 184,
            runningProcessCount = 5,
            widgetState = WidgetState.Ready,
        )
    }
}
