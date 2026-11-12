package io.buszi.nascope.ui.dashboard.widgets.cpu

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.model.CpuCoreUi
import io.buszi.nascope.ui.common.model.CpuUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuDetailsViewModel.SortOrder
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuDetailsViewModel.ViewState

class CpuDetailsPreviewParameterProvider : PreviewParameterProvider<ViewState> {
    override val values: Sequence<ViewState> = sequenceOf(
        // 1. Balanced workstation load
        ViewState(
            cpu = CpuUi(
                modelName = "AMD Ryzen 9 5950X (16-Core)",
                usagePercent = 42.8f,
                trend = "+2.4%",
                frequencyGhz = 3.80f,
                maxFrequencyGhz = 4.90f,
                temperatureC = 58.0f,
                cores = cores(34f, 48f, 12f, 88f, 24f, 62f, 41f, 20f),
                usageHistory = listOf(31f, 34f, 39f, 36f, 42f, 45f, 43f, 39f, 38f, 43f, 49f, 52f, 47f, 44f, 40f),
            ),
            widgetState = WidgetState.Ready,
        ),

        // 2. Hot and saturated, sorted by usage
        ViewState(
            cpu = CpuUi(
                modelName = "Intel Xeon E-2288G (8-Core)",
                usagePercent = 93.5f,
                trend = "+18.0%",
                frequencyGhz = 4.70f,
                maxFrequencyGhz = 5.00f,
                temperatureC = 88.0f,
                cores = cores(97f, 95f, 92f, 99f, 88f, 94f, 91f, 96f),
                usageHistory = listOf(60f, 68f, 75f, 82f, 88f, 91f, 94f, 96f, 95f, 93f),
            ),
            sortOrder = SortOrder.Usage,
            widgetState = WidgetState.Ready,
        ),

        // 3. Container host: no clock, no temperature, no per-core data
        ViewState(
            cpu = CpuUi(usagePercent = 12.3f, usageHistory = listOf(10f, 14f, 12f, 13f)),
            widgetState = WidgetState.Ready,
        ),

        // 4. Stale
        ViewState(
            cpu = CpuUi(usagePercent = 42.8f, cores = cores(34f, 48f, 12f, 88f), usageHistory = listOf(31f, 42f)),
            widgetState = WidgetState.Stale,
        ),

        // 5. Unavailable
        ViewState(widgetState = WidgetState.Unavailable),

        // 6. Loading
        ViewState(widgetState = WidgetState.Loading),
    )

    private companion object {
        fun cores(vararg usages: Float): List<CpuCoreUi> =
            usages.mapIndexed { index, usage -> CpuCoreUi(index, usage) }
    }
}
