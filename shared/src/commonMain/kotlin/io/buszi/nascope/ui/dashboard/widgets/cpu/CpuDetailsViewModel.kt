package io.buszi.nascope.ui.dashboard.widgets.cpu

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.CpuCoreUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuDetailsViewModel.ViewAction.OnSelectSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class CpuDetailsViewModel(
    private val telemetryStateHolder: TelemetryStateHolder,
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
        combine(
            telemetryStateHolder.cpuMatrix,
            telemetryStateHolder.hostOverview,
        ) { cpuData, hostOverview ->
            cpuData to hostOverview
        }.onEach { (cpuData, hostOverview) ->
            mutableViewState.update { current ->
                current.copy(
                    modelName = cpuData.modelName,
                    aggregateUsage = cpuData.aggregateUsage,
                    usageTrend = cpuData.usageTrend,
                    frequencyGhz = cpuData.frequencyGhz,
                    temperatureC = cpuData.temperatureC,
                    cores = cpuData.cores.mapIndexed(CpuCoreUi::from),
                    usageHistory = cpuData.usageHistory,
                    load1m = hostOverview.load1m,
                    load5m = hostOverview.load5m,
                    load15m = hostOverview.load15m,
                    widgetState = WidgetState.Ready,
                )
            }
        }.launchIn(viewModelScope)
    }

    enum class SortOrder {
        Core,
        Usage,
        Frequency,
        Temperature,
    }

    @Stable
    data class ViewState(
        val modelName: String = "",
        val aggregateUsage: Float = 0f,
        val usageTrend: String = "",
        val frequencyGhz: Float = 0f,
        val temperatureC: Float = 0f,
        val cores: List<CpuCoreUi> = emptyList(),
        val usageHistory: List<Float> = emptyList(),
        val load1m: Float = 0f,
        val load5m: Float = 0f,
        val load15m: Float = 0f,
        val sortOrder: SortOrder = SortOrder.Core,
        val widgetState: WidgetState = WidgetState.Loading,
    ) {
        val sortedCores: List<CpuCoreUi>
            get() = when (sortOrder) {
                SortOrder.Core -> cores.sortedBy { it.index }
                SortOrder.Usage -> cores.sortedByDescending { it.usagePercent }
                SortOrder.Frequency -> cores.sortedByDescending { it.frequencyGhz }
                SortOrder.Temperature -> cores.sortedByDescending { it.temperatureC }
            }

        val averageFrequencyGhz: Float
            get() = if (cores.isEmpty()) frequencyGhz else cores.map { it.frequencyGhz }.average().toFloat()

        val peakFrequencyGhz: Float
            get() = cores.maxOfOrNull { it.frequencyGhz } ?: frequencyGhz

        val hottestCoreTemperatureC: Float
            get() = cores.maxOfOrNull { it.temperatureC } ?: temperatureC

        val busiestCore: CpuCoreUi?
            get() = cores.maxByOrNull { it.usagePercent }

        val usageHistoryAverage: Float
            get() = if (usageHistory.isEmpty()) 0f else usageHistory.average().toFloat()

        val usageHistoryPeak: Float
            get() = usageHistory.maxOrNull() ?: 0f

        val temperatureStatus: StatusLevel
            get() = CpuCoreUi.temperatureStatus(temperatureC)

        val overallStatus: StatusLevel
            get() = CpuCoreUi.overallStatus(aggregateUsage, temperatureC)

        /** Load average per logical core; `1.0` means the CPU is fully saturated. */
        fun loadPerCore(load: Float): Float = if (cores.isEmpty()) 0f else load / cores.size
    }

    sealed interface ViewAction {
        data class OnSelectSort(val sortOrder: SortOrder) : ViewAction
    }
}
