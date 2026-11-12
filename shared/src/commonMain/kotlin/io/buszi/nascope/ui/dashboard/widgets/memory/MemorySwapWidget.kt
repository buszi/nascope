package io.buszi.nascope.ui.dashboard.widgets.memory

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.percentage_format
import io.buszi.nascope.resources.widget_memory_swap_breakdown
import io.buszi.nascope.resources.widget_memory_swap_breakdown_compact
import io.buszi.nascope.resources.widget_memory_swap_buffers
import io.buszi.nascope.resources.widget_memory_swap_cached
import io.buszi.nascope.resources.widget_memory_swap_free
import io.buszi.nascope.resources.widget_memory_swap_gb_format
import io.buszi.nascope.resources.widget_memory_swap_no_swap
import io.buszi.nascope.resources.widget_memory_swap_ram
import io.buszi.nascope.resources.widget_memory_swap_ram_compact
import io.buszi.nascope.resources.widget_memory_swap_status_critical
import io.buszi.nascope.resources.widget_memory_swap_status_elevated
import io.buszi.nascope.resources.widget_memory_swap_status_nominal
import io.buszi.nascope.resources.widget_memory_swap_swap
import io.buszi.nascope.resources.widget_memory_swap_swap_compact
import io.buszi.nascope.resources.widget_memory_swap_swap_ratio
import io.buszi.nascope.resources.widget_memory_swap_title
import io.buszi.nascope.resources.widget_memory_swap_title_compact
import io.buszi.nascope.resources.widget_memory_swap_total_ram
import io.buszi.nascope.resources.widget_memory_swap_used
import io.buszi.nascope.resources.widget_state_value_placeholder
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusDot
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.gauges.LinearGauge
import io.buszi.nascope.ui.common.components.metrics.MetricSize
import io.buszi.nascope.ui.common.components.metrics.MetricValue
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.widgets.DashboardWidget
import io.buszi.nascope.ui.dashboard.widgets.memory.MemorySwapViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.widgets.memory.MemorySwapViewModel.ViewState
import kotlin.math.abs
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val AnimationDurationMillis: Int = 300
private const val SwapWarningThreshold: Float = 0.50f
private const val SwapCriticalThreshold: Float = 0.80f

@Composable
fun MemorySwapWidget(
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<MemorySwapViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    MemorySwapWidget(
        viewState = viewState,
        modifier = modifier,
        onViewAction = viewModel::onViewAction,
    )
}

@Composable
fun MemorySwapWidget(
    viewState: ViewState,
    modifier: Modifier = Modifier,
    onViewAction: (ViewAction) -> Unit = {},
) {
    val usedMemRatio = if (viewState.memory.totalBytes > 0L) {
        (viewState.memory.usedBytes.toFloat() / viewState.memory.totalBytes).coerceIn(0f, 1f)
    } else 0f

    val cachedMemRatio = if (viewState.memory.totalBytes > 0L) {
        ((viewState.memory.cachedBytes ?: 0L).toFloat() / viewState.memory.totalBytes).coerceIn(0f, 1f)
    } else 0f

    val buffersMemRatio = if (viewState.memory.totalBytes > 0L) {
        ((viewState.memory.buffersBytes ?: 0L).toFloat() / viewState.memory.totalBytes).coerceIn(0f, 1f)
    } else 0f

    val freeMemRatio = if (viewState.memory.totalBytes > 0L) {
        (viewState.memory.freeBytes.toFloat() / viewState.memory.totalBytes).coerceIn(0f, 1f)
    } else 0f

    val swapRatio = if (viewState.memory.totalSwapBytes > 0L) {
        (viewState.memory.usedSwapBytes.toFloat() / viewState.memory.totalSwapBytes).coerceIn(0f, 1f)
    } else 0f

    val overallStatus = when {
        usedMemRatio >= 0.90f || swapRatio >= SwapCriticalThreshold -> StatusLevel.Critical
        usedMemRatio >= 0.75f || swapRatio >= SwapWarningThreshold -> StatusLevel.Degraded
        else -> StatusLevel.Operational
    }

    val statusLabel = when (overallStatus) {
        StatusLevel.Critical -> stringResource(Res.string.widget_memory_swap_status_critical)
        StatusLevel.Degraded -> stringResource(Res.string.widget_memory_swap_status_elevated)
        StatusLevel.Operational -> stringResource(Res.string.widget_memory_swap_status_nominal)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val containerWidth = maxWidth
        val isWide = containerWidth >= 330.dp

        DashboardWidget(
            title = if (isWide) {
                stringResource(Res.string.widget_memory_swap_title)
            } else {
                stringResource(Res.string.widget_memory_swap_title_compact)
            },
            subtitle = if (viewState.widgetState.isReady && viewState.memory.totalBytes > 0L) {
                stringResource(Res.string.widget_memory_swap_total_ram, formatGb(viewState.memory.totalBytes))
            } else null,
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
                    if (viewState.memory.totalSwapBytes > 0L) {
                        val swapPercent = (swapRatio * 100).toInt()
                        TagChip(
                            text = "SWAP " + stringResource(Res.string.percentage_format, swapPercent),
                        )
                    }
                }
            } else null,
        ) {
            if (viewState.widgetState.isReady) {
                if (isWide) {
                    WideMemorySwapContent(
                        viewState = viewState,
                        maxWidth = containerWidth,
                        usedMemRatio = usedMemRatio,
                        cachedMemRatio = cachedMemRatio,
                        buffersMemRatio = buffersMemRatio,
                        freeMemRatio = freeMemRatio,
                        swapRatio = swapRatio,
                        overallStatus = overallStatus,
                    )
                } else {
                    CompactMemorySwapContent(
                        viewState = viewState,
                        usedMemRatio = usedMemRatio,
                        cachedMemRatio = cachedMemRatio,
                        buffersMemRatio = buffersMemRatio,
                        freeMemRatio = freeMemRatio,
                        swapRatio = swapRatio,
                        overallStatus = overallStatus,
                    )
                }
            }
        }
    }
}

