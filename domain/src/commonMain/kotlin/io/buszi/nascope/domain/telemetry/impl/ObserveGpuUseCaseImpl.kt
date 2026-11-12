package io.buszi.nascope.domain.telemetry.impl

import io.buszi.nascope.domain.model.telemetry.GpuUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.KeyedMetricHistory
import io.buszi.nascope.domain.telemetry.ObserveGpuUseCase
import io.buszi.nascope.domain.telemetry.ObserveGpuUseCase.Data
import io.buszi.nascope.domain.telemetry.ObserveGpuUseCase.Gpu
import io.buszi.nascope.domain.telemetry.Polled
import io.buszi.nascope.domain.telemetry.TelemetryPoller
import io.buszi.nascope.domain.telemetry.mapValue
import io.buszi.nascope.domain.telemetry.shareWhileObserved
import io.buszi.nascope.domain.telemetry.toTelemetryState
import io.buszi.nascope.domain.telemetry.withHistory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

/** A `@Single` so that the brick and any future details screen share one flow, and with it one history. */
@Single
internal class ObserveGpuUseCaseImpl(
    poller: TelemetryPoller,
    applicationScope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
) : ObserveGpuUseCase {

    private val state: Flow<TelemetryState<Data>> = poller.gpus
        .withHistory(newState = { KeyedMetricHistory<String>() }, onPolled = ::record)
        .toTelemetryState(::Data)
        .shareWhileObserved(applicationScope, dispatcher)

    override fun invoke(): Flow<TelemetryState<Data>> = state

    private fun record(history: KeyedMetricHistory<String>, polled: Polled<List<GpuUsage>>): Polled<List<Gpu>> =
        when (polled) {
            Polled.Loading -> {
                history.clear()
                Polled.Loading
            }
            is Polled.Failed -> Polled.Failed(polled.failure)
            is Polled.Value -> {
                if (!polled.isStale) history.retainOnly(polled.value.mapTo(mutableSetOf()) { it.id })
                polled.mapValue { gpus ->
                    gpus.map { usage ->
                        val utilization = usage.utilizationPercent
                        Gpu(
                            usage = usage,
                            utilizationHistory = when {
                                utilization == null -> emptyList()
                                polled.isStale -> history.snapshot(usage.id)
                                else -> history.add(usage.id, utilization)
                            },
                        )
                    }
                }
            }
        }
}
