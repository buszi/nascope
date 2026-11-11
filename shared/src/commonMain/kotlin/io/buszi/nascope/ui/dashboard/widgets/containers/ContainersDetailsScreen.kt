package io.buszi.nascope.ui.dashboard.widgets.containers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.details_containers_field_image
import io.buszi.nascope.resources.details_containers_field_memory_limit
import io.buszi.nascope.resources.details_containers_field_uptime
import io.buszi.nascope.resources.details_containers_filter_all
import io.buszi.nascope.resources.details_containers_filter_format
import io.buszi.nascope.resources.details_containers_list
import io.buszi.nascope.resources.details_containers_logs
import io.buszi.nascope.resources.details_containers_no_logs
import io.buszi.nascope.resources.details_containers_no_match
import io.buszi.nascope.resources.details_containers_overview
import io.buszi.nascope.resources.details_containers_stat_cpu_total
import io.buszi.nascope.resources.details_containers_stat_mem_total
import io.buszi.nascope.resources.details_containers_subtitle
import io.buszi.nascope.resources.details_containers_title
import io.buszi.nascope.resources.details_containers_unlimited
import io.buszi.nascope.resources.widget_containers_cpu_label
import io.buszi.nascope.resources.widget_containers_empty
import io.buszi.nascope.resources.widget_containers_mem_label
import io.buszi.nascope.resources.widget_containers_status_critical
import io.buszi.nascope.resources.widget_containers_status_degraded
import io.buszi.nascope.resources.widget_containers_status_healthy
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.gauges.LinearGauge
import io.buszi.nascope.ui.common.components.inputs.SelectableChip
import io.buszi.nascope.ui.common.model.ContainerStateUi
import io.buszi.nascope.ui.common.model.ContainerUi
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersDetailsViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersDetailsViewModel.ViewState
import io.buszi.nascope.ui.details.components.DetailsKeyValueRow
import io.buszi.nascope.ui.details.components.DetailsLoadingPlaceholder
import io.buszi.nascope.ui.details.components.DetailsScaffold
import io.buszi.nascope.ui.details.components.DetailsSection
import io.buszi.nascope.ui.details.components.DetailsStat
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContainersDetailsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<ContainersDetailsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    ContainersDetailsScreen(
        viewState = viewState,
        onClose = onClose,
        modifier = modifier,
        onViewAction = viewModel::onViewAction,
    )
}

@Composable
fun ContainersDetailsScreen(
    viewState: ViewState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onViewAction: (ViewAction) -> Unit = {},
) {
    DetailsScaffold(
        title = stringResource(Res.string.details_containers_title),
        onClose = onClose,
        testTag = TestTags.details(DashboardBrickId.Containers),
        modifier = modifier,
        subtitle = if (viewState.widgetState.isReady) {
            stringResource(
                Res.string.details_containers_subtitle,
                viewState.engine,
                viewState.containers.size,
            )
        } else {
            null
        },
        statusTag = if (viewState.widgetState.isReady) {
            {
                StatusChip(
                    text = when (viewState.overallStatus) {
                        StatusLevel.Operational -> stringResource(Res.string.widget_containers_status_healthy)
                        StatusLevel.Degraded -> stringResource(Res.string.widget_containers_status_degraded)
                        StatusLevel.Critical -> stringResource(Res.string.widget_containers_status_critical)
                    },
                    status = viewState.overallStatus,
                    pulsing = viewState.overallStatus == StatusLevel.Critical,
                )
            }
        } else {
            null
        },
    ) {
        if (!viewState.widgetState.isReady) {
            DetailsLoadingPlaceholder()
        } else if (viewState.containers.isEmpty()) {
            Text(
                text = stringResource(Res.string.widget_containers_empty),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
            )
        } else {
            OverviewSection(viewState = viewState)
            ContainersSection(viewState = viewState, onViewAction = onViewAction)
        }
    }
}

