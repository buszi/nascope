package io.buszi.nascope.network.telemetry

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import io.buszi.nascope.domain.model.telemetry.ContainerStatus
import io.buszi.nascope.domain.model.telemetry.CpuCoreUsage
import io.buszi.nascope.domain.model.telemetry.CpuInfo
import io.buszi.nascope.domain.model.telemetry.CpuUsage
import io.buszi.nascope.domain.model.telemetry.FileSystemUsage
import io.buszi.nascope.domain.model.telemetry.LoadAverage
import io.buszi.nascope.domain.model.telemetry.MemoryUsage
import io.buszi.nascope.domain.model.telemetry.ProcessCount
import io.buszi.nascope.domain.model.telemetry.SensorKind
import io.buszi.nascope.domain.model.telemetry.SensorReading
import io.buszi.nascope.domain.model.telemetry.SwapUsage
import io.buszi.nascope.domain.model.telemetry.SystemInfo
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal fun parseSystem(body: String): Result<SystemInfo, TelemetryFailure> =
    parseObject(body, SystemDto.serializer()) { d ->
        SystemInfo(
            hostname = d.hostname ?: return@parseObject null,
            osName = d.osName ?: return@parseObject null,
            osVersion = d.osVersion,
            distribution = d.linuxDistro,
            architecture = d.platform,
            displayName = d.hrName,
        )
    }

private val UPTIME_REGEX = Regex("""^(?:(\d+) days?, )?(\d+):(\d{2}):(\d{2})""")

/** Accepts `"0:03:14"` and `"2 days, 4:05:06"`. */
internal fun parseUptime(body: String): Result<Duration, TelemetryFailure> {
    val root = parseRoot(body).getOrElse { return Err(it) }
    val text = (root as? JsonPrimitive)?.takeIf { it.isString }?.content
        ?: return invalid("Expected a JSON string")
    val match = UPTIME_REGEX.find(text.trim()) ?: return invalid("Unrecognised uptime: $text")
    val (days, hours, minutes, seconds) = match.destructured.let {
        listOf(it.component1(), it.component2(), it.component3(), it.component4())
    }
    return Ok(
        (days.toIntOrNull() ?: 0).days + hours.toInt().hours + minutes.toInt().minutes + seconds.toInt().seconds,
    )
}

internal fun parseLoad(body: String): Result<LoadAverage, TelemetryFailure> =
    parseObject(body, LoadDto.serializer()) { d ->
        LoadAverage(
            min1 = d.min1?.toFloat() ?: return@parseObject null,
            min5 = d.min5?.toFloat() ?: return@parseObject null,
            min15 = d.min15?.toFloat() ?: return@parseObject null,
            logicalCores = d.cpuCore,
        )
    }

internal fun parseCpu(body: String): Result<CpuUsage, TelemetryFailure> =
    parseObject(body, CpuDto.serializer()) { d ->
        CpuUsage(
            totalPercent = d.total?.toFloat() ?: return@parseObject null,
            userPercent = d.user?.toFloat(),
            systemPercent = d.system?.toFloat(),
            ioWaitPercent = d.ioWait?.toFloat(),
            idlePercent = d.idle?.toFloat(),
            logicalCores = d.cpuCore,
        )
    }

internal fun parseCpuCores(body: String): Result<List<CpuCoreUsage>, TelemetryFailure> =
    parseList(body, PerCpuDto.serializer()) { d ->
        CpuCoreUsage(
            index = d.cpuNumber ?: return@parseList null,
            totalPercent = d.total?.toFloat() ?: return@parseList null,
        )
    }

/** `cpu_hz*` is assumed to be in Hz; confirmed on real hardware in TASK-5.6. */
internal fun parseCpuInfo(body: String): Result<CpuInfo, TelemetryFailure> =
    parseObject(body, QuicklookDto.serializer()) { d ->
        CpuInfo(
            name = d.cpuName,
            currentFrequencyHz = d.cpuHzCurrent?.toLong(),
            maxFrequencyHz = d.cpuHz?.toLong(),
        )
    }

internal fun parseMemory(body: String): Result<MemoryUsage, TelemetryFailure> =
    parseObject(body, MemoryDto.serializer()) { d ->
        MemoryUsage(
            totalBytes = d.total ?: return@parseObject null,
            usedBytes = d.used ?: return@parseObject null,
            freeBytes = d.free ?: return@parseObject null,
            availableBytes = d.available,
            cachedBytes = d.cached,
            buffersBytes = d.buffers,
        )
    }

internal fun parseSwap(body: String): Result<SwapUsage, TelemetryFailure> =
    parseObject(body, SwapDto.serializer()) { d ->
        SwapUsage(
            totalBytes = d.total ?: return@parseObject null,
            usedBytes = d.used ?: return@parseObject null,
            freeBytes = d.free ?: return@parseObject null,
        )
    }

internal fun parseFileSystems(body: String): Result<List<FileSystemUsage>, TelemetryFailure> =
    parseList(body, FileSystemDto.serializer()) { d ->
        FileSystemUsage(
            mountPoint = d.mountPoint ?: return@parseList null,
            deviceName = d.deviceName,
            fsType = d.fsType,
            totalBytes = d.size ?: return@parseList null,
            usedBytes = d.used ?: return@parseList null,
            freeBytes = d.free ?: return@parseList null,
            options = d.options,
        )
    }

internal fun parseProcessCount(body: String): Result<ProcessCount, TelemetryFailure> =
    parseObject(body, ProcessCountDto.serializer()) { d ->
        ProcessCount(
            total = d.total ?: return@parseObject null,
            running = d.running ?: 0,
            sleeping = d.sleeping ?: 0,
            threads = d.threads ?: 0,
        )
    }

/** Item shape follows the Glances documentation; confirmed against real hardware in TASK-5.6. */
internal fun parseSensors(body: String): Result<List<SensorReading>, TelemetryFailure> =
    parseList(body, SensorDto.serializer()) { d ->
        SensorReading(
            label = d.label ?: return@parseList null,
            kind = sensorKindOf(d.type),
            value = d.value?.toFloat() ?: return@parseList null,
            unit = d.unit,
            warning = d.warning?.toFloat(),
            critical = d.critical?.toFloat(),
        )
    }

internal fun sensorKindOf(type: String?): SensorKind = when (type) {
    "temperature_core" -> SensorKind.CpuTemperature
    "temperature_hdd" -> SensorKind.DriveTemperature
    "fan_speed" -> SensorKind.FanSpeed
    "battery" -> SensorKind.Battery
    else -> SensorKind.Other
}

internal fun containerStatusOf(status: String?): ContainerStatus = when (status?.lowercase()) {
    // "healthy" / "unhealthy" / "starting" are Docker health states of a running container.
    "running", "healthy", "unhealthy", "starting" -> ContainerStatus.Running
    "paused" -> ContainerStatus.Paused
    "restarting" -> ContainerStatus.Restarting
    "exited" -> ContainerStatus.Exited
    "created" -> ContainerStatus.Created
    "dead" -> ContainerStatus.Dead
    else -> ContainerStatus.Unknown
}

/** Glances sends `[""]` for containers whose image it cannot resolve. */
internal fun List<String>?.firstImageOrNull(): String? = this?.firstOrNull()?.takeIf { it.isNotBlank() }
