package io.buszi.nascope.ui.dashboard.widgets.sensors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import io.buszi.nascope.resources.widget_sensors_cooling_active
import io.buszi.nascope.resources.widget_sensors_cooling_active_compact
import io.buszi.nascope.resources.widget_sensors_cooling_label
import io.buszi.nascope.resources.details_sensors_fan_stalled
import io.buszi.nascope.resources.widget_sensors_no_fans
import io.buszi.nascope.resources.widget_sensors_no_sensors
import io.buszi.nascope.resources.widget_sensors_rpm_format
import io.buszi.nascope.resources.widget_sensors_status_critical
import io.buszi.nascope.resources.widget_sensors_status_elevated
import io.buszi.nascope.resources.widget_sensors_status_nominal
import io.buszi.nascope.resources.widget_sensors_subtitle
import io.buszi.nascope.resources.widget_sensors_temp_format
import io.buszi.nascope.resources.widget_sensors_temperatures_label
import io.buszi.nascope.resources.widget_sensors_title
import io.buszi.nascope.resources.widget_sensors_title_compact
import io.buszi.nascope.ui.common.components.badges.StatusChip
import io.buszi.nascope.ui.common.components.badges.StatusDot
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.components.badges.TagChip
import io.buszi.nascope.ui.common.components.badges.color
import io.buszi.nascope.ui.common.components.badges.haloColor
import io.buszi.nascope.ui.common.components.gauges.LinearGauge
import io.buszi.nascope.ui.common.components.metrics.MetricSize
import io.buszi.nascope.ui.common.components.metrics.MetricValue
import io.buszi.nascope.ui.common.model.FanSensorUi
import io.buszi.nascope.ui.common.model.TemperatureSensorUi
import io.buszi.nascope.ui.common.theme.AppTheme
import io.buszi.nascope.ui.dashboard.widgets.DashboardWidget
import io.buszi.nascope.ui.dashboard.widgets.sensors.SensorsThermalsViewModel.ViewAction
import io.buszi.nascope.ui.dashboard.widgets.sensors.SensorsThermalsViewModel.ViewState
import kotlin.math.abs
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SensorsThermalsWidget(
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<SensorsThermalsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    SensorsThermalsWidget(
        viewState = viewState,
        modifier = modifier,
        onViewAction = viewModel::onViewAction,
    )
}

@Composable
fun SensorsThermalsWidget(
    viewState: ViewState,
    modifier: Modifier = Modifier,
    onViewAction: (ViewAction) -> Unit = {},
) {
    val statusLabel = when (viewState.sensors.overallStatus) {
        StatusLevel.Operational -> stringResource(Res.string.widget_sensors_status_nominal)
        StatusLevel.Degraded -> stringResource(Res.string.widget_sensors_status_elevated)
        StatusLevel.Critical -> stringResource(Res.string.widget_sensors_status_critical)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val containerWidth = maxWidth
        val isWide = containerWidth >= 330.dp

        val cpuTempFormatted = viewState.sensors.cpuHeadline?.let {
            stringResource(Res.string.widget_sensors_temp_format, formatDecimal(it.currentTempC))
        } ?: "--"

        val subtitleText = stringResource(
            Res.string.widget_sensors_subtitle,
            cpuTempFormatted,
            viewState.sensors.drives.size,
        )

        val headerActionText = when {
            viewState.sensors.fans.isEmpty() -> stringResource(Res.string.widget_sensors_no_fans)
            isWide -> stringResource(Res.string.widget_sensors_cooling_active, viewState.sensors.activeFansCount)
            else -> stringResource(Res.string.widget_sensors_cooling_active_compact, viewState.sensors.activeFansCount)
        }

        DashboardWidget(
            title = if (isWide) {
                stringResource(Res.string.widget_sensors_title)
            } else {
                stringResource(Res.string.widget_sensors_title_compact)
            },
            subtitle = subtitleText,
            modifier = Modifier.fillMaxSize(),
            state = viewState.widgetState,
            statusTag = {
                StatusChip(
                    text = statusLabel,
                    status = viewState.sensors.overallStatus,
                    pulsing = viewState.sensors.overallStatus == StatusLevel.Critical,
                )
            },
            headerAction = {
                TagChip(text = headerActionText)
            },
        ) {
            val sensors = viewState.sensors

            if (sensors.cpuHeadline == null && sensors.drives.isEmpty() && sensors.fans.isEmpty()) {
                EmptySensorsPlaceholder()
            } else {
                SensorsContent(
                    viewState = viewState,
                    isWide = isWide,
                )
            }
        }
    }
}

@Composable
private fun SensorsContent(
    viewState: ViewState,
    isWide: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceMd),
    ) {
        // Section 1: CPU Package Temperature
        viewState.sensors.cpuHeadline?.let { cpu ->
            CpuTemperatureCard(cpu = cpu, isWide = isWide)
        }

        // Section 2: Drive Temperatures (gracefully hidden if empty)
        if (viewState.sensors.drives.isNotEmpty()) {
            DriveTemperaturesSection(
                drives = viewState.sensors.drives,
            )
        }

        // Section 3: Cooling Fans (gracefully hidden if empty)
        if (viewState.sensors.fans.isNotEmpty()) {
            CoolingFansSection(
                fans = viewState.sensors.fans,
                isWide = isWide,
            )
        }
    }
}

