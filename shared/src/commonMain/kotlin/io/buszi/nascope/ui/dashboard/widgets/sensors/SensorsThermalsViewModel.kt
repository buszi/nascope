package io.buszi.nascope.ui.dashboard.widgets.sensors

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.data.telemetry.SensorsThermalsData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.FanSensorUi
import io.buszi.nascope.ui.common.model.TemperatureSensorUi
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
        val cpuTemperature = data.cpuTemperature?.let(TemperatureSensorUi::from)
        val driveTemperatures = data.driveTemperatures.map(TemperatureSensorUi::from)
        val fans = data.fans.map(FanSensorUi::from)
        val allTemps = listOfNotNull(cpuTemperature) + driveTemperatures
        val overallStatus = when {
            allTemps.any { it.status == StatusLevel.Critical } -> StatusLevel.Critical
            allTemps.any { it.status == StatusLevel.Degraded } -> StatusLevel.Degraded
            else -> StatusLevel.Operational
        }

        val activeFans = fans.count { it.rpm > 0 }
        val maxTemp = allTemps.maxOfOrNull { it.currentTempC } ?: 0f

        return ViewState(
            overallStatus = overallStatus,
            cpuTemperature = cpuTemperature,
            driveTemperatures = driveTemperatures,
            fans = fans,
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
        val cpuTemperature: TemperatureSensorUi? = null,
        val driveTemperatures: List<TemperatureSensorUi> = emptyList(),
        val fans: List<FanSensorUi> = emptyList(),
        val activeFansCount: Int = 0,
        val maxTemperatureC: Float = 0f,
        val widgetState: WidgetState = WidgetState.Ready,
    )

    sealed interface ViewAction {
        data object OnRetry : ViewAction
    }
}
