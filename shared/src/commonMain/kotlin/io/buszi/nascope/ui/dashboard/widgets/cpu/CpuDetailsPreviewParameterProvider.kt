package io.buszi.nascope.ui.dashboard.widgets.cpu

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.model.CpuCoreUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuDetailsViewModel.ViewState

class CpuDetailsPreviewParameterProvider : PreviewParameterProvider<ViewState> {
    override val values: Sequence<ViewState> = sequenceOf(
        // 1. Balanced workstation load
        ViewState(
            modelName = "AMD Ryzen 9 5950X (16-Core)",
            aggregateUsage = 42.8f,
            usageTrend = "+2.4% / 5m",
            frequencyGhz = 3.80f,
            temperatureC = 58.0f,
            cores = previewCores(
                usages = listOf(34f, 48f, 12f, 88f, 24f, 62f, 41f, 20f),
                frequencies = listOf(3.8f, 3.9f, 2.2f, 4.5f, 2.9f, 4.1f, 3.8f, 2.4f),
                temperatures = listOf(55f, 57f, 49f, 66f, 52f, 61f, 56f, 50f),
            ),
            usageHistory = listOf(31f, 34f, 39f, 36f, 42f, 45f, 43f, 39f, 38f, 43f, 49f, 52f, 47f, 44f, 40f),
            load1m = 0.42f,
            load5m = 0.58f,
            load15m = 0.71f,
            widgetState = WidgetState.Ready,
        ),

        // 2. Hot and saturated
        ViewState(
            modelName = "Intel Xeon E-2288G (8-Core)",
            aggregateUsage = 93.5f,
            usageTrend = "+18.0% / 5m",
            frequencyGhz = 4.70f,
            temperatureC = 88.0f,
            cores = previewCores(
                usages = listOf(97f, 95f, 92f, 99f, 88f, 94f, 91f, 96f),
                frequencies = listOf(4.7f, 4.7f, 4.6f, 4.7f, 4.5f, 4.7f, 4.6f, 4.7f),
                temperatures = listOf(86f, 88f, 84f, 90f, 82f, 87f, 85f, 89f),
            ),
            usageHistory = listOf(60f, 68f, 75f, 82f, 88f, 91f, 94f, 96f, 95f, 93f),
            load1m = 9.12f,
            load5m = 7.80f,
            load15m = 5.40f,
            sortOrder = CpuDetailsViewModel.SortOrder.Temperature,
            widgetState = WidgetState.Ready,
        ),

        // 3. Loading
        ViewState(widgetState = WidgetState.Loading),
    )

    private companion object {
        fun previewCores(
            usages: List<Float>,
            frequencies: List<Float>,
            temperatures: List<Float>,
        ): List<CpuCoreUi> = usages.indices.map { index ->
            CpuCoreUi(
                index = index,
                usagePercent = usages[index],
                frequencyGhz = frequencies[index],
                temperatureC = temperatures[index],
            )
        }
    }
}
