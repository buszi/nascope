package io.buszi.nascope.network.telemetry.v4

import com.github.michaelbull.result.Result
import io.buszi.nascope.domain.model.telemetry.ContainerUsage
import io.buszi.nascope.domain.model.telemetry.DiskIoRate
import io.buszi.nascope.domain.model.telemetry.NetworkInterfaceUsage
import io.buszi.nascope.domain.model.telemetry.ProcessUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.network.telemetry.containerStatusOf
import io.buszi.nascope.network.telemetry.firstImageOrNull
import io.buszi.nascope.network.telemetry.parseList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class V4DiskIoDto(
    @SerialName("disk_name") val diskName: String? = null,
    @SerialName("read_bytes_rate_per_sec") val readRate: Double? = null,
    @SerialName("write_bytes_rate_per_sec") val writeRate: Double? = null,
    @SerialName("read_count_rate_per_sec") val readIops: Double? = null,
    @SerialName("write_count_rate_per_sec") val writeIops: Double? = null,
    @SerialName("read_latency") val readLatency: Double? = null,
    @SerialName("write_latency") val writeLatency: Double? = null,
)

@Serializable
internal data class V4NetworkDto(
    @SerialName("interface_name") val name: String? = null,
    val alias: String? = null,
    val speed: Long? = null,
    @SerialName("is_up") val isUp: Boolean? = null,
    @SerialName("bytes_recv_rate_per_sec") val rxRate: Double? = null,
    @SerialName("bytes_sent_rate_per_sec") val txRate: Double? = null,
    @SerialName("bytes_recv_gauge") val rxTotal: Long? = null,
    @SerialName("bytes_sent_gauge") val txTotal: Long? = null,
)

@Serializable
internal data class V4ContainerDto(
    val id: String? = null,
    val name: String? = null,
    val status: String? = null,
    val image: List<String>? = null,
    val engine: String? = null,
    @SerialName("cpu_percent") val cpuPercent: Double? = null,
    @SerialName("memory_usage") val memoryUsage: Long? = null,
    @SerialName("memory_limit") val memoryLimit: Long? = null,
    val uptime: String? = null,
)

@Serializable
internal data class V4MemoryInfoDto(val rss: Long? = null)

@Serializable
internal data class V4ProcessDto(
    val pid: Int? = null,
    val name: String? = null,
    val username: String? = null,
    @SerialName("cpu_percent") val cpuPercent: Double? = null,
    @SerialName("memory_percent") val memoryPercent: Double? = null,
    @SerialName("memory_info") val memoryInfo: V4MemoryInfoDto? = null,
    val status: String? = null,
    val cmdline: List<String>? = null,
)

internal fun parseV4DiskIo(body: String): Result<List<DiskIoRate>, TelemetryFailure> =
    parseList(body, V4DiskIoDto.serializer()) { d ->
        DiskIoRate(
            diskName = d.diskName ?: return@parseList null,
            readBytesPerSec = d.readRate?.toLong() ?: 0,
            writeBytesPerSec = d.writeRate?.toLong() ?: 0,
            readIops = d.readIops?.toLong(),
            writeIops = d.writeIops?.toLong(),
            readLatencyMs = d.readLatency?.toLong(),
            writeLatencyMs = d.writeLatency?.toLong(),
        )
    }

internal fun parseV4Network(body: String): Result<List<NetworkInterfaceUsage>, TelemetryFailure> =
    parseList(body, V4NetworkDto.serializer()) { d ->
        NetworkInterfaceUsage(
            name = d.name ?: return@parseList null,
            alias = d.alias,
            isUp = d.isUp,
            speedBitsPerSec = d.speed?.takeIf { it > 0 },
            rxBytesPerSec = d.rxRate?.toLong() ?: 0,
            txBytesPerSec = d.txRate?.toLong() ?: 0,
            totalRxBytes = d.rxTotal ?: 0,
            totalTxBytes = d.txTotal ?: 0,
        )
    }

internal fun parseV4Containers(body: String): Result<List<ContainerUsage>, TelemetryFailure> =
    parseList(body, V4ContainerDto.serializer()) { d ->
        ContainerUsage(
            id = d.id ?: return@parseList null,
            name = d.name ?: return@parseList null,
            image = d.image.firstImageOrNull(),
            engine = d.engine,
            status = containerStatusOf(d.status),
            cpuPercent = d.cpuPercent?.toFloat(),
            memoryUsageBytes = d.memoryUsage,
            memoryLimitBytes = d.memoryLimit,
            uptimeText = d.uptime,
        )
    }

internal fun parseV4Processes(body: String): Result<List<ProcessUsage>, TelemetryFailure> =
    parseList(body, V4ProcessDto.serializer()) { d ->
        ProcessUsage(
            pid = d.pid ?: return@parseList null,
            name = d.name ?: return@parseList null,
            user = d.username,
            cpuPercent = d.cpuPercent?.toFloat() ?: 0f,
            memoryPercent = d.memoryPercent?.toFloat() ?: 0f,
            memoryRssBytes = d.memoryInfo?.rss,
            statusCode = d.status,
            commandLine = d.cmdline,
        )
    }
