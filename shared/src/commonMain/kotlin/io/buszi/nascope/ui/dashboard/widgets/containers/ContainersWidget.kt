package io.buszi.nascope.ui.dashboard.widgets.containers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.data.telemetry.ContainerInfo
import io.buszi.nascope.data.telemetry.ContainerState
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.widget_containers_collapse
import io.buszi.nascope.resources.widget_containers_cpu_label
import io.buszi.nascope.resources.widget_containers_empty
import io.buszi.nascope.resources.widget_containers_expand_all
import io.buszi.nascope.resources.widget_containers_mem_label
import io.buszi.nascope.resources.widget_containers_paused_label
import io.buszi.nascope.resources.widget_containers_running_label
import io.buszi.nascope.resources.widget_containers_status_critical
import io.buszi.nascope.resources.widget_containers_status_degraded
import io.buszi.nascope.resources.widget_containers_status_healthy
import io.buszi.nascope.resources.widget_containers_stopped_label
import io.buszi.nascope.resources.widget_containers_subtitle
import io.buszi.nascope.resources.widget_containers_subtitle_compact
import io.buszi.nascope.resources.widget_containers_title
import io.buszi.nascope.resources.widget_containers_title_compact
import io.buszi.nascope.resources.widget_overflow_more
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.buttons.ButtonSize
import io.buszi.nascope.ui.common.components.buttons.SecondaryButton
import io.buszi.nascope.ui.common.components.metrics.MetricSize
import io.buszi.nascope.ui.common.components.metrics.MetricValue
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.widgets.DashboardWidget
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersViewModel.ViewAction.ToggleExpand
import io.buszi.nascope.ui.dashboard.widgets.containers.ContainersViewModel.ViewState
import io.buszi.nascope.ui.dashboard.widgets.previewRowSplit
import io.buszi.nascope.ui.dashboard.widgets.rowsThatFit
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs

@Composable
fun ContainersWidget(
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<ContainersViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    ContainersWidget(
        viewState = viewState,
        modifier = modifier,
        onViewAction = viewModel::onViewAction,
    )
}

@Composable
fun ContainersWidget(
    viewState: ViewState,
    modifier: Modifier = Modifier,
    onViewAction: (ViewAction) -> Unit = {},
) {
    val statusLabel = when (viewState.overallStatus) {
        StatusLevel.Operational -> stringResource(Res.string.widget_containers_status_healthy)
        StatusLevel.Degraded -> stringResource(Res.string.widget_containers_status_degraded)
        StatusLevel.Critical -> stringResource(Res.string.widget_containers_status_critical)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 330.dp
        val isFixedHeight = constraints.hasBoundedHeight

        val subtitleText = if (isWide) {
            stringResource(
                Res.string.widget_containers_subtitle,
                viewState.runningCount,
                viewState.totalCount,
            )
        } else {
            stringResource(
                Res.string.widget_containers_subtitle_compact,
                viewState.runningCount,
                viewState.totalCount,
            )
        }

        DashboardWidget(
            title = if (isWide) {
                stringResource(Res.string.widget_containers_title)
            } else {
                stringResource(Res.string.widget_containers_title_compact)
            },
            subtitle = subtitleText,
            modifier = Modifier.fillMaxSize(),
            state = viewState.widgetState,
            statusTag = {
                StatusChip(
                    text = statusLabel,
                    status = viewState.overallStatus,
                    pulsing = viewState.overallStatus == StatusLevel.Critical,
                )
            },
            headerAction = {
                TagChip(text = viewState.engine.substringBefore(" ").uppercase())
            },
        ) {
            ContainersContent(
                viewState = viewState,
                isWide = isWide,
                isFixedHeight = isFixedHeight,
                onViewAction = onViewAction,
            )
        }
    }
}

@Composable
private fun ContainersContent(
    viewState: ViewState,
    isWide: Boolean,
    isFixedHeight: Boolean,
    onViewAction: (ViewAction) -> Unit,
) {
    if (isFixedHeight) {
        ContainersPreviewList(viewState = viewState)
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
    ) {
        // Section 1: Summary Counts (Running / Paused / Stopped)
        CountsSummarySection(viewState = viewState)

        // Section 2: Container List (preview top 3 when collapsed, all when expanded)
        if (viewState.containers.isEmpty()) {
            EmptyContainersPlaceholder()
        } else {
            ContainerListSection(
                viewState = viewState,
                isWide = isWide,
                onViewAction = onViewAction,
            )
        }
    }
}

