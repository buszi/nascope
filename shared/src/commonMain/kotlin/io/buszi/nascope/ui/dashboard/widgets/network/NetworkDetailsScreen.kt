package io.buszi.nascope.ui.dashboard.widgets.network

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import io.buszi.nascope.resources.details_network_field_link_speed
import io.buszi.nascope.resources.details_network_field_total_rx
import io.buszi.nascope.resources.details_network_field_total_tx
import io.buszi.nascope.resources.details_network_interfaces
import io.buszi.nascope.resources.details_network_link_down
import io.buszi.nascope.resources.details_network_link_speed_format
import io.buszi.nascope.resources.details_network_link_up
import io.buszi.nascope.resources.details_network_link_utilization
import io.buszi.nascope.resources.details_network_overview
import io.buszi.nascope.resources.details_network_peak_format
import io.buszi.nascope.resources.details_network_stat_total_rx
import io.buszi.nascope.resources.details_network_stat_total_tx
import io.buszi.nascope.resources.details_network_subtitle
import io.buszi.nascope.resources.details_network_title
import io.buszi.nascope.resources.details_network_unknown_speed
import io.buszi.nascope.resources.percentage_format
import io.buszi.nascope.resources.widget_network_no_interfaces
import io.buszi.nascope.resources.widget_network_rx
import io.buszi.nascope.resources.widget_network_status_degraded
import io.buszi.nascope.resources.widget_network_status_offline
import io.buszi.nascope.resources.widget_network_status_online
import io.buszi.nascope.resources.widget_network_tx
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.gauges.LinearGauge
import io.buszi.nascope.ui.common.components.gauges.MiniSparkline
import io.buszi.nascope.ui.common.components.metrics.MetricSize
import io.buszi.nascope.ui.common.components.metrics.MetricValue
import io.buszi.nascope.ui.common.model.NetworkInterfaceUi
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkDetailsViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkDetailsViewModel.ViewState
import io.buszi.nascope.ui.details.components.DetailsKeyValueRow
import io.buszi.nascope.ui.details.components.DetailsStatePlaceholder
import io.buszi.nascope.ui.details.components.DetailsScaffold
import io.buszi.nascope.ui.details.components.DetailsSection
import io.buszi.nascope.ui.details.components.DetailsStat
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NetworkDetailsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<NetworkDetailsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    NetworkDetailsScreen(
        viewState = viewState,
        onClose = onClose,
        modifier = modifier,
        onViewAction = viewModel::onViewAction,
    )
}

