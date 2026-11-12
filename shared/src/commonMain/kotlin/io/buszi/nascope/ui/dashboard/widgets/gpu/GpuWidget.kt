package io.buszi.nascope.ui.dashboard.widgets.gpu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.widget_gpu_fan
import io.buszi.nascope.resources.widget_gpu_memory
import io.buszi.nascope.resources.widget_gpu_no_gpu
import io.buszi.nascope.resources.widget_gpu_status_critical
import io.buszi.nascope.resources.widget_gpu_status_elevated
import io.buszi.nascope.resources.widget_gpu_status_nominal
import io.buszi.nascope.resources.widget_gpu_subtitle_count
import io.buszi.nascope.resources.widget_gpu_title
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.gauges.MiniSparkline
import io.buszi.nascope.ui.common.components.metrics.MetricSize
import io.buszi.nascope.ui.common.components.metrics.MetricValue
import io.buszi.nascope.ui.common.model.CpuCoreUi
import io.buszi.nascope.ui.common.model.GpuUi
import io.buszi.nascope.ui.common.modifier.optionalModifier
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.widgets.DashboardWidget
import io.buszi.nascope.ui.dashboard.widgets.cpu.formatPercentValue
import io.buszi.nascope.ui.dashboard.widgets.cpu.formatTemperature
import io.buszi.nascope.ui.dashboard.widgets.gpu.GpuViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.widgets.gpu.GpuViewModel.ViewState
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun GpuWidget(
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<GpuViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    GpuWidget(
        viewState = viewState,
        modifier = modifier,
        onViewAction = viewModel::onViewAction,
    )
}

@Composable
fun GpuWidget(
    viewState: ViewState,
    modifier: Modifier = Modifier,
    onViewAction: (ViewAction) -> Unit = {},
) {
    val isReady = viewState.widgetState.isReady
    val status = viewState.overallStatus
    val statusLabel = when (status) {
        StatusLevel.Critical -> stringResource(Res.string.widget_gpu_status_critical)
        StatusLevel.Degraded -> stringResource(Res.string.widget_gpu_status_elevated)
        StatusLevel.Operational -> stringResource(Res.string.widget_gpu_status_nominal)
    }
    val subtitle = when (viewState.gpus.size) {
        0 -> null
        1 -> viewState.gpus.first().name
        else -> pluralStringResource(Res.plurals.widget_gpu_subtitle_count, viewState.gpus.size, viewState.gpus.size)
    }
    // The hottest GPU stands for all in the header.
    val temperature = viewState.gpus.mapNotNull { it.temperatureC }.maxOrNull()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isFixedHeight = constraints.hasBoundedHeight

        DashboardWidget(
            title = stringResource(Res.string.widget_gpu_title),
            subtitle = if (isReady) subtitle else null,
            modifier = Modifier.fillMaxSize(),
            state = viewState.widgetState,
            statusTag = if (isReady && viewState.gpus.isNotEmpty()) {
                {
                    StatusChip(
                        text = statusLabel,
                        status = status,
                        pulsing = status == StatusLevel.Critical,
                    )
                }
            } else null,
            headerAction = if (isReady && temperature != null) {
                {
                    val temperatureStatus = CpuCoreUi.temperatureStatus(temperature)
                    StatusChip(
                        text = formatTemperature(temperature),
                        status = temperatureStatus,
                        pulsing = temperatureStatus == StatusLevel.Critical,
                    )
                }
            } else null,
        ) {
            if (viewState.gpus.isEmpty()) {
                EmptyGpuPlaceholder()
            } else {
                GpuContent(gpus = viewState.gpus, fillHeight = isFixedHeight)
            }
        }
    }
}

@Composable
private fun GpuContent(
    gpus: List<GpuUi>,
    fillHeight: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .optionalModifier(fillHeight) { fillMaxHeight() },
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        gpus.forEach { gpu ->
            GpuRow(
                gpu = gpu,
                showName = gpus.size > 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .optionalModifier(fillHeight) { weight(1f) },
                fillHeight = fillHeight,
            )
        }
    }
}

/** One GPU: the utilization on the left, its recent history in the middle and the other figures on the right. */
@Composable
private fun GpuRow(
    gpu: GpuUi,
    showName: Boolean,
    fillHeight: Boolean,
    modifier: Modifier = Modifier,
) {
    val history = gpu.utilizationHistory
    val extras = listOfNotNull(
        gpu.memoryPercent?.let { stringResource(Res.string.widget_gpu_memory, it.roundToInt()) },
        gpu.fanSpeedPercent?.let { stringResource(Res.string.widget_gpu_fan, it.roundToInt()) },
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs)) {
            if (showName) {
                Text(
                    text = gpu.name,
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                    maxLines = 1,
                )
            }
            MetricValue(
                value = gpu.utilizationPercent?.let(::formatPercentValue) ?: "--",
                unit = "%",
                size = MetricSize.Display,
            )
        }

        if (history.size >= 2) {
            MiniSparkline(
                data = history,
                modifier = Modifier
                    .weight(1f)
                    .optionalModifier(fillHeight) { fillMaxHeight().heightIn(min = AppTheme.dimensions.sparklineHeight) },
                height = if (fillHeight) null else AppTheme.dimensions.sparklineHeight,
                lineColor = AppTheme.colors.telemetryB,
                minValue = 0f,
                // Scaled to the recent peak, so a quiet GPU still draws a readable line.
                maxValue = ((history.maxOrNull() ?: 0f) * 1.3f).coerceIn(MinSparklineScale, 100f),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        if (extras.isNotEmpty()) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
            ) {
                extras.forEach { TagChip(text = it) }
            }
        }
    }
}

private const val MinSparklineScale = 25f

@Composable
private fun EmptyGpuPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.spacing.spaceXl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.widget_gpu_no_gpu),
            style = AppTheme.typography.bodySm,
            color = AppTheme.colors.textMuted,
        )
    }
}

@Preview
@Composable
private fun GpuWidgetPreview(
    @PreviewParameter(GpuPreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        GpuWidget(viewState = viewState)
    }
}
