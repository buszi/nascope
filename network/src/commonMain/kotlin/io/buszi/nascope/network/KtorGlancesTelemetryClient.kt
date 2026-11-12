package io.buszi.nascope.network

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
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
import io.buszi.nascope.network.telemetry.parseAlerts
import io.buszi.nascope.network.telemetry.parseCpu
import io.buszi.nascope.network.telemetry.parseCpuCores
import io.buszi.nascope.network.telemetry.parseCpuInfo
import io.buszi.nascope.network.telemetry.parseFileSystems
import io.buszi.nascope.network.telemetry.parseGpus
import io.buszi.nascope.network.telemetry.parseLoad
import io.buszi.nascope.network.telemetry.parseMemory
import io.buszi.nascope.network.telemetry.parsePlugins
import io.buszi.nascope.network.telemetry.parseProcessCount
import io.buszi.nascope.network.telemetry.parseServerTime
import io.buszi.nascope.network.telemetry.parseSensors
import io.buszi.nascope.network.telemetry.parseSwap
import io.buszi.nascope.network.telemetry.parseSystem
import io.buszi.nascope.network.telemetry.parseUptime
import io.buszi.nascope.network.telemetry.v3.parseV3Containers
import io.buszi.nascope.network.telemetry.v3.parseV3DiskIo
import io.buszi.nascope.network.telemetry.v3.parseV3Network
import io.buszi.nascope.network.telemetry.v3.parseV3Processes
import io.buszi.nascope.network.telemetry.v4.parsePrograms
import io.buszi.nascope.network.telemetry.v4.parseV4Containers
import io.buszi.nascope.network.telemetry.v4.parseV4DiskIo
import io.buszi.nascope.network.telemetry.v4.parseV4Network
import io.buszi.nascope.network.telemetry.v4.parseV4Processes
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import org.koin.core.annotation.Single
import kotlin.time.Duration
import kotlin.time.Instant

@Single
internal class KtorGlancesTelemetryClient(
    private val client: HttpClient,
) : GlancesTelemetryClient {

    override suspend fun fetchPlugins(server: ServerConnection): Result<Set<String>, TelemetryFailure> =
        get(server, "pluginslist", ::parsePlugins)

    override suspend fun fetchSystem(server: ServerConnection): Result<SystemInfo, TelemetryFailure> =
        get(server, "system", ::parseSystem)

    override suspend fun fetchUptime(server: ServerConnection): Result<Duration, TelemetryFailure> =
        get(server, "uptime", ::parseUptime)

    override suspend fun fetchLoad(server: ServerConnection): Result<LoadAverage, TelemetryFailure> =
        get(server, "load", ::parseLoad)

    override suspend fun fetchCpu(server: ServerConnection): Result<CpuUsage, TelemetryFailure> =
        get(server, "cpu", ::parseCpu)

    override suspend fun fetchCpuCores(server: ServerConnection): Result<List<CpuCoreUsage>, TelemetryFailure> =
        get(server, "percpu", ::parseCpuCores)

    override suspend fun fetchCpuInfo(server: ServerConnection): Result<CpuInfo, TelemetryFailure> =
        get(server, "quicklook", ::parseCpuInfo)

    override suspend fun fetchMemory(server: ServerConnection): Result<MemoryUsage, TelemetryFailure> =
        get(server, "mem", ::parseMemory)

    override suspend fun fetchSwap(server: ServerConnection): Result<SwapUsage, TelemetryFailure> =
        get(server, "memswap", ::parseSwap)

    override suspend fun fetchFileSystems(server: ServerConnection): Result<List<FileSystemUsage>, TelemetryFailure> =
        get(server, "fs", ::parseFileSystems)

    override suspend fun fetchDiskIo(server: ServerConnection): Result<List<DiskIoRate>, TelemetryFailure> =
        get(server, "diskio", versioned(server, ::parseV3DiskIo, ::parseV4DiskIo))

    override suspend fun fetchNetworkInterfaces(
        server: ServerConnection,
    ): Result<List<NetworkInterfaceUsage>, TelemetryFailure> =
        get(server, "network", versioned(server, ::parseV3Network, ::parseV4Network))

    override suspend fun fetchSensors(server: ServerConnection): Result<List<SensorReading>, TelemetryFailure> =
        get(server, "sensors", ::parseSensors)

    override suspend fun fetchContainers(server: ServerConnection): Result<List<ContainerUsage>, TelemetryFailure> =
        get(server, "containers", versioned(server, ::parseV3Containers, ::parseV4Containers))

    override suspend fun fetchGpus(server: ServerConnection): Result<List<GpuUsage>, TelemetryFailure> =
        get(server, "gpu", ::parseGpus)

    override suspend fun fetchAlerts(server: ServerConnection): Result<List<GlancesAlert>, TelemetryFailure> =
        get(server, "alert", ::parseAlerts)

    override suspend fun fetchServerTime(server: ServerConnection): Result<Instant, TelemetryFailure> =
        get(server, "now", ::parseServerTime)

    override suspend fun fetchProcessCount(server: ServerConnection): Result<ProcessCount, TelemetryFailure> =
        get(server, "processcount", ::parseProcessCount)

    override suspend fun fetchProcesses(server: ServerConnection): Result<List<ProcessUsage>, TelemetryFailure> =
        get(server, "processlist", versioned(server, ::parseV3Processes, ::parseV4Processes))

    override suspend fun fetchPrograms(server: ServerConnection): Result<List<ProgramUsage>, TelemetryFailure> =
        get(server, "programlist", ::parsePrograms)

    private fun <T> versioned(
        server: ServerConnection,
        v3: (String) -> Result<T, TelemetryFailure>,
        v4: (String) -> Result<T, TelemetryFailure>,
    ): (String) -> Result<T, TelemetryFailure> = when (server.apiVersion) {
        GlancesApiVersion.V3 -> v3
        GlancesApiVersion.V4 -> v4
    }

    private suspend fun <T> get(
        server: ServerConnection,
        plugin: String,
        parse: (String) -> Result<T, TelemetryFailure>,
    ): Result<T, TelemetryFailure> {
        val url = server.address.apiUrl(server.apiVersion, plugin)
        return when (val result = client.glancesGet(url, server.credentials)) {
            is GlancesHttpResult.Failed -> Err(TelemetryFailure.Connection(result.failure))
            is GlancesHttpResult.Response -> when (result.status) {
                HttpStatusCode.OK -> parse(result.body)
                HttpStatusCode.Unauthorized -> Err(TelemetryFailure.Connection(ConnectionFailure.Unauthorized))
                HttpStatusCode.BadRequest -> Err(TelemetryFailure.PluginUnavailable)
                // The whole API is missing: a wrong host, port or API version, not one absent plugin.
                HttpStatusCode.NotFound -> Err(TelemetryFailure.Connection(ConnectionFailure.NotGlances))
                else -> Err(TelemetryFailure.Connection(ConnectionFailure.Unexpected("HTTP ${result.status.value}")))
            }
        }
    }
}
