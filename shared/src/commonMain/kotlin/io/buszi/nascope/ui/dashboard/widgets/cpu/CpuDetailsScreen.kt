package io.buszi.nascope.ui.dashboard.widgets.cpu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.details_cpu_busiest_core_format
import io.buszi.nascope.resources.details_cpu_cores
import io.buszi.nascope.resources.details_cpu_history
import io.buszi.nascope.resources.details_cpu_history_average
import io.buszi.nascope.resources.details_cpu_history_peak
import io.buszi.nascope.resources.details_cpu_overview
import io.buszi.nascope.resources.details_cpu_sort_core
import io.buszi.nascope.resources.details_cpu_sort_usage
import io.buszi.nascope.resources.details_cpu_stat_busiest_core
import io.buszi.nascope.resources.details_cpu_stat_clock
import io.buszi.nascope.resources.details_cpu_stat_clock_max
import io.buszi.nascope.resources.details_cpu_stat_package_temp
import io.buszi.nascope.resources.details_cpu_title
import io.buszi.nascope.resources.percentage_format
import io.buszi.nascope.resources.widget_cpu_matrix_status_critical
import io.buszi.nascope.resources.widget_cpu_matrix_status_elevated
import io.buszi.nascope.resources.widget_cpu_matrix_status_nominal
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.color
import io.buszi.nascope.ui.common.components.gauges.LinearGauge
import io.buszi.nascope.ui.common.components.gauges.MiniSparkline
import io.buszi.nascope.ui.common.components.inputs.SelectableChip
import io.buszi.nascope.ui.common.components.metrics.MetricSize
import io.buszi.nascope.ui.common.components.metrics.MetricValue
import io.buszi.nascope.ui.common.model.CpuCoreUi
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuDetailsViewModel.SortOrder
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuDetailsViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuDetailsViewModel.ViewState
import io.buszi.nascope.ui.details.components.DetailsStatePlaceholder
import io.buszi.nascope.ui.details.components.DetailsScaffold
import io.buszi.nascope.ui.details.components.DetailsSection
import io.buszi.nascope.ui.details.components.DetailsStat
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CpuDetailsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<CpuDetailsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    CpuDetailsScreen(
        viewState = viewState,
        onClose = onClose,
        modifier = modifier,
        onViewAction = viewModel::onViewAction,
    )
}

@Composable
fun CpuDetailsScreen(
    viewState: ViewState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onViewAction: (ViewAction) -> Unit = {},
) {
    DetailsScaffold(
        title = stringResource(Res.string.details_cpu_title),
        onClose = onClose,
        testTag = TestTags.details(DashboardBrickId.CpuMatrix),
        modifier = modifier,
        subtitle = viewState.cpu.modelName.takeIf { viewState.widgetState.isReady && it.isNotBlank() },
        widgetState = viewState.widgetState,
        statusTag = if (viewState.widgetState.isReady) {
            {
                StatusChip(
                    text = when (viewState.cpu.overallStatus) {
                        StatusLevel.Critical -> stringResource(Res.string.widget_cpu_matrix_status_critical)
                        StatusLevel.Degraded -> stringResource(Res.string.widget_cpu_matrix_status_elevated)
                        StatusLevel.Operational -> stringResource(Res.string.widget_cpu_matrix_status_nominal)
                    },
                    status = viewState.cpu.overallStatus,
                )
            }
        } else {
            null
        },
    ) {
        if (!viewState.widgetState.isReady) {
            DetailsStatePlaceholder(viewState.widgetState)
        } else {
            OverviewSection(viewState = viewState)
            if (viewState.cpu.usageHistory.size >= 2) {
                HistorySection(viewState = viewState)
            }
            if (viewState.cpu.cores.isNotEmpty()) {
                CoresSection(viewState = viewState, onViewAction = onViewAction)
            }
        }
    }
}

