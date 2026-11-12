package io.buszi.nascope.data.telemetry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

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
        _containers.value = _containers.value.copy()
        _processList.value = _processList.value.copy()
    }
}
