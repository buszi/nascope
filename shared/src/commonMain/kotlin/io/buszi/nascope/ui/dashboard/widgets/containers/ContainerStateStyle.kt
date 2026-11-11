package io.buszi.nascope.ui.dashboard.widgets.containers

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.widget_containers_paused_label
import io.buszi.nascope.resources.widget_containers_restarting_label
import io.buszi.nascope.resources.widget_containers_running_label
import io.buszi.nascope.resources.widget_containers_stopped_label
import io.buszi.nascope.ui.common.model.ContainerStateUi
import io.buszi.nascope.ui.common.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Accent color identifying the container state across the widget and the details screen. */
@Composable
internal fun ContainerStateUi.accentColor(): Color = when (this) {
    ContainerStateUi.Running -> AppTheme.colors.operational
    ContainerStateUi.Paused -> AppTheme.colors.degraded
    ContainerStateUi.Stopped -> AppTheme.colors.textMuted
    ContainerStateUi.Restarting -> AppTheme.colors.critical
}

/** Uppercase label of the container state. */
@Composable
internal fun ContainerStateUi.label(): String = when (this) {
    ContainerStateUi.Running -> stringResource(Res.string.widget_containers_running_label)
    ContainerStateUi.Paused -> stringResource(Res.string.widget_containers_paused_label)
    ContainerStateUi.Stopped -> stringResource(Res.string.widget_containers_stopped_label)
    ContainerStateUi.Restarting -> stringResource(Res.string.widget_containers_restarting_label)
}
