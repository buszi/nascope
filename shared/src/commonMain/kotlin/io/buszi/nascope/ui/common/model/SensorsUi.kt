package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.telemetry.ObserveSensorsUseCase
import io.buszi.nascope.ui.common.components.badges.StatusLevel

/** UI representation of the sensors, grouped as the use case classified them. */
@Immutable
data class SensorsUi(
    val cpu: List<TemperatureSensorUi> = emptyList(),
    val drives: List<TemperatureSensorUi> = emptyList(),
    val fans: List<FanSensorUi> = emptyList(),
    val other: List<TemperatureSensorUi> = emptyList(),
) {
    /** The CPU package temperature, else the hottest core, else `null`: the brick's CPU headline. */
    val cpuHeadline: TemperatureSensorUi?
        get() = cpu.filter { it.label.startsWith("package", ignoreCase = true) }.maxByOrNull { it.currentTempC }
            ?: cpu.maxByOrNull { it.currentTempC }

    val temperatures: List<TemperatureSensorUi>
        get() = cpu + drives + other

    val hottest: TemperatureSensorUi?
        get() = temperatures.maxByOrNull { it.currentTempC }

    /** Temperature sensors at or above their warning threshold. */
    val atRiskCount: Int
        get() = temperatures.count { it.status != StatusLevel.Operational }

    val activeFansCount: Int
        get() = fans.count { !it.isStalled }

    val overallStatus: StatusLevel
        get() = temperatures.maxOfOrNull { it.status } ?: StatusLevel.Operational

    val isEmpty: Boolean
        get() = temperatures.isEmpty() && fans.isEmpty()

    companion object {
        /** Readings in other units than temperature (e.g. a battery in `%`) are not shown. */
        private val TemperatureUnits = setOf("C", "F")

        fun from(data: ObserveSensorsUseCase.Data): SensorsUi = SensorsUi(
            cpu = data.cpu.map(TemperatureSensorUi::from),
            drives = data.drives.map(TemperatureSensorUi::from),
            fans = data.fans.map(FanSensorUi::from),
            other = data.other
                .filter { it.reading.unit?.uppercase() in TemperatureUnits }
                .map(TemperatureSensorUi::from),
        )
    }
}
