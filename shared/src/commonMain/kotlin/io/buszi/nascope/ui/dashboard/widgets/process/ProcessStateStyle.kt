package io.buszi.nascope.ui.dashboard.widgets.process

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.details_processes_state_disk_sleep
import io.buszi.nascope.resources.details_processes_state_idle
import io.buszi.nascope.resources.details_processes_state_running
import io.buszi.nascope.resources.details_processes_state_sleeping
import io.buszi.nascope.resources.details_processes_state_stopped
import io.buszi.nascope.resources.details_processes_state_unknown
import io.buszi.nascope.resources.details_processes_state_zombie
import io.buszi.nascope.ui.common.model.ProcessStateUi
import io.buszi.nascope.ui.common.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Accent color identifying the process state. */
@Composable
internal fun ProcessStateUi.accentColor(): Color = when (this) {
    ProcessStateUi.Running -> AppTheme.colors.operational
    ProcessStateUi.Sleeping -> AppTheme.colors.textMuted
    ProcessStateUi.Idle -> AppTheme.colors.textDisabled
    ProcessStateUi.DiskSleep -> AppTheme.colors.degraded
    ProcessStateUi.Stopped -> AppTheme.colors.telemetryB
    ProcessStateUi.Zombie -> AppTheme.colors.critical
    ProcessStateUi.Unknown -> AppTheme.colors.textDisabled
}

/** Uppercase label of the process state. */
@Composable
internal fun ProcessStateUi.label(): String = when (this) {
    ProcessStateUi.Running -> stringResource(Res.string.details_processes_state_running)
    ProcessStateUi.Sleeping -> stringResource(Res.string.details_processes_state_sleeping)
    ProcessStateUi.Idle -> stringResource(Res.string.details_processes_state_idle)
    ProcessStateUi.DiskSleep -> stringResource(Res.string.details_processes_state_disk_sleep)
    ProcessStateUi.Stopped -> stringResource(Res.string.details_processes_state_stopped)
    ProcessStateUi.Zombie -> stringResource(Res.string.details_processes_state_zombie)
    ProcessStateUi.Unknown -> stringResource(Res.string.details_processes_state_unknown)
}
