package io.buszi.nascope.ui.dashboard.widgets.sensors

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.data.telemetry.FanSensorData
import io.buszi.nascope.data.telemetry.SensorsThermalsData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.data.telemetry.TemperatureSensorData
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.sensors.SensorsThermalsViewModel.ViewAction.OnRetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SensorsThermalsViewModel(
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

    private fun observeTelemetry() {
        telemetryStateHolder.sensorsThermals
            .onEach { data ->
                mutableViewState.update {
                    deriveViewState(data)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun deriveViewState(data: SensorsThermalsData): ViewState {
        val allTemps = listOfNotNull(data.cpuTemperature) + data.driveTemperatures
        val overallStatus = when {
            allTemps.any { it.currentTempC >= it.criticalThresholdC } -> StatusLevel.Critical
            allTemps.any { it.currentTempC >= it.warningThresholdC } -> StatusLevel.Degraded
            else -> StatusLevel.Operational
        }

        val activeFans = data.fans.count { it.rpm > 0 }
        val maxTemp = allTemps.maxOfOrNull { it.currentTempC } ?: 0f

        return ViewState(
            overallStatus = overallStatus,
            cpuTemperature = data.cpuTemperature,
            driveTemperatures = data.driveTemperatures,
            fans = data.fans,
            activeFansCount = activeFans,
            maxTemperatureC = maxTemp,
            widgetState = WidgetState.Ready,
        )
    }

    private fun retry() {
        mutableViewState.update { it.copy(widgetState = WidgetState.Loading) }
        telemetryStateHolder.refresh()
    }

    @Stable
    data class ViewState(
        val overallStatus: StatusLevel = StatusLevel.Operational,
        val cpuTemperature: TemperatureSensorData? = null,
        val driveTemperatures: List<TemperatureSensorData> = emptyList(),
        val fans: List<FanSensorData> = emptyList(),
        val activeFansCount: Int = 0,
        val maxTemperatureC: Float = 0f,
        val widgetState: WidgetState = WidgetState.Ready,
    )

    sealed interface ViewAction {
        data object OnRetry : ViewAction
    }
}