@Composable
private fun WideMemorySwapContent(
    viewState: ViewState,
    maxWidth: Dp,
    usedMemRatio: Float,
    cachedMemRatio: Float,
    buffersMemRatio: Float,
    freeMemRatio: Float,
    swapRatio: Float,
    overallStatus: StatusLevel,
    modifier: Modifier = Modifier,
) {
    val usedColor = when (overallStatus) {
        StatusLevel.Critical -> AppTheme.colors.critical
        StatusLevel.Degraded -> AppTheme.colors.degraded
        StatusLevel.Operational -> AppTheme.colors.telemetryA
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
    ) {
        // RAM Metric Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            MetricValue(
                value = formatGb(viewState.memory.usedBytes),
                unit = "GB",
                size = MetricSize.Display,
            )

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
            ) {
                Text(
                    text = stringResource(Res.string.widget_memory_swap_ram),
                    style = AppTheme.typography.labelUi,
                    color = AppTheme.colors.textMuted,
                )
                Text(
                    text = stringResource(Res.string.percentage_format, (usedMemRatio * 100).toInt()),
                    style = AppTheme.typography.labelCode,
                    color = when (overallStatus) {
                        StatusLevel.Critical -> AppTheme.colors.critical
                        StatusLevel.Degraded -> AppTheme.colors.degraded
                        StatusLevel.Operational -> AppTheme.colors.textSecondary
                    },
                )
            }
        }

        // Tiered Bar Gauge
        TieredBarGauge(
            usedRatio = usedMemRatio,
            cachedRatio = cachedMemRatio,
            buffersRatio = buffersMemRatio,
            usedColor = usedColor,
            cachedColor = AppTheme.colors.telemetryB,
            buffersColor = AppTheme.colors.operational,
            barHeight = AppTheme.spacing.spaceSm,
        )

        // Memory Breakdown Cells
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.widget_memory_swap_breakdown),
                    style = AppTheme.typography.labelUi,
                    color = AppTheme.colors.textMuted,
                )
                Text(
                    text = stringResource(Res.string.widget_memory_swap_total_ram, formatGb(viewState.memory.totalBytes)),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textSecondary,
                )
            }

            if (maxWidth >= 480.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
                ) {
                    MemoryCell(
                        label = stringResource(Res.string.widget_memory_swap_used),
                        bytes = viewState.memory.usedBytes,
                        ratio = usedMemRatio,
                        indicatorColor = usedColor,
                        modifier = Modifier.weight(1f),
                    )
                    MemoryCell(
                        label = stringResource(Res.string.widget_memory_swap_cached),
                        bytes = viewState.memory.cachedBytes,
                        ratio = cachedMemRatio,
                        indicatorColor = AppTheme.colors.telemetryB,
                        modifier = Modifier.weight(1f),
                    )
                    MemoryCell(
                        label = stringResource(Res.string.widget_memory_swap_buffers),
                        bytes = viewState.memory.buffersBytes,
                        ratio = buffersMemRatio,
                        indicatorColor = AppTheme.colors.operational,
                        modifier = Modifier.weight(1f),
                    )
                    MemoryCell(
                        label = stringResource(Res.string.widget_memory_swap_free),
                        bytes = viewState.memory.freeBytes,
                        ratio = freeMemRatio,
                        indicatorColor = AppTheme.colors.textMuted,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
                    ) {
                        MemoryCell(
                            label = stringResource(Res.string.widget_memory_swap_used),
                            bytes = viewState.memory.usedBytes,
                            ratio = usedMemRatio,
                            indicatorColor = usedColor,
                            modifier = Modifier.weight(1f),
                        )
                        MemoryCell(
                            label = stringResource(Res.string.widget_memory_swap_cached),
                            bytes = viewState.memory.cachedBytes,
                            ratio = cachedMemRatio,
                            indicatorColor = AppTheme.colors.telemetryB,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
                    ) {
                        MemoryCell(
                            label = stringResource(Res.string.widget_memory_swap_buffers),
                            bytes = viewState.memory.buffersBytes,
                            ratio = buffersMemRatio,
                            indicatorColor = AppTheme.colors.operational,
                            modifier = Modifier.weight(1f),
                        )
                        MemoryCell(
                            label = stringResource(Res.string.widget_memory_swap_free),
                            bytes = viewState.memory.freeBytes,
                            ratio = freeMemRatio,
                            indicatorColor = AppTheme.colors.textMuted,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.hairlineBorder)
                .background(AppTheme.colors.subtleDivider),
        )

        // Swap Section
        SwapSectionWide(
            usedSwapBytes = viewState.memory.usedSwapBytes,
            totalSwapBytes = viewState.memory.totalSwapBytes,
            swapRatio = swapRatio,
        )
    }
}

