package io.buszi.nascope.domain.telemetry

import com.github.michaelbull.result.Result
import io.buszi.nascope.domain.model.telemetry.ContainerUsage
import io.buszi.nascope.domain.model.telemetry.CpuCoreUsage
import io.buszi.nascope.domain.model.telemetry.CpuInfo
import io.buszi.nascope.domain.model.telemetry.CpuUsage
import io.buszi.nascope.domain.model.telemetry.DiskIoRate
import io.buszi.nascope.domain.model.telemetry.FileSystemUsage
import io.buszi.nascope.domain.model.telemetry.GpuUsage
import io.buszi.nascope.domain.model.telemetry.LoadAverage
import io.buszi.nascope.domain.model.telemetry.MemoryUsage
import io.buszi.nascope.domain.model.telemetry.NetworkInterfaceUsage
import io.buszi.nascope.domain.model.telemetry.ProcessCount
import io.buszi.nascope.domain.model.telemetry.ProcessUsage
import io.buszi.nascope.domain.model.telemetry.SensorReading
import io.buszi.nascope.domain.model.telemetry.SwapUsage
import io.buszi.nascope.domain.model.telemetry.SystemInfo
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import kotlin.time.Duration

/** The telemetry of one session. Same functions as `GlancesTelemetryClient`, without the server parameter. */
internal interface TelemetrySource {
    /** The plugins the server runs. */
    suspend fun fetchPlugins(): Result<Set<String>, TelemetryFailure>
    suspend fun fetchSystem(): Result<SystemInfo, TelemetryFailure>
    suspend fun fetchUptime(): Result<Duration, TelemetryFailure>
    suspend fun fetchLoad(): Result<LoadAverage, TelemetryFailure>
    suspend fun fetchCpu(): Result<CpuUsage, TelemetryFailure>
    suspend fun fetchCpuCores(): Result<List<CpuCoreUsage>, TelemetryFailure>
    suspend fun fetchCpuInfo(): Result<CpuInfo, TelemetryFailure>
    suspend fun fetchMemory(): Result<MemoryUsage, TelemetryFailure>
    suspend fun fetchSwap(): Result<SwapUsage, TelemetryFailure>
    suspend fun fetchFileSystems(): Result<List<FileSystemUsage>, TelemetryFailure>
    suspend fun fetchDiskIo(): Result<List<DiskIoRate>, TelemetryFailure>
    suspend fun fetchNetworkInterfaces(): Result<List<NetworkInterfaceUsage>, TelemetryFailure>
    suspend fun fetchSensors(): Result<List<SensorReading>, TelemetryFailure>
    suspend fun fetchContainers(): Result<List<ContainerUsage>, TelemetryFailure>
    suspend fun fetchGpus(): Result<List<GpuUsage>, TelemetryFailure>
    suspend fun fetchProcessCount(): Result<ProcessCount, TelemetryFailure>
    suspend fun fetchProcesses(): Result<List<ProcessUsage>, TelemetryFailure>
}
