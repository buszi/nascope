package io.buszi.nascope.ui.dashboard.widgets.sensors

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.model.FanSensorUi
import io.buszi.nascope.ui.common.model.SensorsUi
import io.buszi.nascope.ui.common.model.TemperatureSensorUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.sensors.SensorsThermalsViewModel.ViewState

class SensorsThermalsPreviewParameterProvider : PreviewParameterProvider<ViewState> {
    override val values: Sequence<ViewState> = sequenceOf(
        // 1. Nominal with fans
        ViewState(
            sensors = SensorsUi(
                cpu = listOf(TemperatureSensorUi("Package id 0", 58.0f, 70.0f, 85.0f)),
                drives = listOf(
                    TemperatureSensorUi("nvme0n1 (NVMe)", 41.0f, 65.0f, 75.0f),
                    TemperatureSensorUi("sda (WD Red 8TB)", 34.0f, 50.0f, 60.0f),
                ),
                fans = listOf(FanSensorUi("CPU Fan", 1380), FanSensorUi("Chassis 1", 920)),
            ),
            widgetState = WidgetState.Ready,
        ),

        // 2. Elevated
        ViewState(
            sensors = SensorsUi(
                cpu = listOf(TemperatureSensorUi("Package id 0", 78.0f, 70.0f, 85.0f)),
                drives = listOf(TemperatureSensorUi("nvme0n1 (NVMe)", 48.0f, 65.0f, 75.0f)),
                fans = listOf(FanSensorUi("CPU Fan", 2100)),
            ),
            widgetState = WidgetState.Ready,
        ),

        // 3. NAS in Docker: package and NVMe temperatures, no fans (passive cooling)
        ViewState(
            sensors = SensorsUi(
                cpu = listOf(TemperatureSensorUi("Package id 0", 45.0f, 70.0f, 85.0f)),
                drives = listOf(TemperatureSensorUi("Composite", 38.0f, 70.0f, 85.0f)),
            ),
            widgetState = WidgetState.Ready,
        ),

        // 4. Host with a sensors plugin but no readings (plain Docker): the empty text
        ViewState(widgetState = WidgetState.Ready),

        // 5. Stale
        ViewState(
            sensors = SensorsUi(cpu = listOf(TemperatureSensorUi("Package id 0", 58.0f, 70.0f, 85.0f))),
            widgetState = WidgetState.Stale,
        ),

        // 6. Unavailable (sensors plugin disabled)
        ViewState(widgetState = WidgetState.Unavailable),

        // 7. Loading
        ViewState(widgetState = WidgetState.Loading),
    )
}
