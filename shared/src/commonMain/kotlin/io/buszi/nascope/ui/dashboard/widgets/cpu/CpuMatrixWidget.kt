package io.buszi.nascope.ui.dashboard.widgets.cpu

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.percentage_format
import io.buszi.nascope.resources.widget_cpu_matrix_core_matrix
import io.buszi.nascope.resources.widget_cpu_matrix_core_matrix_compact
import io.buszi.nascope.resources.widget_cpu_matrix_cores_active
import io.buszi.nascope.resources.widget_cpu_matrix_cores_compact
import io.buszi.nascope.resources.widget_cpu_matrix_status_critical
import io.buszi.nascope.resources.widget_cpu_matrix_status_elevated
import io.buszi.nascope.resources.widget_cpu_matrix_status_nominal
import io.buszi.nascope.resources.widget_cpu_matrix_title
import io.buszi.nascope.resources.widget_cpu_matrix_title_compact
import io.buszi.nascope.resources.widget_overflow_more
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusDot
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.gauges.LinearGauge
import io.buszi.nascope.ui.common.components.gauges.MiniSparkline
import io.buszi.nascope.ui.common.components.metrics.MetricSize
import io.buszi.nascope.ui.common.components.metrics.MetricValue
import io.buszi.nascope.ui.common.components.metrics.TrendBadge
import io.buszi.nascope.ui.common.model.CpuCoreUi
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.widgets.DashboardWidget
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuMatrixViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuMatrixViewModel.ViewState
import kotlin.math.abs
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CpuMatrixWidget(
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<CpuMatrixViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    CpuMatrixWidget(
        viewState = viewState,
        modifier = modifier,
        onViewAction = viewModel::onViewAction,
    )
}

@Composable
fun CpuMatrixWidget(
    viewState: ViewState,
    modifier: Modifier = Modifier,
    onViewAction: (ViewAction) -> Unit = {},
) {
    val overallStatus = viewState.cpu.overallStatus
    val statusLabel = when (overallStatus) {
        StatusLevel.Critical -> stringResource(Res.string.widget_cpu_matrix_status_critical)
        StatusLevel.Degraded -> stringResource(Res.string.widget_cpu_matrix_status_elevated)
        StatusLevel.Operational -> stringResource(Res.string.widget_cpu_matrix_status_nominal)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val containerWidth = maxWidth
        val isWide = containerWidth >= 330.dp
        val isFixedHeight = constraints.hasBoundedHeight

        DashboardWidget(
            title = if (isWide) {
                stringResource(Res.string.widget_cpu_matrix_title)
            } else {
                stringResource(Res.string.widget_cpu_matrix_title_compact)
            },
            subtitle = if (viewState.widgetState.isReady && viewState.cpu.modelName.isNotBlank()) viewState.cpu.modelName else null,
            modifier = Modifier.fillMaxSize(),
            state = viewState.widgetState,
            statusTag = if (viewState.widgetState.isReady) {
                {
                    if (isWide) {
                        StatusChip(
                            text = statusLabel,
                            status = overallStatus,
                        )
                    } else {
                        StatusDot(
                            status = overallStatus,
                            pulsing = overallStatus == StatusLevel.Critical,
                        )
                    }
                }
            } else null,
            headerAction = if (viewState.widgetState.isReady) {
                {
                    viewState.cpu.frequencyGhz?.takeIf { isWide }?.let { frequency ->
                        TagChip(text = formatFrequency(frequency))
                    }
                    viewState.cpu.temperatureC?.let { temperature ->
                        val tempStatus = CpuCoreUi.temperatureStatus(temperature)
                        StatusChip(
                            text = formatTemperature(temperature),
                            status = tempStatus,
                            showDot = isWide,
                            pulsing = tempStatus == StatusLevel.Critical,
                        )
                    }
                }
            } else null,
        ) {
            if (viewState.widgetState.isReady) {
                if (isFixedHeight) {
                    FixedHeightCpuContent(viewState = viewState)
                } else if (isWide) {
                    WideCpuMatrixContent(viewState = viewState, maxWidth = containerWidth)
                } else {
                    CompactCpuMatrixContent(viewState = viewState, maxWidth = containerWidth)
                }
            }
        }
    }
}

