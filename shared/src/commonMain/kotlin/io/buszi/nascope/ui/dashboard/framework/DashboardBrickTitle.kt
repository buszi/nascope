package io.buszi.nascope.ui.dashboard.framework

import androidx.compose.runtime.Composable
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.widget_alerts_title
import io.buszi.nascope.resources.widget_containers_title
import io.buszi.nascope.resources.widget_containers_title_compact
import io.buszi.nascope.resources.widget_cpu_matrix_title
import io.buszi.nascope.resources.widget_cpu_matrix_title_compact
import io.buszi.nascope.resources.widget_gpu_title
import io.buszi.nascope.resources.widget_memory_swap_title
import io.buszi.nascope.resources.widget_memory_swap_title_compact
import io.buszi.nascope.resources.widget_network_title
import io.buszi.nascope.resources.widget_network_title_compact
import io.buszi.nascope.resources.widget_processes_title
import io.buszi.nascope.resources.widget_processes_title_compact
import io.buszi.nascope.resources.widget_sensors_title
import io.buszi.nascope.resources.widget_sensors_title_compact
import io.buszi.nascope.resources.widget_storage_title
import io.buszi.nascope.resources.widget_storage_title_compact
import io.buszi.nascope.resources.widget_system_overview_title
import org.jetbrains.compose.resources.stringResource

/**
 * Human-readable title of the brick, shared with its widget header.
 *
 * @param compact Prefer the short title for narrow places such as the phone top bar.
 */
@Composable
fun DashboardBrickId.title(compact: Boolean = false): String = stringResource(
    when (this) {
        DashboardBrickId.SystemOverview -> Res.string.widget_system_overview_title
        DashboardBrickId.CpuMatrix ->
            if (compact) Res.string.widget_cpu_matrix_title_compact else Res.string.widget_cpu_matrix_title
        DashboardBrickId.Gpu -> Res.string.widget_gpu_title
        DashboardBrickId.Alerts -> Res.string.widget_alerts_title
        DashboardBrickId.MemorySwap ->
            if (compact) Res.string.widget_memory_swap_title_compact else Res.string.widget_memory_swap_title
        DashboardBrickId.StoragePools ->
            if (compact) Res.string.widget_storage_title_compact else Res.string.widget_storage_title
        DashboardBrickId.NetworkIo ->
            if (compact) Res.string.widget_network_title_compact else Res.string.widget_network_title
        DashboardBrickId.SensorsThermals ->
            if (compact) Res.string.widget_sensors_title_compact else Res.string.widget_sensors_title
        DashboardBrickId.Containers ->
            if (compact) Res.string.widget_containers_title_compact else Res.string.widget_containers_title
        DashboardBrickId.ProcessList ->
            if (compact) Res.string.widget_processes_title_compact else Res.string.widget_processes_title
    },
)
