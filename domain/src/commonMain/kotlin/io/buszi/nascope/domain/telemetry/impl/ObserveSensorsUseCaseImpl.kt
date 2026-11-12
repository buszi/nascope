package io.buszi.nascope.domain.telemetry.impl

import io.buszi.nascope.domain.model.telemetry.SensorReading
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.KeyedMetricHistory
import io.buszi.nascope.domain.telemetry.ObserveSensorsUseCase
import io.buszi.nascope.domain.telemetry.ObserveSensorsUseCase.Data
import io.buszi.nascope.domain.telemetry.ObserveSensorsUseCase.Sensor
import io.buszi.nascope.domain.telemetry.Polled
import io.buszi.nascope.domain.telemetry.SensorClassifier
import io.buszi.nascope.domain.telemetry.SensorGroup
import io.buszi.nascope.domain.telemetry.TelemetryPoller
import io.buszi.nascope.domain.telemetry.mapValue
import io.buszi.nascope.domain.telemetry.shareWhileObserved
import io.buszi.nascope.domain.telemetry.toTelemetryState
import io.buszi.nascope.domain.telemetry.withHistory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single
internal class ObserveSensorsUseCaseImpl(
    private val poller: TelemetryPoller,
    applicationScope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
) : ObserveSensorsUseCase {

    private val state: Flow<TelemetryState<Data>> = poller.sensors
        .withHistory(newState = { KeyedMetricHistory<String>() }, onPolled = ::record)
        .toTelemetryState { it }
        .shareWhileObserved(applicationScope, dispatcher)

    override fun invoke(): Flow<TelemetryState<Data>> = state

    private fun record(history: KeyedMetricHistory<String>, polled: Polled<List<SensorReading>>): Polled<Data> =
        when (polled) {
            Polled.Loading -> {
                history.clear()
                Polled.Loading
            }
            is Polled.Failed -> Polled.Failed(polled.failure)
            is Polled.Value -> {
                if (!polled.isStale) history.retainOnly(polled.value.mapTo(mutableSetOf()) { it.label })
                polled.mapValue { readings ->
                    val sensors = readings.map { reading ->
                        val samples = if (polled.isStale) {
                            history.snapshot(reading.label)
                        } else {
                            history.add(reading.label, reading.value)
                        }
                        SensorClassifier.classify(reading) to Sensor(reading, samples)
                    }
                    fun group(group: SensorGroup) = sensors.filter { it.first == group }.map { it.second }
                    Data(
                        cpu = group(SensorGroup.Cpu),
                        drives = group(SensorGroup.Drives),
                        fans = group(SensorGroup.Fans),
                        other = group(SensorGroup.Other),
                    )
                }
            }
        }
}
