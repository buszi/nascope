package io.buszi.nascope.ui.dashboard.widgets.containers

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.widget_containers_paused_label
import io.buszi.nascope.resources.widget_containers_restarting_label
import io.buszi.nascope.resources.widget_containers_running_label
import io.buszi.nascope.resources.widget_containers_created_label
import io.buszi.nascope.resources.widget_containers_dead_label
import io.buszi.nascope.resources.widget_containers_exited_label
import io.buszi.nascope.resources.widget_containers_unknown_label
import io.buszi.nascope.ui.common.model.ContainerStateUi
import io.buszi.nascope.ui.common.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Accent color identifying the container state across the widget and the details screen. */
@Composable
internal fun ContainerStateUi.accentColor(): Color = when (this) {
    ContainerStateUi.Running -> AppTheme.colors.operational
    ContainerStateUi.Paused -> AppTheme.colors.degraded
    ContainerStateUi.Restarting -> AppTheme.colors.critical
    ContainerStateUi.Dead -> AppTheme.colors.critical
    ContainerStateUi.Exited -> AppTheme.colors.textMuted
    ContainerStateUi.Created -> AppTheme.colors.textMuted
    ContainerStateUi.Unknown -> AppTheme.colors.textDisabled
}

/** Uppercase label of the container state. */
@Composable
internal fun ContainerStateUi.label(): String = when (this) {
    ContainerStateUi.Running -> stringResource(Res.string.widget_containers_running_label)
    ContainerStateUi.Paused -> stringResource(Res.string.widget_containers_paused_label)
    ContainerStateUi.Restarting -> stringResource(Res.string.widget_containers_restarting_label)
    ContainerStateUi.Exited -> stringResource(Res.string.widget_containers_exited_label)
    ContainerStateUi.Created -> stringResource(Res.string.widget_containers_created_label)
    ContainerStateUi.Dead -> stringResource(Res.string.widget_containers_dead_label)
    ContainerStateUi.Unknown -> stringResource(Res.string.widget_containers_unknown_label)
}
