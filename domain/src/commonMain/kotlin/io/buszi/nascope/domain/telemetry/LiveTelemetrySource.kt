package io.buszi.nascope.domain.telemetry

import com.github.michaelbull.result.Result
import io.buszi.nascope.domain.connection.toServerConnection
import io.buszi.nascope.domain.model.connection.ServerConnection
import io.buszi.nascope.domain.model.connection.ServerProfile
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
import io.buszi.nascope.network.GlancesTelemetryClient
import io.buszi.nascope.storage.SecretStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.time.Duration

/**
 * Telemetry of one connected profile. The [ServerConnection] is rebuilt on every call, so a changed
 * password is picked up without restarting anything.
 */
internal class LiveTelemetrySource(
    private val profile: ServerProfile,
    private val client: GlancesTelemetryClient,
    private val secretStore: SecretStore,
    private val dispatcher: CoroutineDispatcher,
) : TelemetrySource {

    private suspend fun <T> call(
        block: suspend GlancesTelemetryClient.(ServerConnection) -> Result<T, TelemetryFailure>,
    ): Result<T, TelemetryFailure> = withContext(dispatcher) {
        client.block(profile.toServerConnection(secretStore))
    }

    override suspend fun fetchPlugins(): Result<Set<String>, TelemetryFailure> = call { fetchPlugins(it) }
    override suspend fun fetchSystem(): Result<SystemInfo, TelemetryFailure> = call { fetchSystem(it) }
    override suspend fun fetchUptime(): Result<Duration, TelemetryFailure> = call { fetchUptime(it) }
    override suspend fun fetchLoad(): Result<LoadAverage, TelemetryFailure> = call { fetchLoad(it) }
    override suspend fun fetchCpu(): Result<CpuUsage, TelemetryFailure> = call { fetchCpu(it) }
    override suspend fun fetchCpuCores(): Result<List<CpuCoreUsage>, TelemetryFailure> = call { fetchCpuCores(it) }
    override suspend fun fetchCpuInfo(): Result<CpuInfo, TelemetryFailure> = call { fetchCpuInfo(it) }
    override suspend fun fetchMemory(): Result<MemoryUsage, TelemetryFailure> = call { fetchMemory(it) }
    override suspend fun fetchSwap(): Result<SwapUsage, TelemetryFailure> = call { fetchSwap(it) }
    override suspend fun fetchFileSystems(): Result<List<FileSystemUsage>, TelemetryFailure> =
        call { fetchFileSystems(it) }
    override suspend fun fetchDiskIo(): Result<List<DiskIoRate>, TelemetryFailure> = call { fetchDiskIo(it) }
    override suspend fun fetchNetworkInterfaces(): Result<List<NetworkInterfaceUsage>, TelemetryFailure> =
        call { fetchNetworkInterfaces(it) }
    override suspend fun fetchSensors(): Result<List<SensorReading>, TelemetryFailure> = call { fetchSensors(it) }
    override suspend fun fetchContainers(): Result<List<ContainerUsage>, TelemetryFailure> =
        call { fetchContainers(it) }
    override suspend fun fetchGpus(): Result<List<GpuUsage>, TelemetryFailure> = call { fetchGpus(it) }
    override suspend fun fetchProcessCount(): Result<ProcessCount, TelemetryFailure> = call { fetchProcessCount(it) }
    override suspend fun fetchProcesses(): Result<List<ProcessUsage>, TelemetryFailure> = call { fetchProcesses(it) }
}
