package io.buszi.nascope.ui.dashboard.widgets.system

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.gauges.LinearGauge
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.widgets.DashboardWidget
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.widget_system_overview_title
import io.buszi.nascope.ui.dashboard.widgets.WidgetState.Ready
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewViewModel.ViewState
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
    onViewAction: (ViewAction) -> Unit,
) {
    DashboardWidget(
        title = stringResource(Res.string.widget_system_overview_title),
        subtitle = viewState.osName,
        modifier = modifier,
        state = viewState.widgetState,
        statusTag = if (viewState.widgetState.isReady) {
            {
                StatusChip(
                    text = "HEALTHY",
                    status = StatusLevel.Operational,
                )
            }
        } else null,
        headerAction = if (viewState.widgetState.isReady) {
            {
                TagChip(text = "UP ${viewState.uptime}")
            }
        } else null,
    ) {
        if (viewState.widgetState.isReady) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 360.dp

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = viewState.hostname,
                    style = AppTheme.typography.headlineSm,
                    color = AppTheme.colors.textPrimary,
                )
                Text(
                    text = viewState.kernelRelease,
                    style = AppTheme.typography.codeBase,
                    color = AppTheme.colors.textMuted,
                )
            }
            TagChip(text = "${viewState.cpuCores} CORES")
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.hairlineBorder)
                .background(AppTheme.colors.subtleDivider),
        )

        LoadAveragesRow(
            load1m = viewState.load1m,
            load5m = viewState.load5m,
            load15m = viewState.load15m,
            cpuCores = viewState.cpuCores,
        )
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
        Text(
            text = viewState.hostname,
            style = AppTheme.typography.headlineSm,
            color = AppTheme.colors.textPrimary,
        )
        Text(
            text = viewState.kernelRelease,
            style = AppTheme.typography.codeBase,
            color = AppTheme.colors.textMuted,
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.space2xs))

        LoadAveragesRow(
            load1m = viewState.load1m,
            load5m = viewState.load5m,
            load15m = viewState.load15m,
            cpuCores = viewState.cpuCores,
        )
    }
}

@Composable
private fun LoadAveragesRow(
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
        Text(
            text = "LOAD AVERAGES",
            style = AppTheme.typography.labelUi,
            color = AppTheme.colors.textMuted,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            LoadCell(
                label = "1M",
                value = load1m,
                cpuCores = cpuCores,
                modifier = Modifier.weight(1f)
            )
            LoadCell(
                label = "5M",
                value = load5m,
                cpuCores = cpuCores,
                modifier = Modifier.weight(1f)
            )
            LoadCell(
                label = "15M",
                value = load15m,
                cpuCores = cpuCores,
                modifier = Modifier.weight(1f)
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
    val ratio = if (cpuCores > 0) value / cpuCores else value
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
        Text(
            text = label,
            style = AppTheme.typography.labelCode,
            color = AppTheme.colors.textMuted,
        )
        Text(
            text = value.toString(),
            style = AppTheme.typography.metricMd,
            color = AppTheme.colors.textPrimary,
        )
        LinearGauge(
            progress = ratio,
            barHeight = AppTheme.spacing.space2xs,
        )
    }
}

@Preview
@Composable
private fun SystemOverviewWidgetPreview() {
    AppTheme {
        SystemOverviewWidget(
            viewState = ViewState(
                hostname = "alpha-node-01.local",
                osName = "Ubuntu 24.04.1 LTS",
                kernelRelease = "Linux 6.8.0-45-generic",
                uptime = "14d 06h 32m",
                load1m = 0.42f,
                load5m = 0.58f,
                load15m = 0.71f,
                cpuCores = 8,
                widgetState = Ready,
            ),
            onViewAction = {},
        )
    }
}
