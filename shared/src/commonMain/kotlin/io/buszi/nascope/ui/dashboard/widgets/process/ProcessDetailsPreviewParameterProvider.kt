package io.buszi.nascope.ui.dashboard.widgets.process

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.model.ProcessStateUi
import io.buszi.nascope.ui.common.model.ProcessUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.SortField
import io.buszi.nascope.ui.dashboard.widgets.process.ProcessDetailsViewModel.ViewState

class ProcessDetailsPreviewParameterProvider : PreviewParameterProvider<ViewState> {
    override val values: Sequence<ViewState> = sequenceOf(
        // 1. Default view, first row expanded
        ViewState(
            processes = previewProcesses,
            totalProcessCount = 184,
            runningProcessCount = 3,
            expandedPid = 1042,
            widgetState = WidgetState.Ready,
        ),

        // 2. Searching, sorted by name
        ViewState(
            processes = previewProcesses,
            totalProcessCount = 184,
            runningProcessCount = 3,
            query = "pos",
            sortField = SortField.Name,
            sortDescending = false,
            widgetState = WidgetState.Ready,
        ),

        // 3. Filtered to a state, no matches for the query
        ViewState(
            processes = previewProcesses,
            totalProcessCount = 184,
            runningProcessCount = 3,
            query = "zzz",
            stateFilter = ProcessStateUi.Zombie,
            widgetState = WidgetState.Ready,
        ),

        // 4. Host without processcount data and without RSS / user for a process
        ViewState(
            processes = listOf(ProcessUi(1, "init", ProcessUi.MissingUser, 0.1f, 0.2f, null, "")),
            expandedPid = 1,
            widgetState = WidgetState.Ready,
        ),

        // 5. Stale
        ViewState(processes = previewProcesses, totalProcessCount = 184, runningProcessCount = 3, widgetState = WidgetState.Stale),

        // 6. Unavailable
        ViewState(widgetState = WidgetState.Unavailable),

        // 7. Loading
        ViewState(widgetState = WidgetState.Loading),
    )

    private companion object {
        val previewProcesses = listOf(
            ProcessUi(1042, "qemu-system-x86", "libvirt", 86.4f, 14.2f, 4_870_000_000L, "R"),
            ProcessUi(2391, "plex-media-srv", "plex", 16.8f, 8.5f, 2_920_000_000L, "S"),
            ProcessUi(1820, "dockerd", "root", 6.2f, 3.1f, 1_060_000_000L, "S"),
            ProcessUi(3410, "postgres", "postgres", 4.5f, 5.8f, 1_990_000_000L, "S"),
            ProcessUi(8801, "borg", "backup", 11.2f, 1.8f, 620_000_000L, "D"),
            ProcessUi(10240, "defunct-worker", "app", 0f, 0f, 0L, "Z"),
        )
    }
}
