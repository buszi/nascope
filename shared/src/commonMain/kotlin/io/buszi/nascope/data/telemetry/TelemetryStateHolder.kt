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
 * Encapsulates detailed CPU utilization telemetry for the CPU matrix brick.
 */
data class CpuMatrixData(
    val modelName: String,
    val aggregateUsage: Float,
    val usageTrend: String,
    val frequencyGhz: Float,
    val temperatureC: Float,
    val coreUsages: List<Float>,
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

    private val _cpuMatrix = MutableStateFlow(
        CpuMatrixData(
            modelName = "AMD Ryzen 9 5950X (16-Core)",
            aggregateUsage = 42.8f,
            usageTrend = "+2.4% / 5m",
            frequencyGhz = 3.80f,
            temperatureC = 58.0f,
            coreUsages = listOf(
                34.2f, 48.1f, 12.0f, 88.5f,
                24.0f, 62.3f, 41.0f, 19.8f,
                55.4f, 31.2f, 76.8f, 92.1f,
                15.6f, 43.9f, 28.7f, 64.5f,
            ),
        ),
    )
    val cpuMatrix: StateFlow<CpuMatrixData> = _cpuMatrix.asStateFlow()

    fun refresh() {
        // Triggered by manual dashboard or widget retry
        _hostOverview.value = _hostOverview.value.copy()
        _cpuMatrix.value = _cpuMatrix.value.copy()
    }
}