@Composable
private fun CompactMemorySwapContent(
    viewState: ViewState,
    usedMemRatio: Float,
    cachedMemRatio: Float,
    buffersMemRatio: Float,
    freeMemRatio: Float,
    swapRatio: Float,
    overallStatus: StatusLevel,
    modifier: Modifier = Modifier,
) {
    val usedColor = when (overallStatus) {
        StatusLevel.Critical -> AppTheme.colors.critical
        StatusLevel.Degraded -> AppTheme.colors.degraded
        StatusLevel.Operational -> AppTheme.colors.telemetryA
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        // Compact RAM Identity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            MetricValue(
                value = formatGb(viewState.memory.usedBytes),
                unit = "GB",
                size = MetricSize.Medium,
            )

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
            ) {
                TagChip(text = stringResource(Res.string.widget_memory_swap_ram_compact))
                Text(
                    text = stringResource(Res.string.percentage_format, (usedMemRatio * 100).toInt()),
                    style = AppTheme.typography.labelCode,
                    color = when (overallStatus) {
                        StatusLevel.Critical -> AppTheme.colors.critical
                        StatusLevel.Degraded -> AppTheme.colors.degraded
                        StatusLevel.Operational -> AppTheme.colors.textSecondary
                    },
                )
            }
        }

        // Compact Tiered Bar Gauge
        TieredBarGauge(
            usedRatio = usedMemRatio,
            cachedRatio = cachedMemRatio,
            buffersRatio = buffersMemRatio,
            usedColor = usedColor,
            cachedColor = AppTheme.colors.telemetryB,
            buffersColor = AppTheme.colors.operational,
            barHeight = AppTheme.spacing.spaceXs,
        )

        // Compact Breakdown List
        Text(
            text = stringResource(Res.string.widget_memory_swap_breakdown_compact),
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
            CompactMemoryRow(
                label = stringResource(Res.string.widget_memory_swap_used),
                bytes = viewState.memory.usedBytes,
                ratio = usedMemRatio,
                indicatorColor = usedColor,
            )
            CompactMemoryRow(
                label = stringResource(Res.string.widget_memory_swap_cached),
                bytes = viewState.memory.cachedBytes,
                ratio = cachedMemRatio,
                indicatorColor = AppTheme.colors.telemetryB,
            )
            CompactMemoryRow(
                label = stringResource(Res.string.widget_memory_swap_buffers),
                bytes = viewState.memory.buffersBytes,
                ratio = buffersMemRatio,
                indicatorColor = AppTheme.colors.operational,
            )
            CompactMemoryRow(
                label = stringResource(Res.string.widget_memory_swap_free),
                bytes = viewState.memory.freeBytes,
                ratio = freeMemRatio,
                indicatorColor = AppTheme.colors.textMuted,
            )
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.hairlineBorder)
                .background(AppTheme.colors.subtleDivider),
        )

        // Compact Swap Section
        SwapSectionCompact(
            usedSwapBytes = viewState.memory.usedSwapBytes,
            totalSwapBytes = viewState.memory.totalSwapBytes,
            swapRatio = swapRatio,
        )
    }
}

