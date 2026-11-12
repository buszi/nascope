package io.buszi.nascope.network.telemetry.v3

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import io.buszi.nascope.domain.model.telemetry.ContainerUsage
import io.buszi.nascope.domain.model.telemetry.DiskIoRate
import io.buszi.nascope.domain.model.telemetry.NetworkInterfaceUsage
import io.buszi.nascope.domain.model.telemetry.ProcessUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.network.telemetry.containerStatusOf
import io.buszi.nascope.network.telemetry.firstImageOrNull
import io.buszi.nascope.network.telemetry.decodeItems
import io.buszi.nascope.network.telemetry.invalid
import io.buszi.nascope.network.telemetry.parseList
import io.buszi.nascope.network.telemetry.parseRoot
import io.buszi.nascope.network.telemetry.ratePerSecond
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

@Serializable
internal data class V3DiskIoDto(
    @SerialName("disk_name") val diskName: String? = null,
    @SerialName("read_bytes") val readBytes: Long? = null,
    @SerialName("write_bytes") val writeBytes: Long? = null,
    @SerialName("read_count") val readCount: Long? = null,
    @SerialName("write_count") val writeCount: Long? = null,
    @SerialName("time_since_update") val timeSinceUpdate: Double? = null,
)

@Serializable
internal data class V3NetworkDto(
    @SerialName("interface_name") val name: String? = null,
    val alias: String? = null,
    val speed: Long? = null,
    @SerialName("is_up") val isUp: Boolean? = null,
    val rx: Long? = null,
    val tx: Long? = null,
    @SerialName("cumulative_rx") val cumulativeRx: Long? = null,
    @SerialName("cumulative_tx") val cumulativeTx: Long? = null,
    @SerialName("time_since_update") val timeSinceUpdate: Double? = null,
)

@Serializable
internal data class V3ContainerMemoryDto(val limit: Long? = null)

@Serializable
internal data class V3ContainerDto(
    @SerialName("Id") val id: String? = null,
    val name: String? = null,
    @SerialName("Status") val status: String? = null,
    @SerialName("Image") val image: List<String>? = null,
    val engine: String? = null,
    @SerialName("cpu_percent") val cpuPercent: Double? = null,
    @SerialName("memory_usage") val memoryUsage: Long? = null,
    val memory: V3ContainerMemoryDto? = null,
    @SerialName("Uptime") val uptime: String? = null,
)

@Serializable
internal data class V3ProcessDto(
    val pid: Int? = null,
    val name: String? = null,
    val username: String? = null,
    @SerialName("cpu_percent") val cpuPercent: Double? = null,
    @SerialName("memory_percent") val memoryPercent: Double? = null,
    /** An array in Glances 3; `rss` is first. Kept raw so an unexpected shape never fails the item. */
    @SerialName("memory_info") val memoryInfo: kotlinx.serialization.json.JsonElement? = null,
    val status: String? = null,
    val cmdline: List<String>? = null,
)

/** Glances 3 reports per-interval deltas; the rate is delta / `time_since_update`. */
internal fun parseV3DiskIo(body: String): Result<List<DiskIoRate>, TelemetryFailure> =
    parseList(body, V3DiskIoDto.serializer()) { d ->
        DiskIoRate(
            diskName = d.diskName ?: return@parseList null,
            readBytesPerSec = ratePerSecond(d.readBytes, d.timeSinceUpdate),
            writeBytesPerSec = ratePerSecond(d.writeBytes, d.timeSinceUpdate),
            // The operation counts are deltas too. The latency is not reported by Glances 3.
            readIops = d.readCount?.let { ratePerSecond(it, d.timeSinceUpdate) },
            writeIops = d.writeCount?.let { ratePerSecond(it, d.timeSinceUpdate) },
        )
    }

internal fun parseV3Network(body: String): Result<List<NetworkInterfaceUsage>, TelemetryFailure> =
    parseList(body, V3NetworkDto.serializer()) { d ->
        NetworkInterfaceUsage(
            name = d.name ?: return@parseList null,
            alias = d.alias,
            isUp = d.isUp,
            speedBitsPerSec = d.speed?.takeIf { it > 0 },
            rxBytesPerSec = ratePerSecond(d.rx, d.timeSinceUpdate),
            txBytesPerSec = ratePerSecond(d.tx, d.timeSinceUpdate),
            totalRxBytes = d.cumulativeRx ?: 0,
            totalTxBytes = d.cumulativeTx ?: 0,
        )
    }

/** Glances 3 wraps the list: `{"containers": [...]}`. */
internal fun parseV3Containers(body: String): Result<List<ContainerUsage>, TelemetryFailure> {
    val root = parseRoot(body).getOrElse { return Err(it) }
    val array = (root as? JsonObject)?.get("containers") as? JsonArray
        ?: return invalid("Expected an object with a containers array")
    return Ok(
        decodeItems(array, V3ContainerDto.serializer()) { d ->
            ContainerUsage(
                id = d.id ?: return@decodeItems null,
                name = d.name ?: return@decodeItems null,
                image = d.image.firstImageOrNull(),
                engine = d.engine,
                status = containerStatusOf(d.status),
                cpuPercent = d.cpuPercent?.toFloat(),
                memoryUsageBytes = d.memoryUsage,
                memoryLimitBytes = d.memory?.limit,
                uptimeText = d.uptime,
            )
        },
    )
}

internal fun parseV3Processes(body: String): Result<List<ProcessUsage>, TelemetryFailure> =
    parseList(body, V3ProcessDto.serializer()) { d ->
        val rss = ((d.memoryInfo as? JsonArray)?.firstOrNull() as? JsonPrimitive)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }
        ProcessUsage(
            pid = d.pid ?: return@parseList null,
            name = d.name ?: return@parseList null,
            user = d.username,
            cpuPercent = d.cpuPercent?.toFloat() ?: 0f,
            memoryPercent = d.memoryPercent?.toFloat() ?: 0f,
            memoryRssBytes = rss,
            statusCode = d.status,
            commandLine = d.cmdline,
        )
    }
