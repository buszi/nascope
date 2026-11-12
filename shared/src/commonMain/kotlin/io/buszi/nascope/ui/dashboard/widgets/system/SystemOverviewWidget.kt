package io.buszi.nascope.ui.dashboard.widgets.system

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.percentage_format
import io.buszi.nascope.resources.widget_system_overview_cores
import io.buszi.nascope.resources.widget_system_overview_cores_compact
import io.buszi.nascope.resources.widget_system_overview_load_15m
import io.buszi.nascope.resources.widget_system_overview_load_15m_compact
import io.buszi.nascope.resources.widget_system_overview_load_1m
import io.buszi.nascope.resources.widget_system_overview_load_1m_compact
import io.buszi.nascope.resources.widget_system_overview_load_5m
import io.buszi.nascope.resources.widget_system_overview_load_5m_compact
import io.buszi.nascope.resources.widget_system_overview_load_averages
import io.buszi.nascope.resources.widget_system_overview_load_normalized
import io.buszi.nascope.resources.widget_system_overview_status_critical
import io.buszi.nascope.resources.widget_system_overview_status_elevated
import io.buszi.nascope.resources.widget_system_overview_status_healthy
import io.buszi.nascope.resources.widget_system_overview_title
import io.buszi.nascope.resources.widget_system_overview_uptime
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusDot
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.gauges.LinearGauge
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.widgets.DashboardWidget
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewViewModel.ViewState
import kotlin.math.abs
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SystemOverviewWidget(
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<SystemOverviewViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    SystemOverviewWidget(
        viewState = viewState,
        modifier = modifier,
        onViewAction = viewModel::onViewAction,
    )
}

@Composable
fun SystemOverviewWidget(
    viewState: ViewState,
    modifier: Modifier = Modifier,
    onViewAction: (ViewAction) -> Unit = {},
) {
    val overview = viewState.overview
    val normalized1m = overview.load?.let { if (it.cpuCores > 0) it.load1m / it.cpuCores else 0f } ?: 0f
    val overallStatus = when {
        normalized1m >= 1.0f -> StatusLevel.Critical
        normalized1m >= 0.75f -> StatusLevel.Degraded
        else -> StatusLevel.Operational
    }
    val statusLabel = when (overallStatus) {
        StatusLevel.Critical -> stringResource(Res.string.widget_system_overview_status_critical)
        StatusLevel.Degraded -> stringResource(Res.string.widget_system_overview_status_elevated)
        StatusLevel.Operational -> stringResource(Res.string.widget_system_overview_status_healthy)
    }

    DashboardWidget(
        title = stringResource(Res.string.widget_system_overview_title),
        subtitle = if (viewState.widgetState.isReady) overview.osName else null,
        modifier = modifier,
        state = viewState.widgetState,
        statusTag = if (viewState.widgetState.isReady) {
            {
                StatusChip(
                    text = statusLabel,
                    status = overallStatus,
                )
            }
        } else null,
        headerAction = if (viewState.widgetState.isReady) {
            {
                overview.uptime?.let { uptime ->
                    TagChip(text = stringResource(Res.string.widget_system_overview_uptime, uptime))
                }
            }
        } else null,
    ) {
        if (viewState.widgetState.isReady) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 330.dp

                if (isWide) {
                    WideOverviewContent(viewState = viewState)
                } else {
                    CompactOverviewContent(viewState = viewState)
                }
            }
        }
    }
}

@Composable
private fun WideOverviewContent(
    viewState: ViewState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
    ) {
        // Host Identity Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(AppTheme.dimensions.buttonHeightDense)
                    .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
                    .border(
                        width = AppTheme.dimensions.hairlineBorder,
                        color = AppTheme.colors.baseBorder,
                        shape = AppTheme.shapes.base,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                HostPlatformIcon(
                    platform = viewState.overview.platform,
                    tint = AppTheme.colors.telemetryA,
                    iconSize = AppTheme.spacing.spaceLg,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = viewState.overview.hostname,
                    style = AppTheme.typography.headlineSm,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = viewState.overview.kernelRelease,
                    style = AppTheme.typography.codeBase,
                    color = AppTheme.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            viewState.overview.load?.let { load ->
                TagChip(text = stringResource(Res.string.widget_system_overview_cores, load.cpuCores))
            }
        }

        // Load Averages Section: left out when the host reports no load
        viewState.overview.load?.let { load ->
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppTheme.dimensions.hairlineBorder)
                    .background(AppTheme.colors.subtleDivider),
            )
            LoadAveragesWide(
                load1m = load.load1m,
                load5m = load.load5m,
                load15m = load.load15m,
                cpuCores = load.cpuCores,
            )
        }
    }
}

