package io.buszi.nascope.ui.dashboard.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.system_overview
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusDot
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.gauges.LinearGauge
import io.buszi.nascope.ui.common.theme.AppTheme
import kotlin.math.abs
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Self-contained overload of [SystemOverviewWidget] backed by [SystemOverviewViewModel].
 *
 * Automatically resolves the ViewModel via Koin dependency injection, collects
 * reactive telemetry state, and wires up retry dispatching.
 *
 * @param modifier Layout modifier applied to the widget container.
 * @param viewModel ViewModel governing this widget, injected via Koin by default.
 */
@Composable
fun SystemOverviewWidget(
    modifier: Modifier = Modifier,
    viewModel: SystemOverviewViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SystemOverviewWidget(
        data = uiState.data,
        modifier = modifier,
        state = uiState.state,
    )
}

/**
 * Modular telemetry brick summarizing core host system identification and load.
 *
 * Implements the Obsidian Telemetry instrument aesthetic with full adaptivity:
 * - Host identification: Hostname, platform icon, OS/Kernel release, and uptime.
 * - Load averages: 1-minute, 5-minute, and 15-minute readings normalized to CPU core count.
 * - Responsive layout: Automatically transitions between compact and expanded configurations
 *   based on available container width without layout clipping.
 *
 * @param data Host system telemetry specifications and load readings.
 * @param modifier Layout modifier applied to the widget container.
 * @param state Lifecycle status of the widget ([WidgetState.Ready], [WidgetState.Loading], etc.).
 * @param title Section header label. Defaults to localized [Res.string.system_overview].
 * @param headerAction Optional custom trailing slot in the header.
 */
@Composable
fun SystemOverviewWidget(
    data: SystemOverviewData,
    modifier: Modifier = Modifier,
    state: WidgetState = WidgetState.Ready,
    title: String = stringResource(Res.string.system_overview),
    headerAction: (@Composable RowScope.() -> Unit)? = null,
) {
    DashboardWidget(
        title = title,
        subtitle = if (state is WidgetState.Ready) data.osName else null,
        modifier = modifier,
        state = state,
        statusTag = if (state is WidgetState.Ready) {
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                ) {
                    StatusChip(
                        text = data.status.name.uppercase(),
                        status = data.status,
                    )
                    if (!data.architecture.isNullOrBlank()) {
                        TagChip(text = data.architecture)
                    }
                }
            }
        } else null,
        headerAction = headerAction ?: if (state is WidgetState.Ready) {
            {
                TagChip(
                    text = "UP ${data.uptime}",
                    dotColor = AppTheme.colors.operational,
                )
            }
        } else null,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isExpanded = maxWidth >= AppTheme.dimensions.compactWidgetBreakpoint

            if (isExpanded) {
                ExpandedSystemOverviewContent(data = data)
            } else {
                CompactSystemOverviewContent(data = data)
            }
        }
    }
}

@Composable
fun SystemOverviewWidget(
    state: WidgetState,
    modifier: Modifier = Modifier,
    data: SystemOverviewData? = null,
    title: String = stringResource(Res.string.system_overview),
    headerAction: (@Composable RowScope.() -> Unit)? = null,
) {
    if (data != null) {
        SystemOverviewWidget(
            data = data,
            modifier = modifier,
            state = state,
            title = title,
            headerAction = headerAction,
        )
    } else {
        DashboardWidget(
            title = title,
            modifier = modifier,
            state = state,
            headerAction = headerAction,
        ) {
            // Empty placeholder for Loading / Error / Offline states
        }
    }
}

@Composable
private fun ExpandedSystemOverviewContent(
    data: SystemOverviewData,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
    ) {
        // Top Host Identity Row
        HostIdentityRow(data = data)

        // Subtle Divider Line
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.hairlineBorder)
                .background(AppTheme.colors.subtleDivider),
        )

        // Lower Grid: Metadata on left, Load Averages on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceLg),
        ) {
            Column(
                modifier = Modifier.weight(1.1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                Text(
                    text = "HOST TELEMETRY SPEC",
                    style = AppTheme.typography.labelUi,
                    color = AppTheme.colors.textMuted,
                )
                SystemMetaRow(label = "PLATFORM", value = data.platform.displayName)
                SystemMetaRow(label = "KERNEL", value = data.kernelRelease)
                SystemMetaRow(label = "UPTIME", value = data.uptime)
            }

            Column(
                modifier = Modifier.weight(1.3f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                LoadSectionHeader(cpuCores = data.loadAverages.cpuCores)
                LoadMetricsRow(loadAverages = data.loadAverages)
            }
        }
    }
}

