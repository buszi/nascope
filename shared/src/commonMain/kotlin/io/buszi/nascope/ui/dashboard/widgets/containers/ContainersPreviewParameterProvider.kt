package io.buszi.nascope.ui.dashboard.widgets.containers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.model.ContainerStateUi
import io.buszi.nascope.ui.common.model.ContainerUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersViewModel.ViewState

class ContainersPreviewParameterProvider : PreviewParameterProvider<ViewState> {
    override val values: Sequence<ViewState> = sequenceOf(
        // 1. Ready - Compact view (Collapsed)
        ViewState(
            engine = "Docker",
            containers = sampleContainers,
            isExpanded = false,
            widgetState = WidgetState.Ready,
        ),

        // 2. Ready - Expanded view
        ViewState(
            engine = "Docker",
            containers = sampleContainers,
            isExpanded = true,
            widgetState = WidgetState.Ready,
        ),

        // 3. Degraded / Attention State
        ViewState(
            engine = "Podman",
            containers = sampleContainers.take(3).map { it.copy(state = ContainerStateUi.Paused) },
            isExpanded = false,
            widgetState = WidgetState.Ready,
        ),

        // 4. Empty Containers
        ViewState(
            engine = "Docker",
            containers = emptyList(),
            isExpanded = false,
            widgetState = WidgetState.Ready,
        ),

        // 5. Stale
        ViewState(
            engine = "Docker",
            containers = sampleContainers,
            widgetState = WidgetState.Stale,
        ),

        // 6. Unavailable (no Docker socket mounted: the containers plugin has no data)
        ViewState(widgetState = WidgetState.Unavailable),

        // 7. Podman reporting no metrics for a stopped and a created container, no engine tag
        ViewState(
            containers = listOf(
                ContainerUi("a1", "job-runner", "", ContainerStateUi.Exited, null, null),
                ContainerUi("a2", "staging", "", ContainerStateUi.Created, null, null),
                ContainerUi("a3", "legacy", "app:1", ContainerStateUi.Dead, null, null),
            ),
            widgetState = WidgetState.Ready,
        ),

        // 8. Loading State
        ViewState(
            widgetState = WidgetState.Loading,
        ),
    )

    private companion object {
        val sampleContainers = listOf(
            ContainerUi(
                id = "7f3b8a1c",
                name = "traefik",
                image = "traefik:v3.1",
                state = ContainerStateUi.Running,
                cpuPercent = 1.4f,
                memoryUsageBytes = 48_500_000L,
                memoryLimitBytes = 256_000_000L,
                uptime = "Up 14 days",
            ),
            ContainerUi(
                id = "e4a2d981",
                name = "nextcloud",
                image = "nextcloud:29.0-fpm",
                state = ContainerStateUi.Running,
                cpuPercent = 3.2f,
                memoryUsageBytes = 285_000_000L,
                memoryLimitBytes = 1_024_000_000L,
                uptime = "Up 14 days",
            ),
            ContainerUi(
                id = "c18f9024",
                name = "postgres-db",
                image = "postgres:16-alpine",
                state = ContainerStateUi.Running,
                cpuPercent = 0.8f,
                memoryUsageBytes = 194_000_000L,
                memoryLimitBytes = 512_000_000L,
                uptime = "Up 14 days",
            ),
            ContainerUi(
                id = "92d05f3b",
                name = "plex-server",
                image = "linuxserver/plex:latest",
                state = ContainerStateUi.Running,
                cpuPercent = 8.5f,
                memoryUsageBytes = 640_000_000L,
                memoryLimitBytes = 2_048_000_000L,
                uptime = "Up 3 days",
            ),
            ContainerUi(
                id = "4b71e86a",
                name = "vaultwarden",
                image = "vaultwarden/server:latest",
                state = ContainerStateUi.Running,
                cpuPercent = 0.2f,
                memoryUsageBytes = 32_000_000L,
                memoryLimitBytes = 128_000_000L,
                uptime = "Up 14 days",
            ),
            ContainerUi(
                id = "3c89b21f",
                name = "redis-cache",
                image = "redis:7.2-alpine",
                state = ContainerStateUi.Running,
                cpuPercent = 0.4f,
                memoryUsageBytes = 18_000_000L,
                memoryLimitBytes = 128_000_000L,
                uptime = "Up 14 days",
            ),
            ContainerUi(
                id = "d58e310a",
                name = "qbittorrent",
                image = "linuxserver/qbittorrent:latest",
                state = ContainerStateUi.Paused,
                cpuPercent = 0.0f,
                memoryUsageBytes = 112_000_000L,
                memoryLimitBytes = 1_024_000_000L,
                uptime = "Paused",
            ),
            ContainerUi(
                id = "1a84f67c",
                name = "borgmatic-backup",
                image = "b3vis/borgmatic:latest",
                state = ContainerStateUi.Exited,
                cpuPercent = 0.0f,
                memoryUsageBytes = 0L,
                memoryLimitBytes = 512_000_000L,
                uptime = "Exited (0) 6h ago",
            ),
        )
    }
}