@Composable
private fun CompactOverviewContent(
    viewState: ViewState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        // Compact Host Identity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(AppTheme.dimensions.buttonHeightDense)
                    .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
                    .border(
                        width = AppTheme.dimensions.hairlineBorder,
                        color = AppTheme.colors.baseBorder,
                        shape = AppTheme.shapes.base,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                HostPlatformIcon(
                    platform = viewState.overview.platform,
                    tint = AppTheme.colors.telemetryA,
                    iconSize = AppTheme.spacing.spaceMd,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = viewState.overview.hostname,
                    style = AppTheme.typography.bodyMd,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = viewState.overview.kernelRelease,
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            viewState.overview.load?.let { load ->
                TagChip(text = stringResource(Res.string.widget_system_overview_cores_compact, load.cpuCores))
            }
        }

        viewState.overview.load?.let { load ->
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppTheme.dimensions.hairlineBorder)
                    .background(AppTheme.colors.subtleDivider),
            )

            // Compact Load Averages Rows
            Text(
                text = stringResource(Res.string.widget_system_overview_load_averages),
                style = AppTheme.typography.labelUi,
                color = AppTheme.colors.textMuted,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
                    .border(
                        width = AppTheme.dimensions.hairlineBorder,
                        color = AppTheme.colors.baseBorder,
                        shape = AppTheme.shapes.base,
                    )
                    .padding(AppTheme.spacing.spaceSm),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
            ) {
                CompactLoadRow(
                    label = stringResource(Res.string.widget_system_overview_load_1m_compact),
                    value = load.load1m,
                    cpuCores = load.cpuCores,
                )
                CompactLoadRow(
                    label = stringResource(Res.string.widget_system_overview_load_5m_compact),
                    value = load.load5m,
                    cpuCores = load.cpuCores,
                )
                CompactLoadRow(
                    label = stringResource(Res.string.widget_system_overview_load_15m_compact),
                    value = load.load15m,
                    cpuCores = load.cpuCores,
                )
            }
        }
    }
}

@Composable
private fun LoadAveragesWide(
    load1m: Float,
    load5m: Float,
    load15m: Float,
    cpuCores: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.widget_system_overview_load_averages),
                style = AppTheme.typography.labelUi,
                color = AppTheme.colors.textMuted,
            )
            Text(
                text = stringResource(Res.string.widget_system_overview_load_normalized, cpuCores),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textSecondary,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            LoadCell(
                label = stringResource(Res.string.widget_system_overview_load_1m),
                value = load1m,
                cpuCores = cpuCores,
                modifier = Modifier.weight(1f),
            )
            LoadCell(
                label = stringResource(Res.string.widget_system_overview_load_5m),
                value = load5m,
                cpuCores = cpuCores,
                modifier = Modifier.weight(1f),
            )
            LoadCell(
                label = stringResource(Res.string.widget_system_overview_load_15m),
                value = load15m,
                cpuCores = cpuCores,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LoadCell(
    label: String,
    value: Float,
    cpuCores: Int,
    modifier: Modifier = Modifier,
) {
    val ratio = if (cpuCores > 0) (value / cpuCores).coerceIn(0f, 1f) else 0f
    val percentage = (ratio * 100).toInt()
    val status = when {
        ratio >= 0.90f -> StatusLevel.Critical
        ratio >= 0.75f -> StatusLevel.Degraded
        else -> StatusLevel.Operational
    }

    Column(
        modifier = modifier
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.base,
            )
            .padding(AppTheme.spacing.spaceSm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
            )
            StatusDot(
                status = status,
                pulsing = status == StatusLevel.Critical,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = formatLoad(value),
                style = AppTheme.typography.metricMd,
                color = AppTheme.colors.textPrimary,
            )
            Text(
                text = stringResource(Res.string.percentage_format, percentage),
                style = AppTheme.typography.labelCode,
                color = when (status) {
                    StatusLevel.Critical -> AppTheme.colors.critical
                    StatusLevel.Degraded -> AppTheme.colors.degraded
                    StatusLevel.Operational -> AppTheme.colors.textMuted
                },
            )
        }

        LinearGauge(
            progress = ratio,
            barHeight = AppTheme.spacing.space2xs,
            normalColor = AppTheme.colors.operational,
            warningColor = AppTheme.colors.degraded,
            criticalColor = AppTheme.colors.critical,
        )
    }
}

@Composable
private fun CompactLoadRow(
    label: String,
    value: Float,
    cpuCores: Int,
    modifier: Modifier = Modifier,
) {
    val ratio = if (cpuCores > 0) (value / cpuCores).coerceIn(0f, 1f) else 0f
    val percentage = (ratio * 100).toInt()
    val status = when {
        ratio >= 0.90f -> StatusLevel.Critical
        ratio >= 0.75f -> StatusLevel.Degraded
        else -> StatusLevel.Operational
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.textMuted,
            modifier = Modifier.width(32.dp),
        )
        Text(
            text = formatLoad(value),
            style = AppTheme.typography.codeBase,
            color = AppTheme.colors.textPrimary,
            modifier = Modifier.width(42.dp),
        )
        LinearGauge(
            progress = ratio,
            barHeight = AppTheme.spacing.space2xs,
            modifier = Modifier.weight(1f),
            normalColor = AppTheme.colors.operational,
            warningColor = AppTheme.colors.degraded,
            criticalColor = AppTheme.colors.critical,
        )
        Text(
            text = stringResource(Res.string.percentage_format, percentage),
            style = AppTheme.typography.labelCode,
            color = when (status) {
                StatusLevel.Critical -> AppTheme.colors.critical
                StatusLevel.Degraded -> AppTheme.colors.degraded
                StatusLevel.Operational -> AppTheme.colors.textSecondary
            },
            modifier = Modifier.width(36.dp),
        )
    }
}

private fun formatLoad(value: Float): String {
    val rounded = (value * 100).toInt()
    val whole = rounded / 100
    val fraction = abs(rounded % 100)
    val fracStr = if (fraction < 10) "0$fraction" else "$fraction"
    return "$whole.$fracStr"
}

@Preview
@Composable
private fun SystemOverviewWidgetPreview(
    @PreviewParameter(SystemOverviewPreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        SystemOverviewWidget(
            viewState = viewState,
            onViewAction = {},
        )
    }
}