/**
 * Glanceable 1U layout: single-line aggregate usage followed by a 4-column core grid.
 * Shows at most [FIXED_HEIGHT_MAX_CORES] cells; extra cores are summarised as "+N MORE".
 */
@Composable
private fun FixedHeightCpuContent(
    viewState: ViewState,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // A host with few cores in a tall slot has room to spare: use it for the history and bigger core tiles.
        if (viewState.cpu.cores.size in 1..ROOMY_MAX_CORES && maxHeight >= ROOMY_MIN_HEIGHT) {
            RoomyCpuContent(viewState = viewState)
        } else {
            DenseCpuContent(viewState = viewState)
        }
    }
}

private val ROOMY_MIN_HEIGHT = 120.dp
private const val ROOMY_MAX_CORES = 8
private const val ROOMY_CORE_COLUMNS = 4
private const val ROOMY_MIN_SCALE = 25f

/**
 * Few cores, tall slot: the usage with its recent history filling the middle, then one tile per core.
 */
@Composable
private fun RoomyCpuContent(
    viewState: ViewState,
    modifier: Modifier = Modifier,
) {
    val cpu = viewState.cpu
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetricValue(
                value = formatPercentValue(cpu.usagePercent),
                unit = "%",
                size = MetricSize.Display,
            )
            LinearGauge(
                progress = (cpu.usagePercent / 100f).coerceIn(0f, 1f),
                barHeight = AppTheme.spacing.spaceXs,
                modifier = Modifier.weight(1f),
                normalColor = AppTheme.colors.operational,
                warningColor = AppTheme.colors.degraded,
                criticalColor = AppTheme.colors.critical,
            )
        }

        if (cpu.usageHistory.size >= 2) {
            MiniSparkline(
                data = cpu.usageHistory,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = AppTheme.dimensions.sparklineHeight),
                height = null,
                lineColor = AppTheme.colors.telemetryA,
                minValue = 0f,
                // Scaled to the recent peak, so a quiet host still draws a readable line.
                maxValue = (cpu.usageHistoryPeak * 1.3f).coerceIn(ROOMY_MIN_SCALE, 100f),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        cpu.cores.chunked(ROOMY_CORE_COLUMNS).forEachIndexed { rowIndex, rowCores ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
            ) {
                rowCores.forEachIndexed { column, core ->
                    CoreTile(
                        coreIndex = rowIndex * ROOMY_CORE_COLUMNS + column,
                        usage = core.usagePercent,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(ROOMY_CORE_COLUMNS - rowCores.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** A core as a tile: index and percentage on top, the usage bar below. */
@Composable
private fun CoreTile(
    coreIndex: Int,
    usage: Float,
    modifier: Modifier = Modifier,
) {
    val ratio = (usage / 100f).coerceIn(0f, 1f)
    val status = CpuCoreUi.usageStatus(usage)

    Column(
        modifier = modifier
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.sm)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.sm,
            )
            .padding(horizontal = AppTheme.spacing.spaceSm, vertical = AppTheme.spacing.spaceXs),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatCoreIndex(coreIndex),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = stringResource(Res.string.percentage_format, (ratio * 100).toInt()),
                style = AppTheme.typography.codeBase,
                color = when (status) {
                    StatusLevel.Critical -> AppTheme.colors.critical
                    StatusLevel.Degraded -> AppTheme.colors.degraded
                    StatusLevel.Operational -> AppTheme.colors.textPrimary
                },
                maxLines = 1,
                softWrap = false,
            )
        }
        LinearGauge(
            progress = ratio,
            barHeight = AppTheme.spacing.spaceXs,
            modifier = Modifier.fillMaxWidth(),
            normalColor = AppTheme.colors.operational,
            warningColor = AppTheme.colors.degraded,
            criticalColor = AppTheme.colors.critical,
            shape = AppTheme.shapes.sm,
        )
    }
}

@Composable
private fun DenseCpuContent(
    viewState: ViewState,
    modifier: Modifier = Modifier,
) {
    val ratio = (viewState.cpu.usagePercent / 100f).coerceIn(0f, 1f)
    val cores = viewState.cpu.cores.map { it.usagePercent }
    val overflow = (cores.size - FIXED_HEIGHT_MAX_CORES).coerceAtLeast(0)
    val shownCores = if (overflow > 0) cores.take(FIXED_HEIGHT_MAX_CORES - 1) else cores
    val hiddenCount = cores.size - shownCores.size

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetricValue(
                value = formatPercentValue(viewState.cpu.usagePercent),
                unit = "%",
                size = MetricSize.Medium,
                inlineTrend = false,
            )
            LinearGauge(
                progress = ratio,
                barHeight = AppTheme.spacing.spaceXs,
                modifier = Modifier.weight(1f),
                normalColor = AppTheme.colors.operational,
                warningColor = AppTheme.colors.degraded,
                criticalColor = AppTheme.colors.critical,
            )
        }

        val columns = 4
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
        ) {
            val items: List<Pair<Int, Float>?> = shownCores.mapIndexed { index, usage -> index to usage } +
                if (hiddenCount > 0) listOf(null) else emptyList()
            items.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
                ) {
                    rowItems.forEach { item ->
                        if (item != null) {
                            CoreCell(
                                coreIndex = item.first,
                                usage = item.second,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(Res.string.widget_overflow_more, hiddenCount),
                                    style = AppTheme.typography.labelCode,
                                    color = AppTheme.colors.textMuted,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private const val FIXED_HEIGHT_MAX_CORES = 16

@Composable
private fun WideCpuMatrixContent(
    viewState: ViewState,
    maxWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
    ) {
        AggregateUsageWide(
            usage = viewState.cpu.usagePercent,
            trend = viewState.cpu.trend,
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.hairlineBorder)
                .background(AppTheme.colors.subtleDivider),
        )

        CoreMatrixWide(
            coreUsages = viewState.cpu.cores.map { it.usagePercent },
            maxWidth = maxWidth,
        )
    }
}

@Composable
private fun CompactCpuMatrixContent(
    viewState: ViewState,
    maxWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        AggregateUsageCompact(
            usage = viewState.cpu.usagePercent,
            trend = viewState.cpu.trend,
            frequencyGhz = viewState.cpu.frequencyGhz,
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.hairlineBorder)
                .background(AppTheme.colors.subtleDivider),
        )

        CoreMatrixCompact(
            coreUsages = viewState.cpu.cores.map { it.usagePercent },
            maxWidth = maxWidth,
        )
    }
}

