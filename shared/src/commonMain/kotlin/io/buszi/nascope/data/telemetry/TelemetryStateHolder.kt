package io.buszi.nascope.data.telemetry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

/**
 * Encapsulates telemetry for a temperature sensor.
 */
data class TemperatureSensorData(
    val label: String,
    val currentTempC: Float,
    val warningThresholdC: Float = 70.0f,
    val criticalThresholdC: Float = 85.0f,
    val history: List<Float> = emptyList(),
)

/**
 * Encapsulates cooling fan speed telemetry and duty cycle.
 */
data class FanSensorData(
    val label: String,
    val rpm: Int,
    val maxRpm: Int = 2200,
    val dutyCyclePercent: Float = if (maxRpm > 0) (rpm.toFloat() / maxRpm).coerceIn(0f, 1f) else 0f,
    val rpmHistory: List<Float> = emptyList(),
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
    val recentLogs: List<String> = emptyList(),
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

    private val _sensorsThermals = MutableStateFlow(
        SensorsThermalsData(
            cpuTemperature = TemperatureSensorData(
                label = "CPU Package",
                currentTempC = 58.0f,
                warningThresholdC = 75.0f,
                criticalThresholdC = 85.0f,
                    history = listOf(
                        58.0f, 61.1f, 60.5f, 59.9f, 61.3f, 60.3f, 56.5f, 55.1f,
                        56.2f, 55.4f, 54.4f, 57.1f, 60.4f, 60.3f, 59.8f, 61.5f,
                        61.1f, 57.4f, 55.5f, 56.3f, 55.5f, 54.0f, 56.1f, 59.6f,
                    ),
            ),
            driveTemperatures = listOf(
                TemperatureSensorData(
                    label = "nvme0n1 (NVMe)",
                    currentTempC = 41.0f,
                    warningThresholdC = 65.0f,
                    criticalThresholdC = 75.0f,
                    history = listOf(
                        39.6f, 40.4f, 39.1f, 40.0f, 40.8f, 39.5f, 40.3f, 41.1f,
                        39.8f, 40.7f, 41.5f, 40.2f, 41.1f, 41.8f, 40.6f, 41.5f,
                        42.2f, 40.9f, 41.8f, 42.5f, 41.3f, 42.2f, 42.9f, 41.6f,
                    ),
                ),
                TemperatureSensorData(
                    label = "sda (WD Red 8TB)",
                    currentTempC = 34.0f,
                    warningThresholdC = 50.0f,
                    criticalThresholdC = 60.0f,
                    history = listOf(
                        33.6f, 33.9f, 33.8f, 33.4f, 33.3f, 33.7f, 35.1f, 34.9f,
                        34.6f, 34.6f, 34.9f, 35.1f, 33.6f, 33.3f, 33.4f, 33.8f,
                        33.9f, 33.6f, 34.5f, 34.7f, 35.0f, 35.0f, 34.7f, 34.5f,
                    ),
                ),
                TemperatureSensorData(
                    label = "sdb (WD Red 8TB)",
                    currentTempC = 36.0f,
                    warningThresholdC = 50.0f,
                    criticalThresholdC = 60.0f,
                    history = listOf(
                        36.0f, 36.5f, 36.9f, 37.2f, 37.4f, 37.5f, 37.4f, 37.2f,
                        36.9f, 36.5f, 36.0f, 35.5f, 35.1f, 34.6f, 34.3f, 34.1f,
                        34.1f, 34.2f, 34.4f, 34.7f, 35.2f, 35.6f, 36.1f, 36.5f,
                    ),
                ),
            ),
            fans = listOf(
                FanSensorData(
                    label = "CPU Fan",
                    rpm = 1380,
                    maxRpm = 2200,
                    rpmHistory = listOf(
                        1380.0f, 1463.5f, 1428.8f, 1495.7f, 1453.1f, 1358.4f, 1375.7f, 1281.9f,
                        1268.0f, 1345.0f, 1319.6f, 1416.3f, 1480.4f, 1434.9f, 1489.4f, 1421.5f,
                        1332.1f, 1352.9f, 1265.6f, 1281.5f, 1365.1f, 1346.3f, 1449.4f, 1489.1f,
                    ),
                ),
                FanSensorData(
                    label = "Chassis 1",
                    rpm = 920,
                    maxRpm = 1800,
                    rpmHistory = listOf(
                        895.0f, 901.7f, 902.3f, 896.1f, 948.9f, 947.3f, 952.8f, 960.3f,
                        902.9f, 898.3f, 890.6f, 887.0f, 950.7f, 958.4f, 962.9f, 960.2f,
                        892.7f, 887.3f, 889.0f, 896.2f, 962.3f, 961.7f, 954.9f, 948.2f,
                    ),
                ),
                FanSensorData(
                    label = "Chassis 2",
                    rpm = 890,
                    maxRpm = 1800,
                    rpmHistory = listOf(
                        915.2f, 919.6f, 919.2f, 914.3f, 905.5f, 894.2f, 882.3f, 871.6f,
                        863.9f, 860.2f, 861.2f, 866.8f, 876.1f, 887.5f, 899.3f, 909.7f,
                        917.0f, 920.0f, 918.2f, 912.0f, 902.4f, 890.7f, 879.0f, 869.0f,
                    ),
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
                    recentLogs = listOf(
                        "10:41:58 INFO  router nextcloud@docker healthy",
                        "10:42:03 INFO  GET /dashboard/api/overview 200 4ms",
                        "10:42:11 INFO  certificate renewed for cloud.example.com",
                        "10:42:19 INFO  GET /dashboard/api/http/routers 200 6ms",
                    ),
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
                    recentLogs = listOf(
                        "10:41:52 [notice] ready to handle connections",
                        "10:42:01 GET /remote.php/dav/files/alpha/ 207 38ms",
                        "10:42:07 GET /index.php/apps/files/api/v1/stats 200 21ms",
                        "10:42:16 POST /index.php/login/v2/poll 200 12ms",
                    ),
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
                    recentLogs = listOf(
                        "10:40:12 LOG:  checkpoint starting: time",
                        "10:40:12 LOG:  checkpoint complete: wrote 142 buffers (0.9%)",
                        "10:41:40 LOG:  connection received: host=172.18.0.4",
                        "10:42:02 LOG:  connection authorized: user=nextcloud database=nc",
                    ),
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
                    recentLogs = listOf(
                        "10:41:30 INFO  Library scan completed in 4.2s",
                        "10:41:58 INFO  Transcode session started: 1080p -> 720p",
                        "10:42:09 INFO  Client 192.168.1.54 requested /library/sections",
                        "10:42:18 INFO  Transcode session 1 progress 62%",
                    ),
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
                    recentLogs = listOf(
                        "10:30:00 [INFO] Rocket has launched from http://0.0.0.0:80",
                        "10:38:14 [INFO] POST /identity/accounts/prelogin 200",
                        "10:41:05 [INFO] GET /api/sync 200 (cache hit)",
                    ),
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
                    recentLogs = listOf(
                        "10:00:01 * Background saving started by pid 21",
                        "10:00:01 * DB saved on disk",
                        "10:41:10 * 100 changes in 300 seconds. Saving...",
                    ),
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
                    recentLogs = listOf(
                        "09:12:44 INFO  Torrent ubuntu-24.04.iso paused",
                        "09:12:44 INFO  Paused all active transfers (3)",
                        "09:12:45 INFO  Container paused by user",
                    ),
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
                    recentLogs = listOf(
                        "04:00:01 borgmatic: Creating archive at /mnt/backup::alpha-2026-11-11",
                        "04:06:47 borgmatic: Pruning archives",
                        "04:06:52 borgmatic: Finished backup successfully (exit 0)",
                    ),
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
                ProcessInfo(
                    pid = 7015,
                    name = "sshd",
                    user = "root",
                    cpuPercent = 0.3f,
                    memoryPercent = 0.1f,
                    memoryRssBytes = 38_000_000L,
                    status = "S",
                ),
                ProcessInfo(
                    pid = 7344,
                    name = "tailscaled",
                    user = "root",
                    cpuPercent = 0.9f,
                    memoryPercent = 0.4f,
                    memoryRssBytes = 138_000_000L,
                    status = "S",
                ),
                ProcessInfo(
                    pid = 7890,
                    name = "smbd",
                    user = "nobody",
                    cpuPercent = 1.4f,
                    memoryPercent = 0.7f,
                    memoryRssBytes = 240_000_000L,
                    status = "S",
                ),
                ProcessInfo(
                    pid = 8120,
                    name = "nginx",
                    user = "www-data",
                    cpuPercent = 0.6f,
                    memoryPercent = 0.3f,
                    memoryRssBytes = 102_000_000L,
                    status = "S",
                ),
                ProcessInfo(
                    pid = 8455,
                    name = "jellyfin",
                    user = "jellyfin",
                    cpuPercent = 3.7f,
                    memoryPercent = 4.1f,
                    memoryRssBytes = 1_410_000_000L,
                    status = "S",
                ),
                ProcessInfo(
                    pid = 8801,
                    name = "borg",
                    user = "backup",
                    cpuPercent = 11.2f,
                    memoryPercent = 1.8f,
                    memoryRssBytes = 620_000_000L,
                    status = "D",
                ),
                ProcessInfo(
                    pid = 9034,
                    name = "rsync",
                    user = "backup",
                    cpuPercent = 5.6f,
                    memoryPercent = 0.2f,
                    memoryRssBytes = 64_000_000L,
                    status = "D",
                ),
                ProcessInfo(
                    pid = 9230,
                    name = "sonarr",
                    user = "media",
                    cpuPercent = 0.7f,
                    memoryPercent = 1.9f,
                    memoryRssBytes = 650_000_000L,
                    status = "S",
                ),
                ProcessInfo(
                    pid = 9355,
                    name = "radarr",
                    user = "media",
                    cpuPercent = 0.5f,
                    memoryPercent = 1.7f,
                    memoryRssBytes = 590_000_000L,
                    status = "S",
                ),
                ProcessInfo(
                    pid = 9610,
                    name = "prometheus",
                    user = "monitor",
                    cpuPercent = 1.8f,
                    memoryPercent = 2.9f,
                    memoryRssBytes = 1_000_000_000L,
                    status = "S",
                ),
                ProcessInfo(
                    pid = 9722,
                    name = "grafana-server",
                    user = "monitor",
                    cpuPercent = 0.9f,
                    memoryPercent = 1.3f,
                    memoryRssBytes = 450_000_000L,
                    status = "S",
                ),
                ProcessInfo(
                    pid = 9988,
                    name = "cron",
                    user = "root",
                    cpuPercent = 0.0f,
                    memoryPercent = 0.0f,
                    memoryRssBytes = 6_000_000L,
                    status = "S",
                ),
                ProcessInfo(
                    pid = 10240,
                    name = "defunct-worker",
                    user = "app",
                    cpuPercent = 0.0f,
                    memoryPercent = 0.0f,
                    memoryRssBytes = 0L,
                    status = "Z",
                ),
                ProcessInfo(
                    pid = 10412,
                    name = "ffmpeg",
                    user = "plex",
                    cpuPercent = 22.5f,
                    memoryPercent = 2.2f,
                    memoryRssBytes = 760_000_000L,
                    status = "R",
                ),
                ProcessInfo(
                    pid = 10566,
                    name = "python3",
                    user = "app",
                    cpuPercent = 9.8f,
                    memoryPercent = 1.1f,
                    memoryRssBytes = 380_000_000L,
                    status = "R",
                ),
                ProcessInfo(
                    pid = 10733,
                    name = "top",
                    user = "admin",
                    cpuPercent = 0.4f,
                    memoryPercent = 0.1f,
                    memoryRssBytes = 5_000_000L,
                    status = "T",
                ),
            ),
        ),
    )
    val processList: StateFlow<ProcessListData> = _processList.asStateFlow()

    fun refresh() {
        // Triggered by manual dashboard or widget retry
        _sensorsThermals.value = _sensorsThermals.value.copy()
        _containers.value = _containers.value.copy()
        _processList.value = _processList.value.copy()
    }
}
