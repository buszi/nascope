package io.buszi.nascope.ui.dashboard.widgets.process

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.data.telemetry.ProcessInfo
import io.buszi.nascope.data.telemetry.ProcessListData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessListViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessListViewModel.ViewAction.SelectSortBy
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessListViewModel.ViewAction.ToggleSortBy
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
    private val telemetryStateHolder: TelemetryStateHolder,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    private var rawTelemetry: ProcessListData? = null

    init {
        observeTelemetry()
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            OnRetry -> retry()
            is SelectSortBy -> setSortBy(action.sortBy)
            ToggleSortBy -> toggleSortBy()
        }
    }

    private fun observeTelemetry() {
        telemetryStateHolder.processList
            .onEach { data ->
                rawTelemetry = data
                mutableViewState.update { current ->
                    deriveViewState(data, current.sortBy)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun deriveViewState(data: ProcessListData, sortBy: ProcessSortBy): ViewState {
        val sortedProcesses = when (sortBy) {
            ProcessSortBy.Cpu -> data.processes.sortedByDescending { it.cpuPercent }
            ProcessSortBy.Memory -> data.processes.sortedByDescending { it.memoryPercent }
        }

        val overallStatus = when {
            data.processes.any { it.cpuPercent >= 80f || it.memoryPercent >= 80f } -> StatusLevel.Critical
            data.processes.any { it.cpuPercent >= 50f || it.memoryPercent >= 50f } -> StatusLevel.Degraded
            else -> StatusLevel.Operational
        }

        return ViewState(
            processes = sortedProcesses,
            totalProcessCount = data.totalProcessCount,
            runningProcessCount = data.runningProcessCount,
            sortBy = sortBy,
            overallStatus = overallStatus,
            widgetState = WidgetState.Ready,
        )
    }

    private fun setSortBy(sortBy: ProcessSortBy) {
        val data = rawTelemetry ?: return
        mutableViewState.update { current ->
            deriveViewState(data, sortBy)
        }
    }

    private fun toggleSortBy() {
        val nextSort = if (mutableViewState.value.sortBy == ProcessSortBy.Cpu) {
            ProcessSortBy.Memory
        } else {
            ProcessSortBy.Cpu
        }
        setSortBy(nextSort)
    }

    private fun retry() {
        mutableViewState.update { it.copy(widgetState = WidgetState.Loading) }
        telemetryStateHolder.refresh()
    }

    sealed interface ViewAction {
        data object OnRetry : ViewAction
        data class SelectSortBy(val sortBy: ProcessSortBy) : ViewAction
        data object ToggleSortBy : ViewAction
    }

    @Stable
    data class ViewState(
        val processes: List<ProcessInfo> = emptyList(),
        val totalProcessCount: Int = 0,
        val runningProcessCount: Int = 0,
        val sortBy: ProcessSortBy = ProcessSortBy.Cpu,
        val overallStatus: StatusLevel = StatusLevel.Operational,
        val widgetState: WidgetState = WidgetState.Loading,
    )
}