@Composable
private fun AggregateUsageWide(
    usage: Float,
    trend: String?,
    modifier: Modifier = Modifier,
) {
    val ratio = (usage / 100f).coerceIn(0f, 1f)
    val percentage = (ratio * 100).toInt()
    val status = CpuCoreUi.usageStatus(usage)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            if (trend != null) {
                MetricValue(
                    value = formatPercentValue(usage),
                    unit = "%",
                    trend = trend,
                    trendStatus = status,
                    size = MetricSize.Display,
                    inlineTrend = true,
                )
            } else {
                MetricValue(
                    value = formatPercentValue(usage),
                    unit = "%",
                    size = MetricSize.Display,
                    inlineTrend = true,
                )
            }

            Text(
                text = stringResource(Res.string.percentage_format, percentage),
                style = AppTheme.typography.labelCode,
                color = when (status) {
                    StatusLevel.Critical -> AppTheme.colors.critical
                    StatusLevel.Degraded -> AppTheme.colors.degraded
                    StatusLevel.Operational -> AppTheme.colors.textSecondary
                },
            )
        }

        LinearGauge(
            progress = ratio,
            barHeight = AppTheme.spacing.spaceSm,
            normalColor = AppTheme.colors.operational,
            warningColor = AppTheme.colors.degraded,
            criticalColor = AppTheme.colors.critical,
        )
    }
}

@Composable
private fun AggregateUsageCompact(
    usage: Float,
    trend: String?,
    frequencyGhz: Float?,
    modifier: Modifier = Modifier,
) {
    val ratio = (usage / 100f).coerceIn(0f, 1f)
    val status = CpuCoreUi.usageStatus(usage)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            MetricValue(
                value = formatPercentValue(usage),
                unit = "%",
                size = MetricSize.Medium,
                inlineTrend = false,
            )

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
            ) {
                if (frequencyGhz != null) {
                    TagChip(text = formatFrequency(frequencyGhz))
                }
                if (trend != null) {
                    TrendBadge(
                        text = trend,
                        status = status,
                    )
                }
            }
        }

        LinearGauge(
            progress = ratio,
            barHeight = AppTheme.spacing.spaceXs,
            normalColor = AppTheme.colors.operational,
            warningColor = AppTheme.colors.degraded,
            criticalColor = AppTheme.colors.critical,
        )
    }
}

