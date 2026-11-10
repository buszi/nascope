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
 * Encapsulates RAM and swap memory allocation telemetry for the memory brick.
 */
data class MemorySwapData(
    val totalMemBytes: Long,
    val usedMemBytes: Long,
    val cachedMemBytes: Long,
    val buffersMemBytes: Long,
    val freeMemBytes: Long,
    val totalSwapBytes: Long,
    val usedSwapBytes: Long,
    val freeSwapBytes: Long,
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

    private val _memorySwap = MutableStateFlow(
        MemorySwapData(
            totalMemBytes = 34_359_738_368L,
            usedMemBytes = 13_743_895_347L,
            cachedMemBytes = 9_126_805_504L,
            buffersMemBytes = 1_288_490_188L,
            freeMemBytes = 10_200_547_329L,
            totalSwapBytes = 17_179_869_184L,
            usedSwapBytes = 2_576_980_377L,
            freeSwapBytes = 14_602_888_807L,
        ),
    )
    val memorySwap: StateFlow<MemorySwapData> = _memorySwap.asStateFlow()

    fun refresh() {
        // Triggered by manual dashboard or widget retry
        _hostOverview.value = _hostOverview.value.copy()
        _cpuMatrix.value = _cpuMatrix.value.copy()
        _memorySwap.value = _memorySwap.value.copy()
    }
}
