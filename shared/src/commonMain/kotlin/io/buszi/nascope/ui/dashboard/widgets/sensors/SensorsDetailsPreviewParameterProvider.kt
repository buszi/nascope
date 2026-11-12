package io.buszi.nascope.ui.dashboard.widgets.sensors

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.model.FanSensorUi
import io.buszi.nascope.ui.common.model.SensorsUi
import io.buszi.nascope.ui.common.model.TemperatureSensorUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.sensors.SensorsDetailsViewModel.ViewState

class SensorsDetailsPreviewParameterProvider : PreviewParameterProvider<ViewState> {
    override val values: Sequence<ViewState> = sequenceOf(
        // 1. NAS-like: package and cores, NVMe drives, ACPI under "other", no fans
        ViewState(
            sensors = SensorsUi(
                cpu = listOf(
                    TemperatureSensorUi(
                        label = "Package id 0",
                        currentTempC = 58f,
                        warningThresholdC = 70f,
                        criticalThresholdC = 85f,
                        history = listOf(55f, 57f, 60f, 61f, 59f, 56f, 55f, 58f, 61f, 59f),
                    ),
                    TemperatureSensorUi("Core 0", 56f, 70f, 85f),
                ),
                drives = listOf(
                    TemperatureSensorUi(
                        label = "Composite",
                        currentTempC = 41f,
                        warningThresholdC = 70f,
                        criticalThresholdC = 85f,
                        history = listOf(39f, 41f, 43f, 42f, 40f, 39f, 41f),
                    ),
                ),
                other = listOf(TemperatureSensorUi("acpitz 0", 27f, 70f, 85f)),
            ),
            widgetState = WidgetState.Ready,
        ),

        // 2. Hot CPU, stalled fan
        ViewState(
            sensors = SensorsUi(
                cpu = listOf(
                    TemperatureSensorUi(
                        label = "Package id 0",
                        currentTempC = 88f,
                        warningThresholdC = 75f,
                        criticalThresholdC = 85f,
                        history = listOf(70f, 74f, 79f, 83f, 86f, 88f, 89f, 88f),
                    ),
                ),
                fans = listOf(
                    FanSensorUi(label = "CPU Fan", rpm = 0, rpmHistory = listOf(1800f, 1200f, 600f, 0f, 0f)),
                    FanSensorUi(label = "Chassis 1", rpm = 920, rpmHistory = listOf(900f, 920f, 910f)),
                ),
            ),
            widgetState = WidgetState.Ready,
        ),

        // 3. No sensors
        ViewState(widgetState = WidgetState.Ready),

        // 4. Stale
        ViewState(
            sensors = SensorsUi(cpu = listOf(TemperatureSensorUi("Package id 0", 58f, 70f, 85f))),
            widgetState = WidgetState.Stale,
        ),

        // 5. Unavailable
        ViewState(widgetState = WidgetState.Unavailable),

        // 6. Loading
        ViewState(widgetState = WidgetState.Loading),
    )
}
