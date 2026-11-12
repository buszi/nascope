package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.model.telemetry.NetworkInterfaceUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import kotlinx.coroutines.flow.Flow

/** Primary: `network`. History: rx / tx rate in bytes per second per interface name. */
fun interface ObserveNetworkUseCase {
    operator fun invoke(): Flow<TelemetryState<Data>>

    data class Data(
        val interfaces: List<Interface>,
    )

    data class Interface(
        val usage: NetworkInterfaceUsage,
        val rxHistory: List<Float>,
        val txHistory: List<Float>,
    )
}
