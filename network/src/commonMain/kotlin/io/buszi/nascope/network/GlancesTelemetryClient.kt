package io.buszi.nascope.network

import com.github.michaelbull.result.Result
import io.buszi.nascope.domain.model.connection.ServerConnection
import io.buszi.nascope.domain.model.telemetry.ContainerUsage
import io.buszi.nascope.domain.model.telemetry.GlancesAlert
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
import io.buszi.nascope.domain.model.telemetry.ProgramUsage
import io.buszi.nascope.domain.model.telemetry.SensorReading
import io.buszi.nascope.domain.model.telemetry.SwapUsage
import io.buszi.nascope.domain.model.telemetry.SystemInfo
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import kotlin.time.Duration
import kotlin.time.Instant

/** One function is one Glances plugin endpoint. No retries, no caching, no combining of plugins. */
interface GlancesTelemetryClient {
    /** `pluginslist`: the names of the plugins the server runs (served by API v3 and v4 alike). */
    suspend fun fetchPlugins(server: ServerConnection): Result<Set<String>, TelemetryFailure>
    suspend fun fetchSystem(server: ServerConnection): Result<SystemInfo, TelemetryFailure>
    suspend fun fetchUptime(server: ServerConnection): Result<Duration, TelemetryFailure>
    suspend fun fetchLoad(server: ServerConnection): Result<LoadAverage, TelemetryFailure>
    suspend fun fetchCpu(server: ServerConnection): Result<CpuUsage, TelemetryFailure>
    suspend fun fetchCpuCores(server: ServerConnection): Result<List<CpuCoreUsage>, TelemetryFailure>
    suspend fun fetchCpuInfo(server: ServerConnection): Result<CpuInfo, TelemetryFailure>
    suspend fun fetchMemory(server: ServerConnection): Result<MemoryUsage, TelemetryFailure>
    suspend fun fetchSwap(server: ServerConnection): Result<SwapUsage, TelemetryFailure>
    suspend fun fetchFileSystems(server: ServerConnection): Result<List<FileSystemUsage>, TelemetryFailure>
    suspend fun fetchDiskIo(server: ServerConnection): Result<List<DiskIoRate>, TelemetryFailure>
    suspend fun fetchNetworkInterfaces(server: ServerConnection): Result<List<NetworkInterfaceUsage>, TelemetryFailure>
    suspend fun fetchSensors(server: ServerConnection): Result<List<SensorReading>, TelemetryFailure>
    suspend fun fetchContainers(server: ServerConnection): Result<List<ContainerUsage>, TelemetryFailure>
    /** The `gpu` plugin: an empty list when the host has no GPU Glances can read. */
    suspend fun fetchGpus(server: ServerConnection): Result<List<GpuUsage>, TelemetryFailure>
    /** The `alert` plugin: the alert log, active and ended entries. */
    suspend fun fetchAlerts(server: ServerConnection): Result<List<GlancesAlert>, TelemetryFailure>
    /** The `now` plugin: the server's own clock, the only trustworthy base for alert times. */
    suspend fun fetchServerTime(server: ServerConnection): Result<Instant, TelemetryFailure>
    suspend fun fetchProcessCount(server: ServerConnection): Result<ProcessCount, TelemetryFailure>
    suspend fun fetchProcesses(server: ServerConnection): Result<List<ProcessUsage>, TelemetryFailure>
    /** The `programlist` plugin: processes grouped by program. Glances 4 only; `PluginUnavailable` elsewhere. */
    suspend fun fetchPrograms(server: ServerConnection): Result<List<ProgramUsage>, TelemetryFailure>
}