@Composable
private fun CpuTemperatureCard(
    cpu: TemperatureSensorUi,
    isWide: Boolean,
) {
    val tempStatus = cpu.status

    // Gauge range: 30°C to 100°C
    val gaugeRange = 30f..100f
    val progress = cpu.progressIn(gaugeRange)
    val warningThreshold = cpu.warningProgressIn(gaugeRange)
    val criticalThreshold = cpu.criticalProgressIn(gaugeRange)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.base,
            )
            .padding(AppTheme.spacing.spaceMd),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(tempStatus.color, AppTheme.shapes.full),
                    )
                    Text(
                        text = cpu.label,
                        style = AppTheme.typography.labelUi,
                        color = AppTheme.colors.textSecondary,
                    )
                }

                StatusChip(
                    text = "${cpu.currentTempC.roundToInt()}°C",
                    status = tempStatus,
                    showDot = false,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                MetricValue(
                    value = formatDecimal(cpu.currentTempC),
                    unit = "°C",
                    size = MetricSize.Medium,
                    valueColor = tempStatus.color,
                )

                Text(
                    text = "WARN: ${cpu.warningThresholdC.roundToInt()}°C  CRIT: ${cpu.criticalThresholdC.roundToInt()}°C",
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textMuted,
                )
            }

            LinearGauge(
                progress = progress,
                warningThreshold = warningThreshold,
                criticalThreshold = criticalThreshold,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DriveTemperaturesSection(
    drives: List<TemperatureSensorUi>,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        Text(
            text = stringResource(Res.string.widget_sensors_temperatures_label),
            style = AppTheme.typography.labelUi,
            color = AppTheme.colors.textSecondary,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            drives.forEach { drive ->
                DriveTempCard(
                    drive = drive,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DriveTempCard(
    drive: TemperatureSensorUi,
    modifier: Modifier = Modifier,
) {
    val tempStatus = drive.status

    // Gauge range: 20°C to 70°C
    val gaugeRange = 20f..70f
    val progress = drive.progressIn(gaugeRange)
    val warnThreshold = drive.warningProgressIn(gaugeRange)
    val critThreshold = drive.criticalProgressIn(gaugeRange)

    Box(
        modifier = modifier
            .clip(AppTheme.shapes.base)
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.base,
            )
            .padding(AppTheme.spacing.spaceSm),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = drive.label.substringBefore(" "),
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                StatusDot(
                    color = tempStatus.color,
                    haloColor = if (tempStatus != StatusLevel.Operational) tempStatus.haloColor else Color.Transparent,
                )
            }

            Text(
                text = "${drive.currentTempC.roundToInt()}°C",
                style = AppTheme.typography.bodySm,
                color = tempStatus.color,
            )

            LinearGauge(
                progress = progress,
                warningThreshold = warnThreshold,
                criticalThreshold = critThreshold,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CoolingFansSection(
    fans: List<FanSensorUi>,
    isWide: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceXs),
    ) {
        Text(
            text = stringResource(Res.string.widget_sensors_cooling_label),
            style = AppTheme.typography.labelUi,
            color = AppTheme.colors.textSecondary,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.spaceSm),
        ) {
            fans.forEach { fan ->
                FanCard(
                    fan = fan,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FanCard(
    fan: FanSensorUi,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(AppTheme.shapes.base)
            .background(AppTheme.colors.inputBackground, AppTheme.shapes.base)
            .border(
                width = AppTheme.dimensions.hairlineBorder,
                color = AppTheme.colors.baseBorder,
                shape = AppTheme.shapes.base,
            )
            .padding(AppTheme.spacing.spaceSm),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.space2xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = fan.label,
                    style = AppTheme.typography.labelCode,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }

            Text(
                text = if (fan.isStalled) {
                    stringResource(Res.string.details_sensors_fan_stalled)
                } else {
                    stringResource(Res.string.widget_sensors_rpm_format, fan.rpm)
                },
                style = AppTheme.typography.bodySm,
                color = if (fan.isStalled) AppTheme.colors.critical else AppTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun EmptySensorsPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.spacing.spaceXl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.widget_sensors_no_sensors),
            style = AppTheme.typography.bodySm,
            color = AppTheme.colors.textMuted,
        )
    }
}

internal fun formatDecimal(value: Float): String {
    val rounded = (value * 10).roundToInt()
    val whole = rounded / 10
    val fraction = abs(rounded % 10)
    return "$whole.$fraction"
}

@Preview
@Composable
private fun SensorsThermalsWidgetPreview(
    @PreviewParameter(SensorsThermalsPreviewParameterProvider::class) viewState: ViewState,
) {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SensorsThermalsWidget(viewState = viewState)
        }
    }
}
