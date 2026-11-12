package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.model.telemetry.SensorReading
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import kotlinx.coroutines.flow.Flow

/** Primary: `sensors`. History: the value of each sensor, keyed by label. Grouping follows the sensor label. */
fun interface ObserveSensorsUseCase {
    operator fun invoke(): Flow<TelemetryState<Data>>

    data class Data(
        val cpu: List<Sensor>,
        val drives: List<Sensor>,
        val fans: List<Sensor>,
        val other: List<Sensor>,
    )

    data class Sensor(
        val reading: SensorReading,
        val history: List<Float>,
    )
}