@Composable
private fun CompactSystemOverviewContent(
    data: SystemOverviewData,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        // Host Identity
        HostIdentityRow(data = data)

        // Compact Metadata
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
            SystemMetaRow(label = "KERNEL", value = data.kernelRelease)
            SystemMetaRow(label = "PLATFORM", value = data.platform.displayName)
            SystemMetaRow(label = "UPTIME", value = data.uptime)
        }

        Spacer(modifier = Modifier.height(AppTheme.spacing.space2xs))

        // Load Averages
        LoadSectionHeader(cpuCores = data.loadAverages.cpuCores)
        LoadMetricsRow(loadAverages = data.loadAverages)
    }
}

@Composable
private fun HostIdentityRow(
    data: SystemOverviewData,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
                platform = data.platform,
                modifier = Modifier.size(AppTheme.spacing.spaceLg),
                tint = AppTheme.colors.telemetryA,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.hostname,
                style = AppTheme.typography.headlineSm,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(AppTheme.spacing.space2xs))
            Text(
                text = "${data.osName} // ${data.kernelRelease}",
                style = AppTheme.typography.bodySm,
                color = AppTheme.colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LoadSectionHeader(
    cpuCores: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "LOAD AVERAGES",
            style = AppTheme.typography.labelUi,
            color = AppTheme.colors.textMuted,
        )
        Text(
            text = "$cpuCores CORES",
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun LoadMetricsRow(
    loadAverages: LoadAverages,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        LoadMetricCell(
            label = "1 MIN",
            value = loadAverages.oneMinute,
            ratio = loadAverages.oneMinuteRatio,
            status = loadAverages.statusFor(loadAverages.oneMinute),
            modifier = Modifier.weight(1f),
        )
        LoadMetricCell(
            label = "5 MIN",
            value = loadAverages.fiveMinute,
            ratio = loadAverages.fiveMinuteRatio,
            status = loadAverages.statusFor(loadAverages.fiveMinute),
            modifier = Modifier.weight(1f),
        )
        LoadMetricCell(
            label = "15 MIN",
            value = loadAverages.fifteenMinute,
            ratio = loadAverages.fifteenMinuteRatio,
            status = loadAverages.statusFor(loadAverages.fifteenMinute),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LoadMetricCell(
    label: String,
    value: Float,
    ratio: Float,
    status: StatusLevel,
    modifier: Modifier = Modifier,
) {
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
                style = AppTheme.typography.labelUi,
                color = AppTheme.colors.textMuted,
            )
            StatusDot(
                status = status,
                pulsing = status == StatusLevel.Critical,
            )
        }

        Text(
            text = formatLoadValue(value),
            style = AppTheme.typography.metricMd,
            color = AppTheme.colors.textPrimary,
        )

        LinearGauge(
            progress = ratio,
            barHeight = AppTheme.spacing.space2xs,
            shape = AppTheme.shapes.sm,
            normalColor = AppTheme.colors.operational,
            warningColor = AppTheme.colors.degraded,
            criticalColor = AppTheme.colors.critical,
        )
    }
}

@Composable
private fun SystemMetaRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppTheme.typography.labelUi,
            color = AppTheme.colors.textMuted,
        )
        Text(
            text = value,
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.textSecondary,
        )
    }
}

private fun formatLoadValue(value: Float): String {
    val rounded = (value * 100).toInt()
    val whole = rounded / 100
    val fraction = abs(rounded % 100)
    val fracStr = if (fraction < 10) "0$fraction" else "$fraction"
    return "$whole.$fracStr"
}

@Preview
@Composable
private fun SystemOverviewWidgetPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(AppTheme.colors.canvasBackground)
                .padding(AppTheme.spacing.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceLg),
        ) {
            // Expanded preview
            SystemOverviewWidget(
                data = SystemOverviewPreviewParameterProvider.UbuntuServer,
                modifier = Modifier.fillMaxWidth(),
            )

            // Compact preview
            SystemOverviewWidget(
                data = SystemOverviewPreviewParameterProvider.SynologyNas,
                modifier = Modifier.width(AppTheme.spacing.space2xl * 10),
            )

            // Loading state preview
            SystemOverviewWidget(
                state = WidgetState.Loading,
                modifier = Modifier.fillMaxWidth(),
            )

            // Error state preview
            SystemOverviewWidget(
                state = WidgetState.Error(
                    message = "Connection timeout reaching daemon at 192.168.1.120:61208",
                    onRetry = {},
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
