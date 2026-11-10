package io.buszi.nascope.ui.dashboard.widgets.containers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.data.telemetry.ContainerInfo
import io.buszi.nascope.data.telemetry.ContainerState
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersViewModel.ViewState

class ContainersPreviewParameterProvider : PreviewParameterProvider<ViewState> {
    override val values: Sequence<ViewState> = sequenceOf(
        // 1. Ready - Compact view (Collapsed)
        ViewState(
            engine = "Docker 27.0.3",
            containers = sampleContainers,
            runningCount = 6,
            pausedCount = 1,
            stoppedCount = 1,
            totalCount = 8,
            isExpanded = false,
            overallStatus = StatusLevel.Operational,
            widgetState = WidgetState.Ready,
        ),

        // 2. Ready - Expanded view
        ViewState(
            engine = "Docker 27.0.3",
            containers = sampleContainers,
            runningCount = 6,
            pausedCount = 1,
            stoppedCount = 1,
            totalCount = 8,
            isExpanded = true,
            overallStatus = StatusLevel.Operational,
            widgetState = WidgetState.Ready,
        ),

        // 3. Degraded / Attention State
        ViewState(
            engine = "Podman 5.1.0",
            containers = sampleContainers.take(3).map { it.copy(state = ContainerState.Paused) },
            runningCount = 0,
            pausedCount = 3,
            stoppedCount = 0,
            totalCount = 3,
            isExpanded = false,
            overallStatus = StatusLevel.Degraded,
            widgetState = WidgetState.Ready,
        ),

        // 4. Empty Containers
        ViewState(
            engine = "Docker 27.0.3",
            containers = emptyList(),
            runningCount = 0,
            pausedCount = 0,
            stoppedCount = 0,
            totalCount = 0,
            isExpanded = false,
            overallStatus = StatusLevel.Operational,
            widgetState = WidgetState.Ready,
        ),

        // 5. Loading State
        ViewState(
            widgetState = WidgetState.Loading,
        ),
    )

    private companion object {
        val sampleContainers = listOf(
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
        )
    }
}
