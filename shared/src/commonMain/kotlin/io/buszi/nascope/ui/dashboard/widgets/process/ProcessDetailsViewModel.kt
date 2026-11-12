package io.buszi.nascope.ui.dashboard.widgets.process

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveProcessesUseCase
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.ProcessStateUi
import io.buszi.nascope.ui.common.model.ProcessUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.toWidgetState
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnNextPage
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnPrevPage
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnQueryChange
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnSelectSort
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnSelectStateFilter
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewAction.OnToggleProcess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

/**
 * Drives the process details screen: a searchable, sortable, state-filterable and paginated
 * process table with expandable per-process properties.
 */
@KoinViewModel
class ProcessDetailsViewModel(
    private val observeProcesses: ObserveProcessesUseCase,
    private val refreshTelemetry: RefreshTelemetryUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeTelemetry()
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            is OnQueryChange -> mutableViewState.update { it.copy(query = action.query, page = 0) }
            is OnSelectSort -> selectSort(action.field)
            is OnSelectStateFilter -> mutableViewState.update { it.copy(stateFilter = action.state, page = 0) }
            OnNextPage -> mutableViewState.update { it.copy(page = (it.page + 1).coerceAtMost(it.pageCount - 1)) }
            OnPrevPage -> mutableViewState.update { it.copy(page = (it.page - 1).coerceAtLeast(0)) }
            is OnToggleProcess -> toggleProcess(action.pid)
        }
    }

    private fun selectSort(field: SortField) {
        mutableViewState.update { state ->
            if (state.sortField == field) {
                state.copy(sortDescending = !state.sortDescending, page = 0)
            } else {
                state.copy(sortField = field, sortDescending = field.defaultDescending, page = 0)
            }
        }
    }

    private fun toggleProcess(pid: Int) {
        mutableViewState.update { state ->
            state.copy(expandedPid = if (state.expandedPid == pid) null else pid)
        }
    }

    private fun observeTelemetry() {
        observeProcesses().onEach { state ->
            mutableViewState.update { current ->
                val updated = when (state) {
                    is TelemetryState.Ready -> current.copy(
                        processes = state.data.processes.map { ProcessUi.from(it, state.data.logicalCores) },
                        totalProcessCount = state.data.count?.total,
                        runningProcessCount = state.data.count?.running,
                    )
                    TelemetryState.Loading -> current.copy(
                        processes = emptyList(),
                        totalProcessCount = null,
                        runningProcessCount = null,
                    )
                    is TelemetryState.Failed -> current
                }.copy(widgetState = state.toWidgetState(onRetry = refreshTelemetry::invoke))
                updated.copy(
                    page = updated.page.coerceIn(0, updated.pageCount - 1),
                    expandedPid = updated.expandedPid?.takeIf { pid -> updated.processes.any { it.pid == pid } },
                )
            }
        }.launchIn(viewModelScope)
    }

    enum class SortField(val defaultDescending: Boolean) {
        Pid(defaultDescending = false),
        Name(defaultDescending = false),
        Cpu(defaultDescending = true),
        Memory(defaultDescending = true),
    }

    @Stable
    data class ViewState(
        val processes: List<ProcessUi> = emptyList(),
        /** `null` when the host has no `processcount` data. */
        val totalProcessCount: Int? = null,
        val runningProcessCount: Int? = null,
        val query: String = "",
        val sortField: SortField = SortField.Cpu,
        val sortDescending: Boolean = true,
        /** Only processes in this state are listed; `null` lists all. */
        val stateFilter: ProcessStateUi? = null,
        /** Zero-based index of the visible page of [results]. */
        val page: Int = 0,
        val expandedPid: Int? = null,
        val widgetState: WidgetState = WidgetState.Loading,
    ) {
        /** Processes matching [query] and [stateFilter], ordered by [sortField]. */
        val results: List<ProcessUi> by lazy(LazyThreadSafetyMode.NONE) {
            val needle = query.trim().lowercase()
            val comparator = when (sortField) {
                SortField.Pid -> compareBy<ProcessUi> { it.pid }
                SortField.Name -> compareBy<ProcessUi> { it.name.lowercase() }.thenBy { it.pid }
                SortField.Cpu -> compareBy<ProcessUi> { it.cpuPercent }.thenBy { it.pid }
                SortField.Memory -> compareBy<ProcessUi> { it.memoryPercent }.thenBy { it.pid }
            }
            processes
                .filter { stateFilter == null || it.state == stateFilter }
                .filter { needle.isEmpty() || it.matches(needle) }
                .sortedWith(if (sortDescending) comparator.reversed() else comparator)
        }

        val pageCount: Int
            get() = ((results.size + PageSize - 1) / PageSize).coerceAtLeast(1)

        /** The rows of the visible [page]. */
        val pageProcesses: List<ProcessUi>
            get() = results.drop(page * PageSize).take(PageSize)

        /** States present among the tracked processes, for the filter chips. */
        val availableStates: List<ProcessStateUi>
            get() = ProcessStateUi.entries.filter { state -> processes.any { it.state == state } }

        fun countOf(state: ProcessStateUi): Int = processes.count { it.state == state }

        val totalCpuPercent: Float
            get() = processes.sumOf { it.cpuPercent.toDouble() }.toFloat()

        val totalMemoryPercent: Float
            get() = processes.sumOf { it.memoryPercent.toDouble() }.toFloat()

        val overallStatus: StatusLevel
            get() = processes.maxOfOrNull { it.loadStatus } ?: StatusLevel.Operational

        private fun ProcessUi.matches(needle: String): Boolean =
            name.lowercase().contains(needle) ||
                user.lowercase().contains(needle) ||
                pid.toString().contains(needle)

        companion object {
            const val PageSize: Int = 8
        }
    }

    sealed interface ViewAction {
        data class OnQueryChange(val query: String) : ViewAction
        data class OnSelectSort(val field: SortField) : ViewAction
        data class OnSelectStateFilter(val state: ProcessStateUi?) : ViewAction
        data object OnNextPage : ViewAction
        data object OnPrevPage : ViewAction
        data class OnToggleProcess(val pid: Int) : ViewAction
    }
}
