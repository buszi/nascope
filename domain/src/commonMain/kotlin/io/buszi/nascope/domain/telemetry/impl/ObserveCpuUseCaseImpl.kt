package io.buszi.nascope.domain.telemetry.impl

import io.buszi.nascope.domain.model.telemetry.CpuUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.MetricRingBuffer
import io.buszi.nascope.domain.telemetry.ObserveCpuUseCase
import io.buszi.nascope.domain.telemetry.ObserveCpuUseCase.Data
import io.buszi.nascope.domain.telemetry.Polled
import io.buszi.nascope.domain.telemetry.SensorClassifier
import io.buszi.nascope.domain.telemetry.TelemetryPoller
import io.buszi.nascope.domain.telemetry.mapValue
import io.buszi.nascope.domain.telemetry.shareWhileObserved
import io.buszi.nascope.domain.telemetry.toTelemetryState
import io.buszi.nascope.domain.telemetry.withHistory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

/** A `@Single` so that a brick and its details screen share one flow, and with it one history. */
@Single
internal class ObserveCpuUseCaseImpl(
    poller: TelemetryPoller,
    applicationScope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
) : ObserveCpuUseCase {

    private val state: Flow<TelemetryState<Data>> = poller.cpu
        .withHistory(newState = { MetricRingBuffer() }, onPolled = ::record)
        .toTelemetryState(poller.cpuCores, poller.cpuInfo, poller.sensors) { (usage, history), cores, info, sensors ->
            Data(
                usage = usage,
                cores = cores.orEmpty(),
                info = info,
                temperature = sensors?.let(SensorClassifier::cpuTemperature),
                usageHistory = history,
                usageDelta = usageDelta(usage, history),
            )
        }
        .shareWhileObserved(applicationScope, dispatcher)

    override fun invoke(): Flow<TelemetryState<Data>> = state

    private fun record(buffer: MetricRingBuffer, polled: Polled<CpuUsage>): Polled<Pair<CpuUsage, List<Float>>> {
        when (polled) {
            Polled.Loading -> buffer.clear()
            is Polled.Value -> if (!polled.isStale) buffer.add(polled.value.totalPercent)
            is Polled.Failed -> Unit
        }
        return polled.mapValue { it to buffer.snapshot() }
    }

    private fun usageDelta(usage: CpuUsage, history: List<Float>): Float? =
        if (history.size < MinSamplesForDelta) null else usage.totalPercent - history.average().toFloat()

    private companion object {
        const val MinSamplesForDelta = 10
    }
}
