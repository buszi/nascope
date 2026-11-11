package io.buszi.nascope.ui.dashboard.widgets.sensors

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.model.FanSensorUi
import io.buszi.nascope.ui.common.model.TemperatureSensorUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.sensors.SensorsDetailsViewModel.ViewState

class SensorsDetailsPreviewParameterProvider : PreviewParameterProvider<ViewState> {
    override val values: Sequence<ViewState> = sequenceOf(
        // 1. Nominal
        ViewState(
            cpuTemperature = TemperatureSensorUi(
                label = "CPU Package",
                currentTempC = 58f,
                warningThresholdC = 75f,
                criticalThresholdC = 85f,
                history = listOf(55f, 57f, 60f, 61f, 59f, 56f, 55f, 58f, 61f, 59f),
            ),
            driveTemperatures = listOf(
                TemperatureSensorUi(
                    label = "nvme0n1 (NVMe)",
                    currentTempC = 41f,
                    warningThresholdC = 65f,
                    criticalThresholdC = 75f,
                    history = listOf(39f, 41f, 43f, 42f, 40f, 39f, 41f),
                ),
            ),
            fans = listOf(
                FanSensorUi(
                    label = "CPU Fan",
                    rpm = 1380,
                    maxRpm = 2200,
                    rpmHistory = listOf(1290f, 1380f, 1450f, 1400f, 1320f, 1380f),
                ),
                FanSensorUi(label = "Chassis 1", rpm = 920, maxRpm = 1800),
            ),
            widgetState = WidgetState.Ready,
        ),

        // 2. Hot CPU, stalled fan
        ViewState(
            cpuTemperature = TemperatureSensorUi(
                label = "CPU Package",
                currentTempC = 88f,
                warningThresholdC = 75f,
                criticalThresholdC = 85f,
                history = listOf(70f, 74f, 79f, 83f, 86f, 88f, 89f, 88f),
            ),
            fans = listOf(
                FanSensorUi(label = "CPU Fan", rpm = 0, maxRpm = 2200, rpmHistory = listOf(1800f, 1200f, 600f, 0f, 0f)),
            ),
            widgetState = WidgetState.Ready,
        ),

        // 3. No sensors
        ViewState(widgetState = WidgetState.Ready),

        // 4. Loading
        ViewState(widgetState = WidgetState.Loading),
    )
}
