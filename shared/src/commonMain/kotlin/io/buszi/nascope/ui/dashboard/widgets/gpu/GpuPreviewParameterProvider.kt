package io.buszi.nascope.ui.dashboard.widgets.gpu

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.model.GpuUi
import io.buszi.nascope.ui.common.util.Message
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.gpu.GpuViewModel.ViewState

/**
 * Preview parameter provider for the GPU brick:
 * - an integrated GPU that reports the utilization only, idle and busy;
 * - a discrete GPU with memory, temperature and fan, nominal and critical;
 * - two GPUs;
 * - a host without a GPU, and the loading, stale, unavailable, error and offline lifecycle states.
 */
class GpuPreviewParameterProvider : PreviewParameterProvider<ViewState> {

    override val values: Sequence<ViewState> = sequenceOf(
        IntegratedIdle,
        IntegratedBusy,
        DiscreteNominal,
        DiscreteCritical,
        TwoGpus,
        NoGpu,
        LoadingState,
        StaleState,
        UnavailableState,
        ErrorState,
        OfflineState,
    )

    companion object {
        private val history = listOf(4f, 6f, 5f, 9f, 14f, 12f, 8f, 6f, 5f, 7f, 18f, 34f, 41f, 38f, 22f, 10f, 8f, 12f)

        private val intel = GpuUi(
            id = "intel0",
            name = "Intel GPU",
            utilizationPercent = 12f,
            memoryPercent = null,
            temperatureC = null,
            fanSpeedPercent = null,
            utilizationHistory = history,
        )

        private val nvidia = GpuUi(
            id = "nvidia0",
            name = "NVIDIA GeForce RTX 3060",
            utilizationPercent = 46f,
            memoryPercent = 38f,
            temperatureC = 61f,
            fanSpeedPercent = 42f,
            utilizationHistory = history.map { it * 2 },
        )

        val IntegratedIdle = ViewState(
            gpus = listOf(intel.copy(utilizationPercent = 0f, utilizationHistory = List(18) { 0f })),
            widgetState = WidgetState.Ready,
        )

        val IntegratedBusy = ViewState(gpus = listOf(intel), widgetState = WidgetState.Ready)

        val DiscreteNominal = ViewState(gpus = listOf(nvidia), widgetState = WidgetState.Ready)

        val DiscreteCritical = ViewState(
            gpus = listOf(
                nvidia.copy(
                    utilizationPercent = 97f,
                    memoryPercent = 93f,
                    temperatureC = 88f,
                    fanSpeedPercent = 100f,
                    utilizationHistory = history.map { 70f + it / 2f },
                ),
            ),
            widgetState = WidgetState.Ready,
        )

        val TwoGpus = ViewState(
            gpus = listOf(intel, nvidia),
            widgetState = WidgetState.Ready,
        )

        /** Glances answers with an empty list: no GPU it can read. */
        val NoGpu = ViewState(widgetState = WidgetState.Ready)

        val LoadingState = ViewState(widgetState = WidgetState.Loading)

        /** Latest poll failed: values from an earlier one, with the stale tag. */
        val StaleState = IntegratedBusy.copy(widgetState = WidgetState.Stale)

        val UnavailableState = ViewState(widgetState = WidgetState.Unavailable)

        val ErrorState = ViewState(
            widgetState = WidgetState.Error(
                message = Message.plain("Failed to fetch GPU telemetry from Glances daemon"),
                onRetry = {},
            ),
        )

        val OfflineState = ViewState(
            widgetState = WidgetState.Offline(
                reason = Message.plain("GPU subsystem offline"),
                onReconnect = {},
            ),
        )
    }
}
