package io.buszi.nascope.ui.dashboard.widgets.sensors

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.data.telemetry.FanSensorData
import io.buszi.nascope.data.telemetry.TemperatureSensorData
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.sensors.SensorsThermalsViewModel.ViewState

class SensorsThermalsPreviewParameterProvider : PreviewParameterProvider<ViewState> {
    override val values: Sequence<ViewState> = sequenceOf(
        // 1. Nominal / Healthy
        ViewState(
            overallStatus = StatusLevel.Operational,
            cpuTemperature = TemperatureSensorData(
                label = "CPU Package",
                currentTempC = 58.0f,
                warningThresholdC = 75.0f,
                criticalThresholdC = 85.0f,
            ),
            driveTemperatures = listOf(
                TemperatureSensorData("nvme0n1 (NVMe)", 41.0f, 65.0f, 75.0f),
                TemperatureSensorData("sda (WD Red 8TB)", 34.0f, 50.0f, 60.0f),
                TemperatureSensorData("sdb (WD Red 8TB)", 36.0f, 50.0f, 60.0f),
            ),
            fans = listOf(
                FanSensorData("CPU Fan", 1380, 2200),
                FanSensorData("Chassis 1", 920, 1800),
                FanSensorData("Chassis 2", 890, 1800),
            ),
            activeFansCount = 3,
            maxTemperatureC = 58.0f,
            widgetState = WidgetState.Ready,
        ),

        // 2. Elevated / Warning
        ViewState(
            overallStatus = StatusLevel.Degraded,
            cpuTemperature = TemperatureSensorData(
                label = "CPU Package",
                currentTempC = 78.0f,
                warningThresholdC = 75.0f,
                criticalThresholdC = 85.0f,
            ),
            driveTemperatures = listOf(
                TemperatureSensorData("nvme0n1 (NVMe)", 48.0f, 65.0f, 75.0f),
            ),
            fans = listOf(
                FanSensorData("CPU Fan", 2100, 2200),
            ),
            activeFansCount = 1,
            maxTemperatureC = 78.0f,
            widgetState = WidgetState.Ready,
        ),

        // 3. Passive Cooling / Unsupported Sensors (Graceful fallback)
        ViewState(
            overallStatus = StatusLevel.Operational,
            cpuTemperature = TemperatureSensorData(
                label = "CPU Package",
                currentTempC = 45.0f,
                warningThresholdC = 75.0f,
                criticalThresholdC = 85.0f,
            ),
            driveTemperatures = emptyList(),
            fans = emptyList(),
            activeFansCount = 0,
            maxTemperatureC = 45.0f,
            widgetState = WidgetState.Ready,
        ),

        // 4. Loading State
        ViewState(
            widgetState = WidgetState.Loading,
        ),
    )
}