@Composable
fun NetworkDetailsScreen(
    viewState: ViewState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onViewAction: (ViewAction) -> Unit = {},
) {
    DetailsScaffold(
        title = stringResource(Res.string.details_network_title),
        onClose = onClose,
        testTag = TestTags.details(DashboardBrickId.NetworkIo),
        modifier = modifier,
        widgetState = viewState.widgetState,
        subtitle = if (viewState.widgetState.isReady) {
            stringResource(
                Res.string.details_network_subtitle,
                viewState.activeInterfaces.size,
                viewState.interfaces.size,
            )
        } else {
            null
        },
        statusTag = if (viewState.widgetState.isReady) {
            {
                StatusChip(
                    text = when (viewState.overallStatus) {
                        StatusLevel.Operational -> stringResource(Res.string.widget_network_status_online)
                        StatusLevel.Degraded -> stringResource(Res.string.widget_network_status_degraded)
                        StatusLevel.Critical -> stringResource(Res.string.widget_network_status_offline)
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
            DetailsStatePlaceholder(viewState.widgetState)
        } else if (viewState.interfaces.isEmpty()) {
            Text(
                text = stringResource(Res.string.widget_network_no_interfaces),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
            )
        } else {
            OverviewSection(viewState = viewState)
            InterfacesSection(viewState = viewState, onViewAction = onViewAction)
        }
    }
}

@Composable
private fun OverviewSection(
    viewState: ViewState,
    modifier: Modifier = Modifier,
) {
    DetailsSection(
        title = stringResource(Res.string.details_network_overview),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                RateReadout(
                    label = stringResource(Res.string.widget_network_rx),
                    bytesPerSec = viewState.aggregateRxBytesPerSec,
                    color = AppTheme.colors.telemetryB,
                    modifier = Modifier.weight(1f),
                )
                RateReadout(
                    label = stringResource(Res.string.widget_network_tx),
                    bytesPerSec = viewState.aggregateTxBytesPerSec,
                    color = AppTheme.colors.telemetryA,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                DetailsStat(
                    label = stringResource(Res.string.details_network_stat_total_rx),
                    value = BitrateFormatter.formatDataBytes(viewState.totalRxBytes),
                    modifier = Modifier.weight(1f),
                )
                DetailsStat(
                    label = stringResource(Res.string.details_network_stat_total_tx),
                    value = BitrateFormatter.formatDataBytes(viewState.totalTxBytes),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RateReadout(
    label: String,
    bytesPerSec: Long,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val (value, unit) = BitrateFormatter.formatBitrateParts(bytesPerSec)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
        ) {
            Box(
                modifier = Modifier
                    .size(AppTheme.dimensions.statusDotSize)
                    .background(color, AppTheme.shapes.full),
            )
            Text(
                text = label,
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
                maxLines = 1,
            )
        }
        MetricValue(
            value = value,
            unit = unit,
            size = MetricSize.Medium,
        )
    }
}

@Composable
private fun InterfacesSection(
    viewState: ViewState,
    onViewAction: (ViewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailsSection(
        title = stringResource(Res.string.details_network_interfaces),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
            viewState.interfaces.forEach { iface ->
                InterfaceDetailsCard(
                    iface = iface,
                    expanded = viewState.isExpanded(iface.name),
                    onToggle = { onViewAction(ViewAction.OnToggleInterface(iface.name)) },
                )
            }
        }
    }
}

@Composable
private fun InterfaceDetailsCard(
    iface: NetworkInterfaceUi,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = AppTheme.shapes.base
    val linkStatus = if (iface.isUp) StatusLevel.Operational else StatusLevel.Degraded

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
            .testTag(TestTags.networkInterface(iface.name))
            .clickable(onClick = onToggle)
            .padding(AppTheme.spacing.spaceSm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
        ) {
            Text(
                text = iface.name,
                style = AppTheme.typography.codeBase.copy(fontWeight = FontWeight.SemiBold),
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            StatusChip(
                text = if (iface.isUp) {
                    stringResource(Res.string.details_network_link_up)
                } else {
                    stringResource(Res.string.details_network_link_down)
                },
                status = linkStatus,
            )
        }

        ThroughputChannel(
            label = stringResource(Res.string.widget_network_rx),
            bytesPerSec = iface.rxBytesPerSec,
            peakBytesPerSec = iface.peakRxBytesPerSec,
            history = iface.rxHistory,
            color = AppTheme.colors.telemetryB,
            showHistory = expanded,
        )
        ThroughputChannel(
            label = stringResource(Res.string.widget_network_tx),
            bytesPerSec = iface.txBytesPerSec,
            peakBytesPerSec = iface.peakTxBytesPerSec,
            history = iface.txHistory,
            color = AppTheme.colors.telemetryA,
            showHistory = expanded,
        )

        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs)) {
                iface.linkUtilization?.let { utilization ->
                    LinearGauge(
                        progress = utilization,
                        label = stringResource(Res.string.details_network_link_utilization),
                        valueText = stringResource(Res.string.percentage_format, (utilization * 100).toInt()),
                        barHeight = AppTheme.spacing.space2xs,
                        normalColor = AppTheme.colors.telemetryB,
                    )
                }
                DetailsKeyValueRow(
                    label = stringResource(Res.string.details_network_field_link_speed),
                    value = if (iface.linkSpeedMbps != null && iface.linkSpeedMbps > 0) {
                        stringResource(Res.string.details_network_link_speed_format, iface.linkSpeedMbps)
                    } else {
                        stringResource(Res.string.details_network_unknown_speed)
                    },
                )
                DetailsKeyValueRow(
                    label = stringResource(Res.string.details_network_field_total_rx),
                    value = BitrateFormatter.formatDataBytes(iface.totalRxBytes),
                )
                DetailsKeyValueRow(
                    label = stringResource(Res.string.details_network_field_total_tx),
                    value = BitrateFormatter.formatDataBytes(iface.totalTxBytes),
                )
            }
        }
    }
}

@Composable
private fun ThroughputChannel(
    label: String,
    bytesPerSec: Long,
    peakBytesPerSec: Long,
    history: List<Float>,
    color: Color,
    showHistory: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
        ) {
            Box(
                modifier = Modifier
                    .size(AppTheme.spacing.spaceXs)
                    .background(color, AppTheme.shapes.full),
            )
            Text(
                text = label,
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
                modifier = Modifier.weight(1f),
            )
            if (showHistory && peakBytesPerSec > 0L) {
                Text(
                    text = stringResource(
                        Res.string.details_network_peak_format,
                        BitrateFormatter.formatBitrate(peakBytesPerSec),
                    ),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                )
            }
            Text(
                text = BitrateFormatter.formatBitrate(bytesPerSec),
                style = AppTheme.typography.codeBase,
                color = AppTheme.colors.textPrimary,
            )
        }
        if (showHistory && history.size >= 2 && history.any { it > 0f }) {
            MiniSparkline(
                data = history,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppTheme.dimensions.sparklineHeight),
                lineColor = color,
                minValue = 0f,
            )
        }
    }
}

@Preview
@Composable
private fun NetworkDetailsScreenPreview(
    @PreviewParameter(NetworkDetailsPreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        NetworkDetailsScreen(
            viewState = viewState,
            onClose = {},
        )
    }
}
