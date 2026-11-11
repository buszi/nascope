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
 * Encapsulates filesystem mount point and capacity telemetry for storage pools.
 */
data class StorageMountData(
    val mountPoint: String,
    val deviceName: String,
    val fsType: String,
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val isHealthy: Boolean = true,
)

/**
 * Encapsulates storage pools and disk I/O throughput rate telemetry.
 */
data class StoragePoolsData(
    val mounts: List<StorageMountData>,
    val readRateBytesPerSec: Long,
    val writeRateBytesPerSec: Long,
)

/**
 * Encapsulates telemetry for a single network interface.
 */
data class NetworkInterfaceData(
    val name: String,
    val isUp: Boolean,
    val ipAddress: String,
    val rxBytesPerSec: Long,
    val txBytesPerSec: Long,
    val totalRxBytes: Long,
    val totalTxBytes: Long,
    val rxHistory: List<Float> = emptyList(),
    val txHistory: List<Float> = emptyList(),
)

/**
 * Encapsulates overall network I/O throughput and active interfaces.
 */
data class NetworkIoData(
    val interfaces: List<NetworkInterfaceData>,
    val selectedInterfaceName: String? = null,
)

/**
 * Encapsulates telemetry for a temperature sensor.
 */
data class TemperatureSensorData(
    val label: String,
    val currentTempC: Float,
    val warningThresholdC: Float = 70.0f,
    val criticalThresholdC: Float = 85.0f,
)

/**
 * Encapsulates cooling fan speed telemetry and duty cycle.
 */
data class FanSensorData(
    val label: String,
    val rpm: Int,
    val maxRpm: Int = 2200,
    val dutyCyclePercent: Float = if (maxRpm > 0) (rpm.toFloat() / maxRpm).coerceIn(0f, 1f) else 0f,
)

/**
 * Encapsulates system thermal sensors and active cooling telemetry.
 */
data class SensorsThermalsData(
    val cpuTemperature: TemperatureSensorData? = null,
    val driveTemperatures: List<TemperatureSensorData> = emptyList(),
    val fans: List<FanSensorData> = emptyList(),
)

/**
 * Status lifecycle state for container orchestration.
 */
enum class ContainerState {
    Running,
    Paused,
    Stopped,
    Restarting,
}

/**
 * Encapsulates telemetry for a single container instance (Docker / Podman).
 */
data class ContainerInfo(
    val id: String,
    val name: String,
    val image: String,
    val state: ContainerState,
    val cpuPercent: Float,
    val memoryUsageBytes: Long,
    val memoryLimitBytes: Long? = null,
    val uptime: String,
)

/**
 * Encapsulates container engine and container list telemetry.
 */
data class ContainersData(
    val containers: List<ContainerInfo> = emptyList(),
    val engine: String = "Docker 27.0.3",
) {
    val runningCount: Int get() = containers.count { it.state == ContainerState.Running }
    val pausedCount: Int get() = containers.count { it.state == ContainerState.Paused }
    val stoppedCount: Int get() = containers.count { it.state == ContainerState.Stopped }
    val totalCount: Int get() = containers.size
}

/**
 * Encapsulates resource consumption and identity for a single host process.
 */
data class ProcessInfo(
    val pid: Int,
    val name: String,
    val user: String,
    val cpuPercent: Float,
    val memoryPercent: Float,
    val memoryRssBytes: Long = 0L,
    val status: String = "R",
)

/**
 * Encapsulates host process table and summary counts telemetry.
 */
