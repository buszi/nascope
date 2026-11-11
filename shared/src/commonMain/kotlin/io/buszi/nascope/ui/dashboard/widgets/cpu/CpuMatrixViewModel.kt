package io.buszi.nascope.ui.dashboard.widgets.cpu

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuMatrixViewModel.ViewAction.OnRetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class CpuMatrixViewModel(
    private val telemetryStateHolder: TelemetryStateHolder,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeTelemetry()
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            OnRetry -> retry()
        }
    }

    private fun retry() {
        mutableViewState.update { it.copy(widgetState = WidgetState.Loading) }
        telemetryStateHolder.refresh()
    }

    private fun observeTelemetry() {
        telemetryStateHolder.cpuMatrix.onEach { cpuData ->
            mutableViewState.update { state ->
                state.copy(
                    modelName = cpuData.modelName,
                    aggregateUsage = cpuData.aggregateUsage,
                    usageTrend = cpuData.usageTrend,
                    frequencyGhz = cpuData.frequencyGhz,
                    temperatureC = cpuData.temperatureC,
                    coreUsages = cpuData.cores.map { it.usagePercent },
                    widgetState = WidgetState.Ready,
                )
            }
        }.launchIn(viewModelScope)
    }

    @Stable
    data class ViewState(
        val modelName: String = "",
        val aggregateUsage: Float = 0f,
        val usageTrend: String = "",
        val frequencyGhz: Float = 0f,
        val temperatureC: Float = 0f,
        val coreUsages: List<Float> = emptyList(),
        val widgetState: WidgetState = WidgetState.Loading,
    )

    sealed interface ViewAction {
        data object OnRetry : ViewAction
    }
}
