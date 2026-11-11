package io.buszi.nascope.ui.dashboard.widgets.network

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.widget_network_active_count
import io.buszi.nascope.resources.widget_network_active_count_compact
import io.buszi.nascope.resources.widget_network_interfaces_label
import io.buszi.nascope.resources.widget_network_no_interfaces
import io.buszi.nascope.resources.widget_network_rx
import io.buszi.nascope.resources.widget_network_rx_compact
import io.buszi.nascope.resources.widget_network_status_degraded
import io.buszi.nascope.resources.widget_network_status_offline
import io.buszi.nascope.resources.widget_network_status_online
import io.buszi.nascope.resources.widget_network_throughput_label
import io.buszi.nascope.resources.widget_network_title
import io.buszi.nascope.resources.widget_network_title_compact
import io.buszi.nascope.resources.widget_network_total_format
import io.buszi.nascope.resources.widget_network_total_transfer
import io.buszi.nascope.resources.widget_network_tx
import io.buszi.nascope.resources.widget_network_tx_compact
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusDot
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.gauges.MiniSparkline
import io.buszi.nascope.ui.common.components.metrics.MetricSize
import io.buszi.nascope.ui.common.components.metrics.MetricValue
import io.buszi.nascope.ui.common.model.NetworkInterfaceUi
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.widgets.DashboardWidget
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkIoViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkIoViewModel.ViewAction.OnSelectInterface
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkIoViewModel.ViewState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NetworkIoWidget(
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<NetworkIoViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    NetworkIoWidget(
        viewState = viewState,
        modifier = modifier,
        onViewAction = viewModel::onViewAction,
    )
}

@Composable
fun NetworkIoWidget(
    viewState: ViewState,
    modifier: Modifier = Modifier,
    onViewAction: (ViewAction) -> Unit = {},
) {
    val statusLabel = when (viewState.overallStatus) {
        StatusLevel.Operational -> stringResource(Res.string.widget_network_status_online)
        StatusLevel.Degraded -> stringResource(Res.string.widget_network_status_degraded)
        StatusLevel.Critical -> stringResource(Res.string.widget_network_status_offline)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val containerWidth = maxWidth
        val isWide = containerWidth >= 330.dp
        val isFixedHeight = constraints.hasBoundedHeight

        val aggregateRxFormatted = BitrateFormatter.formatBitrate(viewState.aggregateRxBytesPerSec)
        val aggregateTxFormatted = BitrateFormatter.formatBitrate(viewState.aggregateTxBytesPerSec)

        val subtitleText = stringResource(
            Res.string.widget_network_total_transfer,
            aggregateRxFormatted,
            aggregateTxFormatted,
        )

        val headerActionText = if (isWide) {
            stringResource(Res.string.widget_network_active_count, viewState.activeInterfacesCount)
        } else {
            stringResource(Res.string.widget_network_active_count_compact, viewState.activeInterfacesCount)
        }

        DashboardWidget(
            title = if (isWide) {
                stringResource(Res.string.widget_network_title)
            } else {
                stringResource(Res.string.widget_network_title_compact)
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
                TagChip(text = headerActionText)
            },
        ) {
            if (viewState.interfaces.isEmpty()) {
                EmptyInterfacesPlaceholder()
            } else {
                val currentInterface = viewState.selectedInterface ?: viewState.interfaces.first()

                NetworkWidgetContent(
                    selectedInterface = currentInterface,
                    interfaces = viewState.interfaces,
                    isWide = isWide,
                    showHistory = !isFixedHeight,
                    onSelectInterface = { onViewAction(OnSelectInterface(it)) },
                )
            }
        }
    }
}

@Composable
private fun NetworkWidgetContent(
    selectedInterface: NetworkInterfaceUi,
    interfaces: List<NetworkInterfaceUi>,
    isWide: Boolean,
    showHistory: Boolean,
    onSelectInterface: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
    ) {
        // Section 1: Throughput Rate Panels (RX & TX)
        ThroughputRatePanels(
            rxBytesPerSec = selectedInterface.rxBytesPerSec,
            txBytesPerSec = selectedInterface.txBytesPerSec,
            totalRxBytes = selectedInterface.totalRxBytes,
            totalTxBytes = selectedInterface.totalTxBytes,
            isWide = isWide,
        )

        // Section 2: Throughput History Sparkline
        if (showHistory) {
            ThroughputSparklineSection(
                rxHistory = selectedInterface.rxHistory,
                txHistory = selectedInterface.txHistory,
            )
        }

        // Section 3: Interface Selector
        InterfacesSelectorSection(
            interfaces = interfaces,
            selectedInterfaceName = selectedInterface.name,
            onSelectInterface = onSelectInterface,
        )
    }
}

