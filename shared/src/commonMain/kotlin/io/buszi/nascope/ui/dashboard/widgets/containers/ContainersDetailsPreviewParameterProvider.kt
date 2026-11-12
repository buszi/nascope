package io.buszi.nascope.ui.dashboard.widgets.containers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.model.ContainerStateUi
import io.buszi.nascope.ui.common.model.ContainerUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersDetailsViewModel.ViewState

class ContainersDetailsPreviewParameterProvider : PreviewParameterProvider<ViewState> {
    override val values: Sequence<ViewState> = sequenceOf(
        // 1. Mixed states, first container expanded
        ViewState(
            engine = "Docker",
            containers = previewContainers,
            expandedContainerId = "7f3b8a1c",
            widgetState = WidgetState.Ready,
        ),

        // 2. Filtered to exited containers
        ViewState(
            engine = "Docker",
            containers = previewContainers,
            stateFilter = ContainerStateUi.Exited,
            widgetState = WidgetState.Ready,
        ),

        // 3. Filter without matches
        ViewState(
            engine = "Podman",
            containers = previewContainers,
            stateFilter = ContainerStateUi.Restarting,
            widgetState = WidgetState.Ready,
        ),

        // 4. No containers
        ViewState(engine = "Docker", widgetState = WidgetState.Ready),

        // 5. Stale
        ViewState(engine = "Docker", containers = previewContainers, widgetState = WidgetState.Stale),

        // 6. Unavailable
        ViewState(widgetState = WidgetState.Unavailable),

        // 7. Podman: no metrics for a stopped container
        ViewState(
            engine = "Podman",
            containers = listOf(ContainerUi("a1", "job-runner", "", ContainerStateUi.Exited, null, null)),
            expandedContainerId = "a1",
            widgetState = WidgetState.Ready,
        ),

        // 8. Loading
        ViewState(widgetState = WidgetState.Loading),
    )

    private companion object {
        val previewContainers = listOf(
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
                id = "d58e310a",
                name = "qbittorrent",
                image = "linuxserver/qbittorrent:latest",
                state = ContainerStateUi.Paused,
                cpuPercent = 0f,
                memoryUsageBytes = 112_000_000L,
                memoryLimitBytes = 1_024_000_000L,
                uptime = "Paused",
            ),
            ContainerUi(
                id = "1a84f67c",
                name = "borgmatic-backup",
                image = "b3vis/borgmatic:latest",
                state = ContainerStateUi.Exited,
                cpuPercent = null,
                memoryUsageBytes = null,
                memoryLimitBytes = null,
                uptime = "Exited (0) 6h ago",
            ),
        )
    }
}
