package io.buszi.nascope.ui.dashboard.widgets.sensors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.details_sensors_cooling
import io.buszi.nascope.resources.details_sensors_fan_max_format
import io.buszi.nascope.resources.details_sensors_fan_stalled
import io.buszi.nascope.resources.details_sensors_fans_active_format
import io.buszi.nascope.resources.details_sensors_headroom_critical
import io.buszi.nascope.resources.details_sensors_headroom_format
import io.buszi.nascope.resources.details_sensors_headroom_warning
import io.buszi.nascope.resources.details_sensors_overview
import io.buszi.nascope.resources.details_sensors_stat_at_risk
import io.buszi.nascope.resources.details_sensors_stat_avg
import io.buszi.nascope.resources.details_sensors_stat_fan_duty
import io.buszi.nascope.resources.details_sensors_stat_fans_active
import io.buszi.nascope.resources.details_sensors_stat_hottest
import io.buszi.nascope.resources.details_sensors_stat_max
import io.buszi.nascope.resources.details_sensors_stat_min
import io.buszi.nascope.resources.details_sensors_subtitle
import io.buszi.nascope.resources.details_sensors_temperatures
import io.buszi.nascope.resources.details_sensors_title
import io.buszi.nascope.resources.percentage_format
import io.buszi.nascope.resources.widget_sensors_no_sensors
import io.buszi.nascope.resources.widget_sensors_rpm_format
import io.buszi.nascope.resources.widget_sensors_status_critical
import io.buszi.nascope.resources.widget_sensors_status_elevated
import io.buszi.nascope.resources.widget_sensors_status_nominal
import io.buszi.nascope.resources.widget_sensors_temp_format
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.color
import io.buszi.nascope.ui.common.components.gauges.LinearGauge
import io.buszi.nascope.ui.common.components.gauges.MiniSparkline
import io.buszi.nascope.ui.common.model.FanSensorUi
import io.buszi.nascope.ui.common.model.TemperatureSensorUi
import io.buszi.nascope.ui.common.testing.TestTags
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import io.buszi.nascope.ui.dashboard.widgets.sensors.SensorsDetailsViewModel.ViewState
import io.buszi.nascope.ui.details.components.DetailsLoadingPlaceholder
import io.buszi.nascope.ui.details.components.DetailsScaffold
import io.buszi.nascope.ui.details.components.DetailsSection
import io.buszi.nascope.ui.details.components.DetailsStat
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val CpuGaugeRange = 30f..100f
private val DriveGaugeRange = 20f..70f