@Composable
private fun ContainersPreviewList(
    viewState: ViewState,
) {
    if (viewState.containers.isEmpty()) {
        EmptyContainersPlaceholder()
        return
    }

    val rowSpacing = AppTheme.spacing.spaceXs

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val split = previewRowSplit(
            totalItems = viewState.containers.size,
            capacity = rowsThatFit(
                availableHeight = maxHeight,
                rowHeight = PREVIEW_ROW_HEIGHT,
                spacing = rowSpacing,
            ),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(rowSpacing),
        ) {
            viewState.containers.take(split.shown).forEach { container ->
                ContainerPreviewRow(container = container)
            }

            if (split.overflow > 0) {
                Text(
                    text = stringResource(Res.string.widget_overflow_more, split.overflow),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PREVIEW_ROW_HEIGHT)
                        .wrapContentHeight(Alignment.CenterVertically),
                )
            }
        }
    }
}

@Composable
private fun ContainerPreviewRow(
    container: ContainerInfo,
    modifier: Modifier = Modifier,
) {
    val statusColor = when (container.state) {
        ContainerState.Running -> AppTheme.colors.operational
        ContainerState.Paused -> AppTheme.colors.degraded
        ContainerState.Stopped -> AppTheme.colors.textMuted
        ContainerState.Restarting -> AppTheme.colors.critical
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PREVIEW_ROW_HEIGHT)
            .clip(AppTheme.shapes.base)
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.base,
            )
            .padding(horizontal = AppTheme.spacing.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(statusColor, AppTheme.shapes.full),
        )
        Text(
            text = container.name,
            style = AppTheme.typography.codeBase.copy(fontWeight = FontWeight.SemiBold),
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatCpuPercent(container.cpuPercent),
            style = AppTheme.typography.labelCode,
            color = if (container.cpuPercent > 0f) AppTheme.colors.telemetryA else AppTheme.colors.textSecondary,
            maxLines = 1,
        )
        Text(
            text = formatContainerMemory(container.memoryUsageBytes),
            style = AppTheme.typography.labelCode,
            color = if (container.memoryUsageBytes > 0L) AppTheme.colors.telemetryB else AppTheme.colors.textSecondary,
            maxLines = 1,
        )
    }
}

private val PREVIEW_ROW_HEIGHT = 26.dp

@Composable
private fun CountsSummarySection(
    viewState: ViewState,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        CountPillCard(
            label = stringResource(Res.string.widget_containers_running_label),
            count = viewState.runningCount,
            accentColor = AppTheme.colors.operational,
            modifier = Modifier.weight(1f),
        )

        CountPillCard(
            label = stringResource(Res.string.widget_containers_paused_label),
            count = viewState.pausedCount,
            accentColor = if (viewState.pausedCount > 0) AppTheme.colors.degraded else AppTheme.colors.textMuted,
            modifier = Modifier.weight(1f),
        )

        CountPillCard(
            label = stringResource(Res.string.widget_containers_stopped_label),
            count = viewState.stoppedCount,
            accentColor = if (viewState.stoppedCount > 0) AppTheme.colors.critical else AppTheme.colors.textMuted,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CountPillCard(
    label: String,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(AppTheme.shapes.base)
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.base,
            )
            .padding(
                horizontal = AppTheme.spacing.spaceSm,
                vertical = AppTheme.spacing.spaceXs,
            ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(accentColor, AppTheme.shapes.full),
                )
                Text(
                    text = label,
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            MetricValue(
                value = count.toString(),
                size = MetricSize.Medium,
                valueColor = accentColor,
            )
        }
    }
}

