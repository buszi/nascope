package io.buszi.nascope.ui.dashboard.widgets.cpu

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveCpuUseCase
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.ui.common.model.CpuCoreUi
import io.buszi.nascope.ui.common.model.CpuUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuDetailsViewModel.ViewAction.OnSelectSort
import io.buszi.nascope.ui.dashboard.widgets.toWidgetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class CpuDetailsViewModel(
    private val observeCpu: ObserveCpuUseCase,
    private val refreshTelemetry: RefreshTelemetryUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeTelemetry()
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            is OnSelectSort -> mutableViewState.update { it.copy(sortOrder = action.sortOrder) }
        }
    }

    private fun observeTelemetry() {
        observeCpu().onEach { state ->
            mutableViewState.update {
                it.copy(
                    cpu = when (state) {
                        is TelemetryState.Ready -> CpuUi.from(state.data)
                        TelemetryState.Loading -> CpuUi()
                        is TelemetryState.Failed -> it.cpu
                    },
                    widgetState = state.toWidgetState(onRetry = refreshTelemetry::invoke),
                )
            }
        }.launchIn(viewModelScope)
    }

    enum class SortOrder {
        Core,
        Usage,
    }

    @Stable
    data class ViewState(
        val cpu: CpuUi = CpuUi(),
        val sortOrder: SortOrder = SortOrder.Core,
        val widgetState: WidgetState = WidgetState.Loading,
    ) {
        val sortedCores: List<CpuCoreUi>
            get() = when (sortOrder) {
                SortOrder.Core -> cpu.cores.sortedBy { it.index }
                SortOrder.Usage -> cpu.cores.sortedByDescending { it.usagePercent }
            }
    }

    sealed interface ViewAction {
        data class OnSelectSort(val sortOrder: SortOrder) : ViewAction
    }
}
