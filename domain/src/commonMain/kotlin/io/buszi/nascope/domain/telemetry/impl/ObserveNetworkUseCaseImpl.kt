package io.buszi.nascope.domain.telemetry.impl

import io.buszi.nascope.domain.model.telemetry.NetworkInterfaceUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.KeyedMetricHistory
import io.buszi.nascope.domain.telemetry.ObserveNetworkUseCase
import io.buszi.nascope.domain.telemetry.ObserveNetworkUseCase.Data
import io.buszi.nascope.domain.telemetry.ObserveNetworkUseCase.Interface
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

@Single
internal class ObserveNetworkUseCaseImpl(
    poller: TelemetryPoller,
    applicationScope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
) : ObserveNetworkUseCase {

    private class Histories(
        val rx: KeyedMetricHistory<String> = KeyedMetricHistory(),
        val tx: KeyedMetricHistory<String> = KeyedMetricHistory(),
    )

    private val state: Flow<TelemetryState<Data>> = poller.networkInterfaces
        .withHistory(newState = ::Histories, onPolled = ::record)
        .toTelemetryState(::Data)
        .shareWhileObserved(applicationScope, dispatcher)

    override fun invoke(): Flow<TelemetryState<Data>> = state

    private fun record(histories: Histories, polled: Polled<List<NetworkInterfaceUsage>>): Polled<List<Interface>> =
        when (polled) {
            Polled.Loading -> {
                histories.rx.clear()
                histories.tx.clear()
                Polled.Loading
            }
            is Polled.Failed -> Polled.Failed(polled.failure)
            is Polled.Value -> {
                if (!polled.isStale) {
                    val names = polled.value.mapTo(mutableSetOf()) { it.name }
                    histories.rx.retainOnly(names)
                    histories.tx.retainOnly(names)
                }
                polled.mapValue { interfaces ->
                    interfaces.map { usage ->
                        Interface(
                            usage = usage,
                            rxHistory = if (polled.isStale) {
                                histories.rx.snapshot(usage.name)
                            } else {
                                histories.rx.add(usage.name, usage.rxBytesPerSec.toFloat())
                            },
                            txHistory = if (polled.isStale) {
                                histories.tx.snapshot(usage.name)
                            } else {
                                histories.tx.add(usage.name, usage.txBytesPerSec.toFloat())
                            },
                        )
                    }
                }
            }
        }
}
