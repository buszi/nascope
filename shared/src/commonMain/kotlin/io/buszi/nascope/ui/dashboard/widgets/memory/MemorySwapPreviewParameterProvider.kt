package io.buszi.nascope.ui.dashboard.widgets.memory

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.util.Message
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.memory.MemorySwapViewModel.ViewState

/**
 * Preview parameter provider supplying realistic synthetic memory telemetry datasets.
 *
 * Demonstrates a variety of real-world scenarios:
 * - Nominal Linux multi-purpose host with balanced RAM and low swap usage.
 * - Elevated memory server operating under high RAM load and moderate swap.
 * - Critical memory exhaustion scenario where RAM and swap are nearly depleted.
 * - Appliance host running without swap space configured.
 * - Loading, Error, and Subsystem Offline lifecycle states.
 */
class MemorySwapPreviewParameterProvider : PreviewParameterProvider<ViewState> {

    override val values: Sequence<ViewState> = sequenceOf(
        NominalServer,
        ElevatedServer,
        CriticalServer,
        NoSwapAppliance,
        LoadingState,
        ErrorState,
        OfflineState,
    )

    companion object {
        val NominalServer = ViewState(
            totalMemBytes = 34_359_738_368L, // 32.0 GB
            usedMemBytes = 13_743_895_347L,  // 12.8 GB (40.0%)
            cachedMemBytes = 9_126_805_504L, // 8.5 GB (26.5%)
            buffersMemBytes = 1_288_490_188L,// 1.2 GB (3.7%)
            freeMemBytes = 10_200_547_329L,  // 9.5 GB (29.8%)
            totalSwapBytes = 17_179_869_184L,// 16.0 GB
            usedSwapBytes = 2_576_980_377L,  // 2.4 GB (15.0%)
            freeSwapBytes = 14_602_888_807L, // 13.6 GB (85.0%)
            widgetState = WidgetState.Ready,
        )

        val ElevatedServer = ViewState(
            totalMemBytes = 68_719_476_736L, // 64.0 GB
            usedMemBytes = 55_834_574_848L,  // 52.0 GB (81.2%)
            cachedMemBytes = 6_442_450_944L, // 6.0 GB (9.4%)
            buffersMemBytes = 2_147_483_648L,// 2.0 GB (3.1%)
            freeMemBytes = 4_294_967_296L,   // 4.0 GB (6.3%)
            totalSwapBytes = 34_359_738_368L,// 32.0 GB
            usedSwapBytes = 19_327_352_832L, // 18.0 GB (56.2%)
            freeSwapBytes = 15_032_385_536L, // 14.0 GB (43.8%)
            widgetState = WidgetState.Ready,
        )

        val CriticalServer = ViewState(
            totalMemBytes = 17_179_869_184L, // 16.0 GB
            usedMemBytes = 15_891_378_995L,  // 14.8 GB (92.5%)
            cachedMemBytes = 644_245_094L,   // 0.6 GB (3.8%)
            buffersMemBytes = 214_748_364L,  // 0.2 GB (1.2%)
            freeMemBytes = 429_496_731L,     // 0.4 GB (2.5%)
            totalSwapBytes = 8_589_934_592L, // 8.0 GB
            usedSwapBytes = 7_730_941_132L,  // 7.2 GB (90.0%)
            freeSwapBytes = 858_993_460L,    // 0.8 GB (10.0%)
            widgetState = WidgetState.Ready,
        )

        val NoSwapAppliance = ViewState(
            totalMemBytes = 8_589_934_592L,  // 8.0 GB
            usedMemBytes = 3_435_973_836L,   // 3.2 GB (40.0%)
            cachedMemBytes = 3_006_477_107L, // 2.8 GB (35.0%)
            buffersMemBytes = 536_870_912L,  // 0.5 GB (6.3%)
            freeMemBytes = 1_610_612_737L,   // 1.5 GB (18.7%)
            totalSwapBytes = 0L,
            usedSwapBytes = 0L,
            freeSwapBytes = 0L,
            widgetState = WidgetState.Ready,
        )

        val LoadingState = ViewState(
            widgetState = WidgetState.Loading,
        )

        val ErrorState = ViewState(
            widgetState = WidgetState.Error(
                message = Message.plain("Failed to fetch memory telemetry from Glances daemon"),
                onRetry = {},
            ),
        )

        val OfflineState = ViewState(
            widgetState = WidgetState.Offline(
                reason = Message.plain("Memory subsystem offline"),
                onReconnect = {},
            ),
        )
    }
}