data class ProcessListData(
    val processes: List<ProcessInfo> = emptyList(),
    val totalProcessCount: Int = 184,
    val runningProcessCount: Int = 3,
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

    private val _storagePools = MutableStateFlow(
        StoragePoolsData(
            mounts = listOf(
                StorageMountData(
                    mountPoint = "/",
                    deviceName = "/dev/nvme0n1p2",
                    fsType = "ext4",
                    totalBytes = 512_000_000_000L,
                    usedBytes = 153_600_000_000L,
                    freeBytes = 358_400_000_000L,
                    isHealthy = true,
                ),
                StorageMountData(
                    mountPoint = "/volume1",
                    deviceName = "/dev/mapper/vg0-data",
                    fsType = "btrfs",
                    totalBytes = 3_800_000_000_000L,
                    usedBytes = 2_470_000_000_000L,
                    freeBytes = 1_330_000_000_000L,
                    isHealthy = true,
                ),
                StorageMountData(
                    mountPoint = "/zpool/media",
                    deviceName = "tank/media",
                    fsType = "zfs",
                    totalBytes = 8_000_000_000_000L,
                    usedBytes = 6_880_000_000_000L,
                    freeBytes = 1_120_000_000_000L,
                    isHealthy = false,
                ),
                StorageMountData(
                    mountPoint = "/mnt/backup",
                    deviceName = "/dev/sdc1",
                    fsType = "ext4",
                    totalBytes = 2_000_000_000_000L,
                    usedBytes = 420_000_000_000L,
                    freeBytes = 1_580_000_000_000L,
                    isHealthy = true,
                ),
            ),
            readRateBytesPerSec = 42_500_000L,
            writeRateBytesPerSec = 128_300_000L,
        ),
    )
    val storagePools: StateFlow<StoragePoolsData> = _storagePools.asStateFlow()

    private val _networkIo = MutableStateFlow(
        NetworkIoData(
            interfaces = listOf(
                NetworkInterfaceData(
                    name = "eth0",
                    isUp = true,
                    ipAddress = "192.168.1.120/24",
                    rxBytesPerSec = 14_850_000L,
                    txBytesPerSec = 3_210_000L,
                    totalRxBytes = 842_300_000_000L,
                    totalTxBytes = 195_600_000_000L,
                    rxHistory = listOf(
                        8.2f, 10.5f, 12.1f, 15.4f, 14.8f, 18.2f, 21.0f, 19.5f,
                        16.3f, 14.0f, 13.8f, 15.2f, 17.5f, 14.85f, 12.0f, 14.2f,
                        16.8f, 19.1f, 15.5f, 13.9f, 14.85f, 15.1f, 14.2f, 14.85f,
                    ),
                    txHistory = listOf(
                        1.8f, 2.1f, 2.5f, 3.8f, 3.2f, 4.1f, 4.5f, 3.9f,
                        3.0f, 2.8f, 3.1f, 3.5f, 3.21f, 2.9f, 3.4f, 3.8f,
                        4.0f, 3.5f, 3.1f, 3.21f, 3.3f, 3.0f, 3.21f, 3.21f,
                    ),
                ),
                NetworkInterfaceData(
                    name = "eth1",
                    isUp = true,
                    ipAddress = "10.0.0.5/24",
                    rxBytesPerSec = 520_000L,
                    txBytesPerSec = 85_000L,
                    totalRxBytes = 124_000_000_000L,
                    totalTxBytes = 42_000_000_000L,
                    rxHistory = listOf(
                        0.3f, 0.4f, 0.5f, 0.6f, 0.52f, 0.48f, 0.55f, 0.51f,
                        0.52f, 0.6f, 0.5f, 0.45f, 0.52f, 0.58f, 0.52f, 0.49f,
                        0.53f, 0.52f, 0.51f, 0.52f, 0.54f, 0.5f, 0.52f, 0.52f,
                    ),
                    txHistory = listOf(
                        0.05f, 0.08f, 0.09f, 0.085f, 0.07f, 0.08f, 0.09f, 0.085f,
                        0.08f, 0.09f, 0.085f, 0.07f, 0.08f, 0.09f, 0.085f, 0.08f,
                        0.085f, 0.09f, 0.08f, 0.085f, 0.085f, 0.08f, 0.085f, 0.085f,
                    ),
                ),
                NetworkInterfaceData(
                    name = "wlan0",
                    isUp = false,
                    ipAddress = "Disconnected",
                    rxBytesPerSec = 0L,
                    txBytesPerSec = 0L,
                    totalRxBytes = 1_200_000_000L,
                    totalTxBytes = 450_000_000L,
                    rxHistory = List(24) { 0f },
                    txHistory = List(24) { 0f },
                ),
            ),
            selectedInterfaceName = "eth0",
        ),
    )
    val networkIo: StateFlow<NetworkIoData> = _networkIo.asStateFlow()

    private val _sensorsThermals = MutableStateFlow(
        SensorsThermalsData(
            cpuTemperature = TemperatureSensorData(
                label = "CPU Package",
                currentTempC = 58.0f,
                warningThresholdC = 75.0f,
                criticalThresholdC = 85.0f,
            ),
            driveTemperatures = listOf(
                TemperatureSensorData(
                    label = "nvme0n1 (NVMe)",
                    currentTempC = 41.0f,
                    warningThresholdC = 65.0f,
                    criticalThresholdC = 75.0f,
                ),
                TemperatureSensorData(
                    label = "sda (WD Red 8TB)",
                    currentTempC = 34.0f,
                    warningThresholdC = 50.0f,
                    criticalThresholdC = 60.0f,
                ),
                TemperatureSensorData(
                    label = "sdb (WD Red 8TB)",
                    currentTempC = 36.0f,
                    warningThresholdC = 50.0f,
                    criticalThresholdC = 60.0f,
                ),
            ),
            fans = listOf(
                FanSensorData(
                    label = "CPU Fan",
                    rpm = 1380,
                    maxRpm = 2200,
                ),
                FanSensorData(
                    label = "Chassis 1",
                    rpm = 920,
                    maxRpm = 1800,
                ),
                FanSensorData(
                    label = "Chassis 2",
                    rpm = 890,
                    maxRpm = 1800,
                ),
            ),
        ),
    )
    val sensorsThermals: StateFlow<SensorsThermalsData> = _sensorsThermals.asStateFlow()

    private val _containers = MutableStateFlow(
        ContainersData(
            engine = "Docker 27.0.3",
            containers = listOf(
                ContainerInfo(
                    id = "7f3b8a1c",
                    name = "traefik",
                    image = "traefik:v3.1",
                    state = ContainerState.Running,
                    cpuPercent = 1.4f,
                    memoryUsageBytes = 48_500_000L,
                    memoryLimitBytes = 256_000_000L,
                    uptime = "Up 14 days",
                ),
                ContainerInfo(
                    id = "e4a2d981",
                    name = "nextcloud",
                    image = "nextcloud:29.0-fpm",
                    state = ContainerState.Running,
                    cpuPercent = 3.2f,
                    memoryUsageBytes = 285_000_000L,
                    memoryLimitBytes = 1_024_000_000L,
                    uptime = "Up 14 days",
                ),
                ContainerInfo(
                    id = "c18f9024",
                    name = "postgres-db",
                    image = "postgres:16-alpine",
                    state = ContainerState.Running,
                    cpuPercent = 0.8f,
                    memoryUsageBytes = 194_000_000L,
                    memoryLimitBytes = 512_000_000L,
                    uptime = "Up 14 days",
                ),
                ContainerInfo(
                    id = "92d05f3b",
                    name = "plex-server",
                    image = "linuxserver/plex:latest",
                    state = ContainerState.Running,
                    cpuPercent = 8.5f,
                    memoryUsageBytes = 640_000_000L,
                    memoryLimitBytes = 2_048_000_000L,
                    uptime = "Up 3 days",
                ),
                ContainerInfo(
                    id = "4b71e86a",
                    name = "vaultwarden",
                    image = "vaultwarden/server:latest",
                    state = ContainerState.Running,
                    cpuPercent = 0.2f,
                    memoryUsageBytes = 32_000_000L,
                    memoryLimitBytes = 128_000_000L,
                    uptime = "Up 14 days",
                ),
                ContainerInfo(
                    id = "3c89b21f",
                    name = "redis-cache",
                    image = "redis:7.2-alpine",
                    state = ContainerState.Running,
                    cpuPercent = 0.4f,
                    memoryUsageBytes = 18_000_000L,
                    memoryLimitBytes = 128_000_000L,
                    uptime = "Up 14 days",
                ),
                ContainerInfo(
                    id = "d58e310a",
                    name = "qbittorrent",
                    image = "linuxserver/qbittorrent:latest",
                    state = ContainerState.Paused,
                    cpuPercent = 0.0f,
                    memoryUsageBytes = 112_000_000L,
                    memoryLimitBytes = 1_024_000_000L,
                    uptime = "Paused",
                ),
                ContainerInfo(
                    id = "1a84f67c",
                    name = "borgmatic-backup",
                    image = "b3vis/borgmatic:latest",
                    state = ContainerState.Stopped,
                    cpuPercent = 0.0f,
                    memoryUsageBytes = 0L,
                    memoryLimitBytes = 512_000_000L,
                    uptime = "Exited (0) 6h ago",
                ),
            ),
        ),
    )
    val containers: StateFlow<ContainersData> = _containers.asStateFlow()

    private val _processList = MutableStateFlow(
        ProcessListData(
            totalProcessCount = 184,
            runningProcessCount = 3,
            processes = listOf(
                ProcessInfo(
                    pid = 1042,
                    name = "qemu-system-x86",
                    user = "libvirt",
                    cpuPercent = 28.4f,
                    memoryPercent = 14.2f,
                    memoryRssBytes = 4_870_000_000L,
                    status = "R",
                ),
                ProcessInfo(
                    pid = 2391,
                    name = "plex-media-srv",
                    user = "plex",
                    cpuPercent = 16.8f,
                    memoryPercent = 8.5f,
                    memoryRssBytes = 2_920_000_000L,
                    status = "S",
                ),
                ProcessInfo(
                    pid = 1820,
                    name = "dockerd",
                    user = "root",
                    cpuPercent = 6.2f,
                    memoryPercent = 3.1f,
                    memoryRssBytes = 1_060_000_000L,
                    status = "S",
                ),
                ProcessInfo(
                    pid = 3410,
                    name = "postgres",
                    user = "postgres",
                    cpuPercent = 4.5f,
                    memoryPercent = 5.8f,
                    memoryRssBytes = 1_990_000_000L,
                    status = "S",
                ),
                ProcessInfo(
                    pid = 4125,
                    name = "traefik",
                    user = "traefik",
                    cpuPercent = 2.1f,
                    memoryPercent = 1.2f,
                    memoryRssBytes = 412_000_000L,
                    status = "S",
                ),
                ProcessInfo(
                    pid = 5122,
                    name = "node",
                    user = "app",
                    cpuPercent = 1.9f,
                    memoryPercent = 3.8f,
                    memoryRssBytes = 1_300_000_000L,
                    status = "S",
                ),
                ProcessInfo(
                    pid = 890,
                    name = "systemd-journal",
                    user = "root",
                    cpuPercent = 0.8f,
                    memoryPercent = 1.5f,
                    memoryRssBytes = 515_000_000L,
                    status = "S",
                ),
                ProcessInfo(
                    pid = 6210,
                    name = "redis-server",
                    user = "redis",
                    cpuPercent = 0.5f,
                    memoryPercent = 2.4f,
                    memoryRssBytes = 824_000_000L,
                    status = "S",
                ),
            ),
        ),
    )
    val processList: StateFlow<ProcessListData> = _processList.asStateFlow()

    fun refresh() {
        // Triggered by manual dashboard or widget retry
        _hostOverview.value = _hostOverview.value.copy()
        _cpuMatrix.value = _cpuMatrix.value.copy()
        _memorySwap.value = _memorySwap.value.copy()
        _storagePools.value = _storagePools.value.copy()
        _networkIo.value = _networkIo.value.copy()
        _sensorsThermals.value = _sensorsThermals.value.copy()
        _containers.value = _containers.value.copy()
        _processList.value = _processList.value.copy()
    }
}