@Composable
private fun CoreMatrixWide(
    coreUsages: List<Float>,
    maxWidth: Dp,
    modifier: Modifier = Modifier,
) {
    if (coreUsages.isEmpty()) return

    val columns = if (maxWidth >= 540.dp && coreUsages.size >= 16) 8 else 4

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
                text = stringResource(Res.string.widget_cpu_matrix_core_matrix),
                style = AppTheme.typography.labelUi,
                color = AppTheme.colors.textMuted,
            )
            Text(
                text = stringResource(Res.string.widget_cpu_matrix_cores_active, coreUsages.size),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textSecondary,
            )
        }

        val chunkedCores = coreUsages.mapIndexed { index, usage -> index to usage }.chunked(columns)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
        ) {
            chunkedCores.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
                ) {
                    rowItems.forEach { (coreIndex, usage) ->
                        CoreCell(
                            coreIndex = coreIndex,
                            usage = usage,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CoreMatrixCompact(
    coreUsages: List<Float>,
    maxWidth: Dp,
    modifier: Modifier = Modifier,
) {
    if (coreUsages.isEmpty()) return

    val columns = if (maxWidth >= 200.dp) 2 else 1

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
                text = stringResource(Res.string.widget_cpu_matrix_core_matrix_compact),
                style = AppTheme.typography.labelUi,
                color = AppTheme.colors.textMuted,
            )
            Text(
                text = stringResource(Res.string.widget_cpu_matrix_cores_compact, coreUsages.size),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textSecondary,
            )
        }

        val chunkedCores = coreUsages.mapIndexed { index, usage -> index to usage }.chunked(columns)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
        ) {
            chunkedCores.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
                ) {
                    rowItems.forEach { (coreIndex, usage) ->
                        CoreCell(
                            coreIndex = coreIndex,
                            usage = usage,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CoreCell(
    coreIndex: Int,
    usage: Float,
    modifier: Modifier = Modifier,
) {
    val ratio = (usage / 100f).coerceIn(0f, 1f)
    val percentage = (ratio * 100).toInt()
    val status = CpuCoreUi.usageStatus(usage)

    Row(
        modifier = modifier
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.sm)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.sm,
            )
            .padding(
                horizontal = AppTheme.spacing.spaceXs,
                vertical = AppTheme.spacing.space2xs,
            ),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatCoreIndex(coreIndex),
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.textMuted,
            maxLines = 1,
            softWrap = false,
        )

        LinearGauge(
            progress = ratio,
            barHeight = AppTheme.spacing.space2xs,
            modifier = Modifier.weight(1f),
            normalColor = AppTheme.colors.operational,
            warningColor = AppTheme.colors.degraded,
            criticalColor = AppTheme.colors.critical,
            shape = AppTheme.shapes.sm,
        )

        Text(
            text = stringResource(Res.string.percentage_format, percentage),
            style = AppTheme.typography.labelCode,
            color = when (status) {
                StatusLevel.Critical -> AppTheme.colors.critical
                StatusLevel.Degraded -> AppTheme.colors.degraded
                StatusLevel.Operational -> AppTheme.colors.textSecondary
            },
            maxLines = 1,
            softWrap = false,
        )
    }
}

internal fun formatCoreIndex(index: Int): String {
    return index.toString().padStart(2, '0')
}

internal fun formatFrequency(ghz: Float): String {
    val rounded = (ghz * 100).toInt()
    val whole = rounded / 100
    val fraction = abs(rounded % 100)
    val fracStr = if (fraction < 10) "0$fraction" else "$fraction"
    return "$whole.$fracStr GHz"
}

internal fun formatTemperature(tempC: Float): String {
    return "${tempC.toInt()}°C"
}

internal fun formatPercentValue(value: Float): String {
    val rounded = (value * 10).toInt()
    val whole = rounded / 10
    val fraction = abs(rounded % 10)
    return "$whole.$fraction"
}

@Preview
@Composable
private fun CpuMatrixWidgetPreview(
    @PreviewParameter(CpuMatrixPreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        CpuMatrixWidget(
            viewState = viewState,
            onViewAction = {},
        )
    }
}