@Composable
private fun ContainerListSection(
    viewState: ViewState,
    isWide: Boolean,
    onViewAction: (ViewAction) -> Unit,
) {
    val displayContainers = if (viewState.isExpanded) {
        viewState.containers
    } else {
        viewState.containers.take(3)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        displayContainers.forEach { container ->
            ContainerRowCard(
                container = container,
                isWide = isWide,
            )
        }

        if (viewState.totalCount > 3) {
            val expandButtonText = if (viewState.isExpanded) {
                stringResource(Res.string.widget_containers_collapse)
            } else {
                stringResource(Res.string.widget_containers_expand_all, viewState.totalCount)
            }

            SecondaryButton(
                text = expandButtonText,
                onClick = { onViewAction(ToggleExpand) },
                size = ButtonSize.Dense,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ContainerRowCard(
    container: ContainerInfo,
    isWide: Boolean,
    modifier: Modifier = Modifier,
) {
    val (statusColor, statusLabel) = when (container.state) {
        ContainerState.Running -> Pair(AppTheme.colors.operational, "RUNNING")
        ContainerState.Paused -> Pair(AppTheme.colors.degraded, "PAUSED")
        ContainerState.Stopped -> Pair(AppTheme.colors.textMuted, "STOPPED")
        ContainerState.Restarting -> Pair(AppTheme.colors.critical, "RESTARTING")
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.base)
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.base,
            )
            .padding(AppTheme.spacing.spaceSm),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
        ) {
            // Line 1: Container Name & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(statusColor, AppTheme.shapes.full),
                    )

                    Text(
                        text = container.name,
                        style = AppTheme.typography.codeBase.copy(fontWeight = FontWeight.SemiBold),
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = "#${container.id}",
                        style = AppTheme.typography.labelCode,
                        color = AppTheme.colors.textMuted,
                        maxLines = 1,
                    )
                }

                TagChip(text = statusLabel)
            }

            // Line 2: Image & Uptime
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = container.image,
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                Spacer(modifier = Modifier.width(AppTheme.spacing.spaceSm))

                Text(
                    text = container.uptime,
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                    maxLines = 1,
                )
            }

            // Line 3: CPU & Memory Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                ) {
                    Text(
                        text = stringResource(Res.string.widget_containers_cpu_label),
                        style = AppTheme.typography.labelCode,
                        color = AppTheme.colors.textMuted,
                    )
                    Text(
                        text = formatCpuPercent(container.cpuPercent),
                        style = AppTheme.typography.labelCode,
                        color = if (container.cpuPercent > 0f) AppTheme.colors.telemetryA else AppTheme.colors.textSecondary,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                ) {
                    Text(
                        text = stringResource(Res.string.widget_containers_mem_label),
                        style = AppTheme.typography.labelCode,
                        color = AppTheme.colors.textMuted,
                    )
                    Text(
                        text = formatContainerMemory(container.memoryUsageBytes),
                        style = AppTheme.typography.labelCode,
                        color = if (container.memoryUsageBytes > 0L) AppTheme.colors.telemetryB else AppTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyContainersPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.base)
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.base,
            )
            .padding(AppTheme.spacing.spaceLg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.widget_containers_empty),
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.textMuted,
        )
    }
}

internal fun formatCpuPercent(percent: Float): String {
    val rounded = ((percent * 10).toLong() / 10.0).toString()
    return "$rounded%"
}

internal fun formatContainerMemory(bytes: Long): String {
    val absBytes = abs(bytes)
    return when {
        absBytes >= 1_000_000_000L -> {
            val gb = bytes / 1_000_000_000.0
            val formatted = ((gb * 10).toLong() / 10.0).toString()
            "$formatted GB"
        }
        absBytes >= 1_000_000L -> {
            val mb = bytes / 1_000_000.0
            val formatted = ((mb * 10).toLong() / 10.0).toString()
            "$formatted MB"
        }
        absBytes >= 1_000L -> {
            val kb = bytes / 1_000.0
            val formatted = ((kb * 10).toLong() / 10.0).toString()
            "$formatted KB"
        }
        else -> "$bytes B"
    }
}

@Preview
@Composable
private fun ContainersWidgetPreview(
    @PreviewParameter(ContainersPreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        Box(
            modifier = Modifier
                .background(AppTheme.colors.canvasBackground)
                .padding(AppTheme.spacing.spaceLg),
        ) {
            ContainersWidget(viewState = viewState)
        }
    }
}