@Composable
private fun OverviewSection(
    viewState: ViewState,
    modifier: Modifier = Modifier,
) {
    DetailsSection(
        title = stringResource(Res.string.details_containers_overview),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                DetailsStat(
                    label = ContainerStateUi.Running.label(),
                    value = viewState.countOf(ContainerStateUi.Running).toString(),
                    valueColor = ContainerStateUi.Running.accentColor(),
                    modifier = Modifier.weight(1f),
                )
                DetailsStat(
                    label = ContainerStateUi.Paused.label(),
                    value = viewState.countOf(ContainerStateUi.Paused).toString(),
                    valueColor = ContainerStateUi.Paused.accentColor(),
                    modifier = Modifier.weight(1f),
                )
                DetailsStat(
                    label = ContainerStateUi.Stopped.label(),
                    value = viewState.countOf(ContainerStateUi.Stopped).toString(),
                    valueColor = ContainerStateUi.Stopped.accentColor(),
                    modifier = Modifier.weight(1f),
                )
                DetailsStat(
                    label = ContainerStateUi.Restarting.label(),
                    value = viewState.countOf(ContainerStateUi.Restarting).toString(),
                    valueColor = ContainerStateUi.Restarting.accentColor(),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                DetailsStat(
                    label = stringResource(Res.string.details_containers_stat_cpu_total),
                    value = formatCpuPercent(viewState.totalCpuPercent),
                    valueColor = AppTheme.colors.telemetryA,
                    modifier = Modifier.weight(1f),
                )
                DetailsStat(
                    label = stringResource(Res.string.details_containers_stat_mem_total),
                    value = formatContainerMemory(viewState.totalMemoryBytes),
                    valueColor = AppTheme.colors.telemetryB,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContainersSection(
    viewState: ViewState,
    onViewAction: (ViewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailsSection(
        title = stringResource(Res.string.details_containers_list),
        modifier = modifier,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
        ) {
            SelectableChip(
                text = stringResource(
                    Res.string.details_containers_filter_format,
                    stringResource(Res.string.details_containers_filter_all),
                    viewState.containers.size,
                ),
                selected = viewState.stateFilter == null,
                onClick = { onViewAction(ViewAction.OnSelectFilter(null)) },
                modifier = Modifier.testTag(TestTags.containersFilter(null)),
            )
            ContainerStateUi.entries.forEach { state ->
                SelectableChip(
                    text = stringResource(
                        Res.string.details_containers_filter_format,
                        state.label(),
                        viewState.countOf(state),
                    ),
                    selected = viewState.stateFilter == state,
                    onClick = { onViewAction(ViewAction.OnSelectFilter(state)) },
                    modifier = Modifier.testTag(TestTags.containersFilter(state.name)),
                )
            }
        }

        Column(
            modifier = Modifier.padding(top = AppTheme.spacing.spaceSm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            if (viewState.filteredContainers.isEmpty()) {
                Text(
                    text = stringResource(Res.string.details_containers_no_match),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                )
            }
            viewState.filteredContainers.forEach { container ->
                ContainerDetailsCard(
                    container = container,
                    expanded = viewState.expandedContainerId == container.id,
                    onToggle = { onViewAction(ViewAction.OnToggleContainer(container.id)) },
                )
            }
        }
    }
}

@Composable
private fun ContainerDetailsCard(
    container: ContainerUi,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = AppTheme.shapes.base
    val accent = container.state.accentColor()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppTheme.colors.inputBackground, shape)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = if (expanded) AppTheme.colors.focusHighlight else AppTheme.colors.baseBorder,
                shape = shape,
            )
            .testTag(TestTags.container(container.id))
            .clickable(onClick = onToggle)
            .padding(AppTheme.spacing.spaceSm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
            ) {
                Box(
                    modifier = Modifier
                        .size(AppTheme.dimensions.statusDotSize)
                        .background(accent, AppTheme.shapes.full),
                )
                Text(
                    text = container.name,
                    style = AppTheme.typography.codeBase.copy(fontWeight = FontWeight.SemiBold),
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = "#${container.id}",
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                    maxLines = 1,
                )
            }
            TagChip(text = container.state.label())
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            ResourceGauge(
                label = stringResource(Res.string.widget_containers_cpu_label),
                valueText = formatCpuPercent(container.cpuPercent),
                progress = container.cpuPercent / 100f,
                color = AppTheme.colors.telemetryA,
                modifier = Modifier.weight(1f),
            )
            ResourceGauge(
                label = stringResource(Res.string.widget_containers_mem_label),
                valueText = formatContainerMemory(container.memoryUsageBytes),
                progress = container.memoryRatio ?: 0f,
                color = AppTheme.colors.telemetryB,
                modifier = Modifier.weight(1f),
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier.padding(top = AppTheme.spacing.spaceXs),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
            ) {
                DetailsKeyValueRow(
                    label = stringResource(Res.string.details_containers_field_image),
                    value = container.image,
                )
                DetailsKeyValueRow(
                    label = stringResource(Res.string.details_containers_field_uptime),
                    value = container.uptime,
                )
                DetailsKeyValueRow(
                    label = stringResource(Res.string.details_containers_field_memory_limit),
                    value = container.memoryLimitBytes?.let(::formatContainerMemory)
                        ?: stringResource(Res.string.details_containers_unlimited),
                )
                LogPreview(logs = container.recentLogs)
            }
        }
    }
}

@Composable
private fun ResourceGauge(
    label: String,
    valueText: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
                maxLines = 1,
            )
            Text(
                text = valueText,
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textSecondary,
                maxLines = 1,
            )
        }
        LinearGauge(
            progress = progress,
            barHeight = AppTheme.spacing.space2xs,
            normalColor = color,
            warningColor = color,
            criticalColor = color,
        )
    }
}

@Composable
private fun LogPreview(
    logs: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
    ) {
        Text(
            text = stringResource(Res.string.details_containers_logs),
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.textMuted,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.canvasBackground, AppTheme.shapes.sm)
                .border(
                    width = AppTheme.dimensions.hairlineBorder,
                    color = AppTheme.colors.subtleDivider,
                    shape = AppTheme.shapes.sm,
                )
                .padding(AppTheme.spacing.spaceSm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
        ) {
            if (logs.isEmpty()) {
                Text(
                    text = stringResource(Res.string.details_containers_no_logs),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                )
            }
            logs.forEach { line ->
                Text(
                    text = line,
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ContainersDetailsScreenPreview(
    @PreviewParameter(ContainersDetailsPreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        ContainersDetailsScreen(
            viewState = viewState,
            onClose = {},
        )
    }
}