@Composable
private fun ThroughputRatePanels(
    rxBytesPerSec: Long,
    txBytesPerSec: Long,
    totalRxBytes: Long,
    totalTxBytes: Long,
    isWide: Boolean,
) {
    val (rxVal, rxUnit) = BitrateFormatter.formatBitrateParts(rxBytesPerSec)
    val (txVal, txUnit) = BitrateFormatter.formatBitrateParts(txBytesPerSec)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        // RX Card (Download - Series B Cyan)
        ThroughputCard(
            modifier = Modifier.weight(1f),
            label = if (isWide) {
                stringResource(Res.string.widget_network_rx)
            } else {
                stringResource(Res.string.widget_network_rx_compact)
            },
            value = rxVal,
            unit = rxUnit,
            totalText = stringResource(Res.string.widget_network_total_format, BitrateFormatter.formatDataBytes(totalRxBytes)),
            accentColor = AppTheme.colors.telemetryB,
        )

        // TX Card (Upload - Series A Electric Violet)
        ThroughputCard(
            modifier = Modifier.weight(1f),
            label = if (isWide) {
                stringResource(Res.string.widget_network_tx)
            } else {
                stringResource(Res.string.widget_network_tx_compact)
            },
            value = txVal,
            unit = txUnit,
            totalText = stringResource(Res.string.widget_network_total_format, BitrateFormatter.formatDataBytes(totalTxBytes)),
            accentColor = AppTheme.colors.telemetryA,
        )
    }
}

@Composable
private fun ThroughputCard(
    label: String,
    value: String,
    unit: String,
    totalText: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.base,
            )
            .padding(AppTheme.spacing.spaceMd),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
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
                    style = AppTheme.typography.labelUi,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            MetricValue(
                value = value,
                unit = unit,
                size = MetricSize.Medium,
            )

            Text(
                text = totalText,
                style = AppTheme.typography.bodySm,
                color = AppTheme.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ThroughputSparklineSection(
    rxHistory: List<Float>,
    txHistory: List<Float>,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.widget_network_throughput_label),
                style = AppTheme.typography.labelUi,
                color = AppTheme.colors.textSecondary,
            )

            // Sparkline legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(AppTheme.colors.telemetryB, AppTheme.shapes.full),
                    )
                    Text(
                        text = "RX",
                        style = AppTheme.typography.labelCode,
                        color = AppTheme.colors.textMuted,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(AppTheme.colors.telemetryA, AppTheme.shapes.full),
                    )
                    Text(
                        text = "TX",
                        style = AppTheme.typography.labelCode,
                        color = AppTheme.colors.textMuted,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
                .border(
                    width = AppTheme.dimensions.hairlineBorder,
                    color = AppTheme.colors.baseBorder,
                    shape = AppTheme.shapes.base,
                )
                .padding(vertical = AppTheme.spacing.space2xs, horizontal = AppTheme.spacing.spaceXs),
        ) {
            val maxSample = maxOf(
                rxHistory.maxOrNull() ?: 1f,
                txHistory.maxOrNull() ?: 1f,
                1f,
            )

            // Layered Dual Sparkline
            MiniSparkline(
                data = rxHistory,
                lineColor = AppTheme.colors.telemetryB,
                minValue = 0f,
                maxValue = maxSample,
                modifier = Modifier.fillMaxWidth(),
            )
            MiniSparkline(
                data = txHistory,
                lineColor = AppTheme.colors.telemetryA,
                minValue = 0f,
                maxValue = maxSample,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun InterfacesSelectorSection(
    interfaces: List<NetworkInterfaceUi>,
    selectedInterfaceName: String,
    onSelectInterface: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        Text(
            text = stringResource(Res.string.widget_network_interfaces_label),
            style = AppTheme.typography.labelUi,
            color = AppTheme.colors.textSecondary,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            interfaces.forEach { iface ->
                val isSelected = iface.name == selectedInterfaceName
                InterfaceCard(
                    iface = iface,
                    isSelected = isSelected,
                    onClick = { onSelectInterface(iface.name) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun InterfaceCard(
    iface: NetworkInterfaceUi,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isSelected) {
        AppTheme.colors.telemetryB
    } else {
        AppTheme.colors.baseBorder
    }

    val backgroundColor = if (isSelected) {
        AppTheme.colors.inputBackground
    } else {
        AppTheme.colors.canvasBackground
    }

    Box(
        modifier = modifier
            .clip(AppTheme.shapes.base)
            .background(backgroundColor, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = borderColor,
                shape = AppTheme.shapes.base,
            )
            .clickable(onClick = onClick)
            .padding(AppTheme.spacing.spaceSm),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = iface.name,
                    style = AppTheme.typography.labelCode,
                    color = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
                )

                StatusDot(
                    color = if (iface.isUp) AppTheme.colors.operational else AppTheme.colors.textDisabled,
                    haloColor = if (iface.isUp) AppTheme.colors.operationalHalo else Color.Transparent,
                )
            }

            Text(
                text = iface.ipAddress,
                style = AppTheme.typography.bodySm,
                color = AppTheme.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyInterfacesPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.spacing.spaceXl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.widget_network_no_interfaces),
            style = AppTheme.typography.bodySm,
            color = AppTheme.colors.textMuted,
        )
    }
}

@Preview
@Composable
private fun NetworkIoWidgetPreview(
    @PreviewParameter(NetworkIoPreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            NetworkIoWidget(viewState = viewState)
        }
    }
}
