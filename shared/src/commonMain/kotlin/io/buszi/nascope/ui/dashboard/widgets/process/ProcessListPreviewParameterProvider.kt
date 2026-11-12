package io.buszi.nascope.ui.dashboard.widgets.process

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.ProcessUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessListViewModel.ViewState

class ProcessListPreviewParameterProvider : PreviewParameterProvider<ViewState> {
    override val values: Sequence<ViewState> = sequenceOf(
        // 1. Ready - Sorted by CPU
        ViewState(
            processes = sampleProcesses.sortedByDescending { it.cpuPercent },
            totalProcessCount = 184,
            runningProcessCount = 3,
            sortBy = ProcessSortBy.Cpu,
            overallStatus = StatusLevel.Operational,
            widgetState = WidgetState.Ready,
        ),

        // 2. Ready - Sorted by Memory
        ViewState(
            processes = sampleProcesses.sortedByDescending { it.memoryPercent },
            totalProcessCount = 184,
            runningProcessCount = 3,
            sortBy = ProcessSortBy.Memory,
            overallStatus = StatusLevel.Operational,
            widgetState = WidgetState.Ready,
        ),

        // 3. High Load / Elevated State
        ViewState(
            processes = listOf(
                sampleProcesses.first().copy(cpuPercent = 72.5f),
            ) + sampleProcesses.drop(1),
            totalProcessCount = 192,
            runningProcessCount = 8,
            sortBy = ProcessSortBy.Cpu,
            overallStatus = StatusLevel.Degraded,
            widgetState = WidgetState.Ready,
        ),

        // 4. Host without processcount data: the counts are left out
        ViewState(
            processes = sampleProcesses,
            totalProcessCount = null,
            runningProcessCount = null,
            widgetState = WidgetState.Ready,
        ),

        // 5. Stale
        ViewState(processes = sampleProcesses, widgetState = WidgetState.Stale),

        // 6. Unavailable
        ViewState(widgetState = WidgetState.Unavailable),

        // 7. Loading State
        ViewState(
            widgetState = WidgetState.Loading,
        ),
    )

    private companion object {
        val sampleProcesses = listOf(
            ProcessUi(
                pid = 1042,
                name = "qemu-system-x86",
                user = "libvirt",
                cpuPercent = 28.4f,
                memoryPercent = 14.2f,
                memoryRssBytes = 4_870_000_000L,
                status = "R",
            ),
            ProcessUi(
                pid = 2391,
                name = "plex-media-srv",
                user = "plex",
                cpuPercent = 16.8f,
                memoryPercent = 8.5f,
                memoryRssBytes = 2_920_000_000L,
                status = "S",
            ),
            ProcessUi(
                pid = 1820,
                name = "dockerd",
                user = "root",
                cpuPercent = 6.2f,
                memoryPercent = 3.1f,
                memoryRssBytes = 1_060_000_000L,
                status = "S",
            ),
            ProcessUi(
                pid = 3410,
                name = "postgres",
                user = "postgres",
                cpuPercent = 4.5f,
                memoryPercent = 5.8f,
                memoryRssBytes = 1_990_000_000L,
                status = "S",
            ),
            ProcessUi(
                pid = 4125,
                name = "traefik",
                user = "traefik",
                cpuPercent = 2.1f,
                memoryPercent = 1.2f,
                memoryRssBytes = 412_000_000L,
                status = "S",
            ),
            ProcessUi(
                pid = 5122,
                name = "node",
                user = "app",
                cpuPercent = 1.9f,
                memoryPercent = 3.8f,
                memoryRssBytes = 1_300_000_000L,
                status = "S",
            ),
            ProcessUi(
                pid = 890,
                name = "systemd-journal",
                user = "root",
                cpuPercent = 0.8f,
                memoryPercent = 1.5f,
                memoryRssBytes = 515_000_000L,
                status = "S",
            ),
            ProcessUi(
                pid = 6210,
                name = "redis-server",
                user = "redis",
                cpuPercent = 0.5f,
                memoryPercent = 2.4f,
                memoryRssBytes = 824_000_000L,
                status = "S",
            ),
        )
    }
}
