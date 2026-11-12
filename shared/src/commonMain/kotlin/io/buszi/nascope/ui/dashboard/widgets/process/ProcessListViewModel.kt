package io.buszi.nascope.ui.dashboard.widgets.process

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveProcessesUseCase
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.ProcessUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessListViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessListViewModel.ViewAction.SelectSortBy
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessListViewModel.ViewAction.ToggleSortBy
import io.buszi.nascope.ui.dashboard.widgets.toWidgetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

enum class ProcessSortBy {
    Cpu,
    Memory,
}

@KoinViewModel
class ProcessListViewModel(
    private val observeProcesses: ObserveProcessesUseCase,
    private val refreshTelemetry: RefreshTelemetryUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    /** The latest processes, unsorted, so a new sort order applies without waiting for the next poll. */
    private var rawProcesses: List<ProcessUi> = emptyList()

    init {
        observeTelemetry()
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            OnRetry -> refreshTelemetry()
            is SelectSortBy -> setSortBy(action.sortBy)
            ToggleSortBy -> setSortBy(
                if (mutableViewState.value.sortBy == ProcessSortBy.Cpu) ProcessSortBy.Memory else ProcessSortBy.Cpu,
            )
        }
    }

    private fun observeTelemetry() {
        observeProcesses()
            .onEach { state ->
                mutableViewState.update { current ->
                    when (state) {
                        is TelemetryState.Ready -> {
                            rawProcesses = state.data.processes.map { ProcessUi.from(it, state.data.logicalCores) }
                            current.copy(
                                totalProcessCount = state.data.count?.total,
                                runningProcessCount = state.data.count?.running,
                            )
                        }
                        TelemetryState.Loading -> {
                            rawProcesses = emptyList()
                            current.copy(totalProcessCount = null, runningProcessCount = null)
                        }
                        is TelemetryState.Failed -> current
                    }.withProcesses(rawProcesses)
                        .copy(widgetState = state.toWidgetState(onRetry = refreshTelemetry::invoke))
                }
            }
            .launchIn(viewModelScope)
    }

    private fun setSortBy(sortBy: ProcessSortBy) {
        mutableViewState.update { it.copy(sortBy = sortBy).withProcesses(rawProcesses) }
    }

    private fun ViewState.withProcesses(processes: List<ProcessUi>): ViewState = copy(
        processes = when (sortBy) {
            ProcessSortBy.Cpu -> processes.sortedByDescending { it.cpuPercent }
            ProcessSortBy.Memory -> processes.sortedByDescending { it.memoryPercent }
        },
        overallStatus = processes.maxOfOrNull { it.loadStatus } ?: StatusLevel.Operational,
    )

    sealed interface ViewAction {
        data object OnRetry : ViewAction
        data class SelectSortBy(val sortBy: ProcessSortBy) : ViewAction
        data object ToggleSortBy : ViewAction
    }

    @Stable
    data class ViewState(
        val processes: List<ProcessUi> = emptyList(),
        /** `null` when the host has no `processcount` data: the counts are left out. */
        val totalProcessCount: Int? = null,
        val runningProcessCount: Int? = null,
        val sortBy: ProcessSortBy = ProcessSortBy.Cpu,
        val overallStatus: StatusLevel = StatusLevel.Operational,
        val widgetState: WidgetState = WidgetState.Loading,
    )
}
