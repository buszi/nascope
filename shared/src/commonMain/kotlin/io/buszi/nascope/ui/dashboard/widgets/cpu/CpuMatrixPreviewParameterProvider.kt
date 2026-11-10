package io.buszi.nascope.ui.dashboard.widgets.cpu

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.util.Message
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.cpu.CpuMatrixViewModel.ViewState

/**
 * Preview parameter provider supplying realistic synthetic CPU telemetry datasets.
 *
 * Demonstrates a variety of real-world scenarios:
 * - 16-core consumer workstation with balanced workloads.
 * - 4-core low-power NAS appliance under light usage.
 * - 8-core server node operating under elevated load.
 * - 32-core high-throughput compute cluster node.
 * - Critical throttled CPU state with high thermals and peak utilization.
 * - Loading, Error, and Subsystem Offline lifecycle states.
 */
class CpuMatrixPreviewParameterProvider : PreviewParameterProvider<ViewState> {

    override val values: Sequence<ViewState> = sequenceOf(
        Ryzen16Core,
        IntelQuadCore,
        XeonOctaCore,
        Threadripper32Core,
        HighLoadCritical,
        LoadingState,
        ErrorState,
        OfflineState,
    )

    companion object {
        val Ryzen16Core = ViewState(
            modelName = "AMD Ryzen 9 5950X (16-Core)",
            aggregateUsage = 42.8f,
            usageTrend = "+2.4% / 5m",
            frequencyGhz = 3.80f,
            temperatureC = 58.0f,
            coreUsages = listOf(
                34.2f, 48.1f, 12.0f, 88.5f,
                24.0f, 62.3f, 41.0f, 19.8f,
                55.4f, 31.2f, 76.8f, 92.1f,
                15.6f, 43.9f, 28.7f, 64.5f,
            ),
            widgetState = WidgetState.Ready,
        )

        val IntelQuadCore = ViewState(
            modelName = "Intel Celeron J4125 (4-Core)",
            aggregateUsage = 28.4f,
            usageTrend = "-1.2% / 5m",
            frequencyGhz = 2.00f,
            temperatureC = 44.0f,
            coreUsages = listOf(22.0f, 35.5f, 18.2f, 38.0f),
            widgetState = WidgetState.Ready,
        )

        val XeonOctaCore = ViewState(
            modelName = "Intel Xeon E5-2667 v4 (8-Core)",
            aggregateUsage = 76.4f,
            usageTrend = "+8.5% / 5m",
            frequencyGhz = 3.20f,
            temperatureC = 76.0f,
            coreUsages = listOf(72.0f, 84.5f, 68.2f, 88.0f, 74.1f, 79.5f, 65.4f, 79.8f),
            widgetState = WidgetState.Ready,
        )

        val Threadripper32Core = ViewState(
            modelName = "AMD Threadripper PRO 5975WX (32-Core)",
            aggregateUsage = 61.2f,
            usageTrend = "+0.8% / 5m",
            frequencyGhz = 4.10f,
            temperatureC = 68.0f,
            coreUsages = listOf(
                45.2f, 68.1f, 52.0f, 78.5f, 34.0f, 62.3f, 41.0f, 59.8f,
                75.4f, 81.2f, 46.8f, 62.1f, 55.6f, 63.9f, 48.7f, 74.5f,
                65.0f, 71.3f, 58.4f, 82.0f, 42.1f, 55.8f, 49.2f, 67.4f,
                70.5f, 84.0f, 53.6f, 69.1f, 60.2f, 73.4f, 51.0f, 77.9f,
            ),
            widgetState = WidgetState.Ready,
        )

        val HighLoadCritical = ViewState(
            modelName = "AMD EPYC 7763 (64-Core)",
            aggregateUsage = 94.6f,
            usageTrend = "+18.2% / 5m",
            frequencyGhz = 4.45f,
            temperatureC = 89.0f,
            coreUsages = listOf(
                92.0f, 96.5f, 91.2f, 98.0f, 89.1f, 95.5f, 94.4f, 97.8f,
                93.2f, 99.1f, 90.5f, 96.0f, 92.4f, 94.8f, 91.0f, 98.5f,
            ),
            widgetState = WidgetState.Ready,
        )

        val LoadingState = ViewState(
            widgetState = WidgetState.Loading,
        )

        val ErrorState = ViewState(
            widgetState = WidgetState.Error(
                message = Message.plain("Connection refused to Glances daemon at 192.168.1.120:61208"),
                onRetry = {},
            ),
        )

        val OfflineState = ViewState(
            widgetState = WidgetState.Offline(
                reason = Message.plain("Host CPU telemetry subsystem suspended or unreachable"),
                onReconnect = {},
            ),
        )
    }
}