@Composable
private fun TieredBarGauge(
    usedRatio: Float,
    cachedRatio: Float,
    buffersRatio: Float,
    modifier: Modifier = Modifier,
    usedColor: Color = AppTheme.colors.telemetryA,
    cachedColor: Color = AppTheme.colors.telemetryB,
    buffersColor: Color = AppTheme.colors.operational,
    trackColor: Color = AppTheme.colors.inputBackground,
    borderColor: Color = AppTheme.colors.baseBorder,
    barHeight: Dp = AppTheme.spacing.spaceSm,
    shape: Shape = AppTheme.shapes.sm,
) {
    val clampedUsed = usedRatio.coerceIn(0f, 1f)
    val clampedCached = cachedRatio.coerceIn(0f, (1f - clampedUsed).coerceAtLeast(0f))
    val clampedBuffers = buffersRatio.coerceIn(0f, (1f - clampedUsed - clampedCached).coerceAtLeast(0f))

    val animatedUsed by animateFloatAsState(
        targetValue = clampedUsed,
        animationSpec = tween(
            durationMillis = AnimationDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "TieredGaugeUsedProgress",
    )

    val animatedCached by animateFloatAsState(
        targetValue = clampedCached,
        animationSpec = tween(
            durationMillis = AnimationDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "TieredGaugeCachedProgress",
    )

    val animatedBuffers by animateFloatAsState(
        targetValue = clampedBuffers,
        animationSpec = tween(
            durationMillis = AnimationDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "TieredGaugeBuffersProgress",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .background(color = trackColor, shape = shape)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = borderColor,
                shape = shape,
            )
            .clip(shape),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            val totalWidth = size.width
            val usedWidth = totalWidth * animatedUsed
            val cachedWidth = totalWidth * animatedCached
            val buffersWidth = totalWidth * animatedBuffers

            if (usedWidth > 0f) {
                drawRect(
                    color = usedColor,
                    topLeft = Offset.Zero,
                    size = Size(width = usedWidth, height = size.height),
                )
            }
            if (cachedWidth > 0f) {
                drawRect(
                    color = cachedColor,
                    topLeft = Offset(x = usedWidth, y = 0f),
                    size = Size(width = cachedWidth, height = size.height),
                )
            }
            if (buffersWidth > 0f) {
                drawRect(
                    color = buffersColor,
                    topLeft = Offset(x = usedWidth + cachedWidth, y = 0f),
                    size = Size(width = buffersWidth, height = size.height),
                )
            }
        }
    }
}

@Composable
private fun MemoryCell(
    label: String,
    bytes: Long?,
    ratio: Float,
    indicatorColor: Color,
    modifier: Modifier = Modifier,
) {
    val percentage = (ratio.coerceIn(0f, 1f) * 100).toInt()
    val percentageText = if (bytes == null) {
        stringResource(Res.string.widget_state_value_placeholder)
    } else {
        stringResource(Res.string.percentage_format, percentage)
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
            Box(
                modifier = Modifier
                    .size(AppTheme.spacing.spaceXs)
                    .background(indicatorColor, AppTheme.shapes.full),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = bytes?.let { stringResource(Res.string.widget_memory_swap_gb_format, formatGb(it)) }
                ?: stringResource(Res.string.widget_state_value_placeholder),
                style = AppTheme.typography.metricMd,
                color = AppTheme.colors.textPrimary,
            )
            Text(
                text = percentageText,
                style = AppTheme.typography.labelCode,
                color = indicatorColor,
            )
        }
    }
}

