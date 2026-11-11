package io.buszi.nascope.ui.dashboard.widgets.sensors

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.FanSensorUi
import io.buszi.nascope.ui.common.model.TemperatureSensorUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

/**
 * Drives the sensors details screen: a read-only thermal and cooling breakdown with history,
 * min / average / max and threshold headroom for every sensor.
 */
@KoinViewModel
class SensorsDetailsViewModel(
    private val telemetryStateHolder: TelemetryStateHolder,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeTelemetry()
    }

    private fun observeTelemetry() {
        telemetryStateHolder.sensorsThermals.onEach { data ->
            mutableViewState.update {
                it.copy(
                    cpuTemperature = data.cpuTemperature?.let(TemperatureSensorUi::from),
                    driveTemperatures = data.driveTemperatures.map(TemperatureSensorUi::from),
                    fans = data.fans.map(FanSensorUi::from),
                    widgetState = WidgetState.Ready,
                )
            }
        }.launchIn(viewModelScope)
    }

    @Stable
    data class ViewState(
        val cpuTemperature: TemperatureSensorUi? = null,
        val driveTemperatures: List<TemperatureSensorUi> = emptyList(),
        val fans: List<FanSensorUi> = emptyList(),
        val widgetState: WidgetState = WidgetState.Loading,
    ) {
        val temperatures: List<TemperatureSensorUi>
            get() = listOfNotNull(cpuTemperature) + driveTemperatures

        val hottest: TemperatureSensorUi?
            get() = temperatures.maxByOrNull { it.currentTempC }

        /** Temperature sensors at or above their warning threshold. */
        val atRiskCount: Int
            get() = temperatures.count { it.status != StatusLevel.Operational }

        val activeFansCount: Int
            get() = fans.count { !it.isStalled }

        val averageFanDutyPercent: Float
            get() = if (fans.isEmpty()) 0f else fans.map { it.dutyCyclePercent * 100f }.average().toFloat()

        val overallStatus: StatusLevel
            get() = temperatures.maxOfOrNull { it.status } ?: StatusLevel.Operational
    }
}
