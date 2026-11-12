package io.buszi.nascope.network.telemetry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SystemDto(
    val hostname: String? = null,
    @SerialName("os_name") val osName: String? = null,
    @SerialName("os_version") val osVersion: String? = null,
    @SerialName("linux_distro") val linuxDistro: String? = null,
    val platform: String? = null,
    @SerialName("hr_name") val hrName: String? = null,
)

@Serializable
internal data class LoadDto(
    val min1: Double? = null,
    val min5: Double? = null,
    val min15: Double? = null,
    @SerialName("cpucore") val cpuCore: Int? = null,
)

@Serializable
internal data class CpuDto(
    val total: Double? = null,
    val user: Double? = null,
    val system: Double? = null,
    @SerialName("iowait") val ioWait: Double? = null,
    val idle: Double? = null,
    @SerialName("cpucore") val cpuCore: Int? = null,
)

@Serializable
internal data class PerCpuDto(
    @SerialName("cpu_number") val cpuNumber: Int? = null,
    val total: Double? = null,
)

@Serializable
internal data class QuicklookDto(
    @SerialName("cpu_name") val cpuName: String? = null,
    @SerialName("cpu_hz_current") val cpuHzCurrent: Double? = null,
    @SerialName("cpu_hz") val cpuHz: Double? = null,
)

@Serializable
internal data class MemoryDto(
    val total: Long? = null,
    val used: Long? = null,
    val free: Long? = null,
    val available: Long? = null,
    val cached: Long? = null,
    val buffers: Long? = null,
)

@Serializable
internal data class SwapDto(
    val total: Long? = null,
    val used: Long? = null,
    val free: Long? = null,
)

@Serializable
internal data class FileSystemDto(
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("fs_type") val fsType: String? = null,
    @SerialName("mnt_point") val mountPoint: String? = null,
    val options: String? = null,
    val size: Long? = null,
    val used: Long? = null,
    val free: Long? = null,
)

@Serializable
internal data class GpuDto(
    @SerialName("gpu_id") val gpuId: String? = null,
    val name: String? = null,
    @SerialName("proc") val utilization: Double? = null,
    @SerialName("mem") val memory: Double? = null,
    val temperature: Double? = null,
    @SerialName("fan_speed") val fanSpeed: Double? = null,
)

@Serializable
internal data class ProcessCountDto(
    val total: Int? = null,
    val running: Int? = null,
    val sleeping: Int? = null,
    @SerialName("thread") val threads: Int? = null,
)

@Serializable
internal data class SensorDto(
    val label: String? = null,
    val value: Double? = null,
    val unit: String? = null,
    val type: String? = null,
    val warning: Double? = null,
    val critical: Double? = null,
)