@Composable
fun SensorsDetailsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<SensorsDetailsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    SensorsDetailsScreen(
        viewState = viewState,
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
fun SensorsDetailsScreen(
    viewState: ViewState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailsScaffold(
        title = stringResource(Res.string.details_sensors_title),
        onClose = onClose,
        testTag = TestTags.details(DashboardBrickId.SensorsThermals),
        modifier = modifier,
        subtitle = if (viewState.widgetState.isReady) {
            stringResource(
                Res.string.details_sensors_subtitle,
                viewState.temperatures.size,
                viewState.fans.size,
            )
        } else {
            null
        },
        statusTag = if (viewState.widgetState.isReady) {
            {
                StatusChip(
                    text = when (viewState.overallStatus) {
                        StatusLevel.Operational -> stringResource(Res.string.widget_sensors_status_nominal)
                        StatusLevel.Degraded -> stringResource(Res.string.widget_sensors_status_elevated)
                        StatusLevel.Critical -> stringResource(Res.string.widget_sensors_status_critical)
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
            DetailsLoadingPlaceholder()
        } else if (viewState.temperatures.isEmpty() && viewState.fans.isEmpty()) {
            Text(
                text = stringResource(Res.string.widget_sensors_no_sensors),
                style = AppTheme.typography.labelCode,
                color = AppTheme.colors.textMuted,
            )
        } else {
            OverviewSection(viewState = viewState)
            if (viewState.temperatures.isNotEmpty()) {
                TemperaturesSection(viewState = viewState)
            }
            if (viewState.fans.isNotEmpty()) {
                CoolingSection(viewState = viewState)
            }
        }
    }
}

@Composable
private fun OverviewSection(
    viewState: ViewState,
    modifier: Modifier = Modifier,
) {
    val hottest = viewState.hottest

    DetailsSection(
        title = stringResource(Res.string.details_sensors_overview),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                if (hottest != null) {
                    DetailsStat(
                        label = stringResource(Res.string.details_sensors_stat_hottest),
                        value = stringResource(Res.string.widget_sensors_temp_format, formatDecimal(hottest.currentTempC)),
                        valueColor = hottest.status.valueColor(),
                        modifier = Modifier.weight(1f),
                    )
                    DetailsStat(
                        label = stringResource(Res.string.details_sensors_stat_at_risk),
                        value = viewState.atRiskCount.toString(),
                        valueColor = if (viewState.atRiskCount > 0) AppTheme.colors.degraded else AppTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
            ) {
                if (viewState.fans.isNotEmpty()) {
                    DetailsStat(
                        label = stringResource(Res.string.details_sensors_stat_fans_active),
                        value = stringResource(
                            Res.string.details_sensors_fans_active_format,
                            viewState.activeFansCount,
                            viewState.fans.size,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    DetailsStat(
                        label = stringResource(Res.string.details_sensors_stat_fan_duty),
                        value = stringResource(Res.string.percentage_format, viewState.averageFanDutyPercent.toInt()),
                        valueColor = AppTheme.colors.telemetryB,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TemperaturesSection(
    viewState: ViewState,
    modifier: Modifier = Modifier,
) {
    DetailsSection(
        title = stringResource(Res.string.details_sensors_temperatures),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
            viewState.cpuTemperature?.let { cpu ->
                TemperatureCard(sensor = cpu, gaugeRange = CpuGaugeRange)
            }
            viewState.driveTemperatures.forEach { drive ->
                TemperatureCard(sensor = drive, gaugeRange = DriveGaugeRange)
            }
        }
    }
}

@Composable
private fun TemperatureCard(
    sensor: TemperatureSensorUi,
    gaugeRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    val status = sensor.status

    SensorCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            Text(
                text = sensor.label,
                style = AppTheme.typography.codeBase,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(Res.string.widget_sensors_temp_format, formatDecimal(sensor.currentTempC)),
                style = AppTheme.typography.codeBase,
                color = status.valueColor(),
            )
        }

        LinearGauge(
            progress = sensor.progressIn(gaugeRange),
            barHeight = AppTheme.spacing.space2xs,
            warningThreshold = sensor.warningProgressIn(gaugeRange),
            criticalThreshold = sensor.criticalProgressIn(gaugeRange),
            normalColor = AppTheme.colors.telemetryB,
        )

        if (sensor.history.size >= 2) {
            MiniSparkline(
                data = sensor.history,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppTheme.dimensions.sparklineHeight),
                lineColor = if (status == StatusLevel.Operational) AppTheme.colors.telemetryB else status.color,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            DetailsStat(
                label = stringResource(Res.string.details_sensors_stat_min),
                value = formatDecimal(sensor.minHistoryC),
                modifier = Modifier.weight(1f),
            )
            DetailsStat(
                label = stringResource(Res.string.details_sensors_stat_avg),
                value = formatDecimal(sensor.averageHistoryC),
                modifier = Modifier.weight(1f),
            )
            DetailsStat(
                label = stringResource(Res.string.details_sensors_stat_max),
                value = formatDecimal(sensor.maxHistoryC),
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            DetailsStat(
                label = stringResource(Res.string.details_sensors_headroom_warning),
                value = stringResource(Res.string.details_sensors_headroom_format, formatDecimal(sensor.headroomToWarningC)),
                valueColor = if (sensor.headroomToWarningC <= 0f) AppTheme.colors.degraded else AppTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            DetailsStat(
                label = stringResource(Res.string.details_sensors_headroom_critical),
                value = stringResource(Res.string.details_sensors_headroom_format, formatDecimal(sensor.headroomToCriticalC)),
                valueColor = if (sensor.headroomToCriticalC <= 0f) AppTheme.colors.critical else AppTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CoolingSection(
    viewState: ViewState,
    modifier: Modifier = Modifier,
) {
    DetailsSection(
        title = stringResource(Res.string.details_sensors_cooling),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm)) {
            viewState.fans.forEach { fan ->
                FanCard(fan = fan)
            }
        }
    }
}

@Composable
private fun FanCard(
    fan: FanSensorUi,
    modifier: Modifier = Modifier,
) {
    SensorCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            Text(
                text = fan.label,
                style = AppTheme.typography.codeBase,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (fan.isStalled) {
                    stringResource(Res.string.details_sensors_fan_stalled)
                } else {
                    stringResource(Res.string.widget_sensors_rpm_format, fan.rpm)
                },
                style = AppTheme.typography.codeBase,
                color = if (fan.isStalled) AppTheme.colors.critical else AppTheme.colors.textPrimary,
            )
        }

        LinearGauge(
            progress = fan.dutyCyclePercent,
            label = stringResource(Res.string.details_sensors_fan_max_format, fan.maxRpm),
            valueText = stringResource(Res.string.percentage_format, (fan.dutyCyclePercent * 100).toInt()),
            barHeight = AppTheme.spacing.space2xs,
            normalColor = AppTheme.colors.telemetryB,
            warningColor = AppTheme.colors.telemetryB,
            criticalColor = AppTheme.colors.telemetryB,
        )

        if (fan.rpmHistory.size >= 2) {
            MiniSparkline(
                data = fan.rpmHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppTheme.dimensions.sparklineHeight),
                lineColor = AppTheme.colors.telemetryA,
                minValue = 0f,
                maxValue = fan.maxRpm.toFloat(),
            )
        }
    }
}

@Composable
private fun SensorCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.base,
            )
            .padding(AppTheme.spacing.spaceSm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
    ) {
        content()
    }
}

@Composable
private fun StatusLevel.valueColor(): Color = when (this) {
    StatusLevel.Operational -> AppTheme.colors.textPrimary
    StatusLevel.Degraded, StatusLevel.Critical -> color
}

@Preview
@Composable
private fun SensorsDetailsScreenPreview(
    @PreviewParameter(SensorsDetailsPreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        SensorsDetailsScreen(
            viewState = viewState,
            onClose = {},
        )
    }
}