@Composable
private fun CompactMemoryRow(
    label: String,
    bytes: Long?,
    ratio: Float,
    indicatorColor: Color,
    modifier: Modifier = Modifier,
) {
    val percentage = (ratio.coerceIn(0f, 1f) * 100).toInt()
    val percentageText = if (bytes == null) {
        stringResource(Res.string.widget_state_value_placeholder)
    } else {
        stringResource(Res.string.percentage_format, percentage)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(AppTheme.spacing.spaceXs)
                .background(indicatorColor, AppTheme.shapes.full),
        )
        Text(
            text = label,
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.textMuted,
            modifier = Modifier.width(52.dp),
        )
        Text(
            text = bytes?.let { stringResource(Res.string.widget_memory_swap_gb_format, formatGb(it)) }
                ?: stringResource(Res.string.widget_state_value_placeholder),
            style = AppTheme.typography.codeBase,
            color = AppTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = percentageText,
            style = AppTheme.typography.labelCode,
            color = indicatorColor,
            modifier = Modifier.width(36.dp),
        )
    }
}

@Composable
private fun SwapSectionWide(
    usedSwapBytes: Long,
    totalSwapBytes: Long,
    swapRatio: Float,
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
                text = stringResource(Res.string.widget_memory_swap_swap),
                style = AppTheme.typography.labelUi,
                color = AppTheme.colors.textMuted,
            )
            if (totalSwapBytes > 0L) {
                Text(
                    text = stringResource(
                        Res.string.widget_memory_swap_swap_ratio,
                        formatGb(usedSwapBytes),
                        formatGb(totalSwapBytes),
                    ),
                    style = AppTheme.typography.labelCode,
                    color = when {
                        swapRatio >= SwapCriticalThreshold -> AppTheme.colors.critical
                        swapRatio >= SwapWarningThreshold -> AppTheme.colors.degraded
                        else -> AppTheme.colors.textSecondary
                    },
                )
            } else {
                Text(
                    text = stringResource(Res.string.widget_memory_swap_no_swap),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textDisabled,
                )
            }
        }

        LinearGauge(
            progress = swapRatio,
            barHeight = AppTheme.spacing.spaceSm,
            warningThreshold = SwapWarningThreshold,
            criticalThreshold = SwapCriticalThreshold,
            normalColor = AppTheme.colors.operational,
            warningColor = AppTheme.colors.degraded,
            criticalColor = AppTheme.colors.critical,
        )
    }
}

@Composable
private fun SwapSectionCompact(
    usedSwapBytes: Long,
    totalSwapBytes: Long,
    swapRatio: Float,
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
                text = stringResource(Res.string.widget_memory_swap_swap_compact),
                style = AppTheme.typography.labelUi,
                color = AppTheme.colors.textMuted,
            )
            if (totalSwapBytes > 0L) {
                Text(
                    text = stringResource(
                        Res.string.widget_memory_swap_swap_ratio,
                        formatGb(usedSwapBytes),
                        formatGb(totalSwapBytes),
                    ),
                    style = AppTheme.typography.labelCode,
                    color = when {
                        swapRatio >= SwapCriticalThreshold -> AppTheme.colors.critical
                        swapRatio >= SwapWarningThreshold -> AppTheme.colors.degraded
                        else -> AppTheme.colors.textSecondary
                    },
                )
            } else {
                Text(
                    text = stringResource(Res.string.widget_memory_swap_no_swap),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textDisabled,
                )
            }
        }

        LinearGauge(
            progress = swapRatio,
            barHeight = AppTheme.spacing.spaceXs,
            warningThreshold = SwapWarningThreshold,
            criticalThreshold = SwapCriticalThreshold,
            normalColor = AppTheme.colors.operational,
            warningColor = AppTheme.colors.degraded,
            criticalColor = AppTheme.colors.critical,
        )
    }
}

private fun formatGb(bytes: Long): String {
    if (bytes <= 0L) return "0.0"
    val gigabytes = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val rounded = kotlin.math.round(gigabytes * 10).toLong()
    val whole = rounded / 10
    val fraction = abs((rounded % 10).toInt())
    return "$whole.$fraction"
}

@Preview
@Composable
private fun MemorySwapWidgetPreview(
    @PreviewParameter(MemorySwapPreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        MemorySwapWidget(
            viewState = viewState,
            onViewAction = {},
        )
    }
}