@Composable
private fun OverviewSection(
    viewState: ViewState,
    modifier: Modifier = Modifier,
) {
    val cpu = viewState.cpu
    val busiest = cpu.busiestCore

    DetailsSection(
        title = stringResource(Res.string.details_cpu_overview),
        modifier = modifier,
    ) {
        MetricValue(
            value = formatPercentValue(cpu.usagePercent),
            unit = "%",
            trend = cpu.trend,
            size = MetricSize.Display,
            inlineTrend = true,
        )
        LinearGauge(
            progress = cpu.usagePercent / 100f,
            modifier = Modifier.padding(vertical = AppTheme.spacing.spaceSm),
            warningThreshold = CpuCoreUi.UsageWarningPercent / 100f,
            criticalThreshold = CpuCoreUi.UsageCriticalPercent / 100f,
        )
        // Only what the host reports: clock, max clock and package temperature are each left out when missing.
        if (cpu.frequencyGhz != null || cpu.maxFrequencyGhz != null || cpu.temperatureC != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                cpu.frequencyGhz?.let {
                    DetailsStat(
                        label = stringResource(Res.string.details_cpu_stat_clock),
                        value = formatFrequency(it),
                        modifier = Modifier.weight(1f),
                    )
                }
                cpu.maxFrequencyGhz?.let {
                    DetailsStat(
                        label = stringResource(Res.string.details_cpu_stat_clock_max),
                        value = formatFrequency(it),
                        modifier = Modifier.weight(1f),
                    )
                }
                cpu.temperatureC?.let {
                    DetailsStat(
                        label = stringResource(Res.string.details_cpu_stat_package_temp),
                        value = formatTemperature(it),
                        valueColor = cpu.temperatureStatus.valueColor(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (busiest != null) {
            DetailsStat(
                label = stringResource(Res.string.details_cpu_stat_busiest_core),
                value = stringResource(
                    Res.string.details_cpu_busiest_core_format,
                    formatCoreIndex(busiest.index),
                    busiest.usagePercent.toInt(),
                ),
                valueColor = busiest.usageStatus.valueColor(),
                modifier = Modifier.padding(top = AppTheme.spacing.spaceSm),
            )
        }
    }
}

@Composable
private fun HistorySection(
    viewState: ViewState,
    modifier: Modifier = Modifier,
) {
    DetailsSection(
        title = stringResource(Res.string.details_cpu_history),
        modifier = modifier,
        headerAction = {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
                Text(
                    text = stringResource(
                        Res.string.details_cpu_history_average,
                        formatPercentValue(viewState.cpu.usageHistoryAverage),
                    ),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                )
                Text(
                    text = stringResource(
                        Res.string.details_cpu_history_peak,
                        formatPercentValue(viewState.cpu.usageHistoryPeak),
                    ),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                )
            }
        },
    ) {
        MiniSparkline(
            data = viewState.cpu.usageHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.sparklineHeight * 2),
            lineColor = AppTheme.colors.telemetryA,
            minValue = 0f,
            maxValue = (viewState.cpu.usageHistoryPeak * 1.25f).coerceIn(20f, 100f),
        )
    }
}

@Composable
private fun CoresSection(
    viewState: ViewState,
    onViewAction: (ViewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailsSection(
        title = stringResource(Res.string.details_cpu_cores),
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs)) {
            SortOrder.entries.forEach { order ->
                SelectableChip(
                    text = order.label(),
                    selected = viewState.sortOrder == order,
                    onClick = { onViewAction(ViewAction.OnSelectSort(order)) },
                    modifier = Modifier.testTag(TestTags.cpuSort(order.name)),
                )
            }
        }
        Column(
            modifier = Modifier.padding(top = AppTheme.spacing.spaceSm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
        ) {
            viewState.sortedCores.forEach { core ->
                CoreRow(core = core)
            }
        }
    }
}

@Composable
private fun SortOrder.label(): String = when (this) {
    SortOrder.Core -> stringResource(Res.string.details_cpu_sort_core)
    SortOrder.Usage -> stringResource(Res.string.details_cpu_sort_usage)
}

@Composable
private fun CoreRow(
    core: CpuCoreUi,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.cpuCore(core.index))
            .semantics(mergeDescendants = true) {}
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.sm)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.sm,
            )
            .padding(
                horizontal = AppTheme.spacing.spaceSm,
                vertical = AppTheme.spacing.spaceXs,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        Text(
            text = formatCoreIndex(core.index),
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.textMuted,
            maxLines = 1,
        )
        LinearGauge(
            progress = core.usageRatio,
            modifier = Modifier.weight(1f),
            barHeight = AppTheme.spacing.space2xs,
            warningThreshold = CpuCoreUi.UsageWarningPercent / 100f,
            criticalThreshold = CpuCoreUi.UsageCriticalPercent / 100f,
            normalColor = AppTheme.colors.operational,
        )
        Text(
            text = stringResource(Res.string.percentage_format, core.usagePercent.toInt()),
            style = AppTheme.typography.labelCode,
            color = core.usageStatus.valueColor(),
            modifier = Modifier.width(34.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun StatusLevel.valueColor() = when (this) {
    StatusLevel.Operational -> AppTheme.colors.textPrimary
    StatusLevel.Degraded, StatusLevel.Critical -> color
}

@Preview
@Composable
private fun CpuDetailsScreenPreview(
    @PreviewParameter(CpuDetailsPreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        CpuDetailsScreen(
            viewState = viewState,
            onClose = {},
        )
    }
}
