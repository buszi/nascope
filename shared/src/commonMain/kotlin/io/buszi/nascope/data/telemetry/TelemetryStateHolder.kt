package io.buszi.nascope.data.telemetry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

/**
 * Encapsulates host system identification telemetry for the overview brick.
 */
data class HostOverviewData(
    val hostname: String,
    val osName: String,
    val kernelRelease: String,
    val uptime: String,
    val load1m: Float,
    val load5m: Float,
    val load15m: Float,
    val cpuCores: Int,
)

/**
 * Shared singleton hub managing telemetry polling, caching, and stream distribution.
 *
 * All widget ViewModels subscribe to specific telemetry slices exposed by this state holder.
 */
@Single
class TelemetryStateHolder {

    private val _hostOverview = MutableStateFlow(
        HostOverviewData(
            hostname = "alpha-node-01.local",
            osName = "Ubuntu 24.04.1 LTS",
            kernelRelease = "Linux 6.8.0-45-generic",
            uptime = "14d 06h 32m",
            load1m = 0.42f,
            load5m = 0.58f,
            load15m = 0.71f,
            cpuCores = 8,
        ),
    )
    val hostOverview: StateFlow<HostOverviewData> = _hostOverview.asStateFlow()

    fun refresh() {
        // Triggered by manual dashboard or widget retry
        _hostOverview.value = _hostOverview.value.copy()
    }
}
