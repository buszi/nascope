package io.buszi.nascope.ui.dashboard.widgets.memory

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.model.MemorySwapUi
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
        StaleState,
        UnavailableState,
        NoCacheInfoState,
        OfflineState,
    )

    companion object {
        val NominalServer = ViewState(
            memory = MemorySwapUi(
                totalBytes = 34_359_738_368L,
                usedBytes = 13_743_895_347L,
                cachedBytes = 9_126_805_504L,
                buffersBytes = 1_288_490_188L,
                freeBytes = 10_200_547_329L,
                totalSwapBytes = 17_179_869_184L,
                usedSwapBytes = 2_576_980_377L,
                freeSwapBytes = 14_602_888_807L,
            ),
            widgetState = WidgetState.Ready,
        )

        val ElevatedServer = ViewState(
            memory = MemorySwapUi(
                totalBytes = 68_719_476_736L,
                usedBytes = 55_834_574_848L,
                cachedBytes = 6_442_450_944L,
                buffersBytes = 2_147_483_648L,
                freeBytes = 4_294_967_296L,
                totalSwapBytes = 34_359_738_368L,
                usedSwapBytes = 19_327_352_832L,
                freeSwapBytes = 15_032_385_536L,
            ),
            widgetState = WidgetState.Ready,
        )

        val CriticalServer = ViewState(
            memory = MemorySwapUi(
                totalBytes = 17_179_869_184L,
                usedBytes = 15_891_378_995L,
                cachedBytes = 644_245_094L,
                buffersBytes = 214_748_364L,
                freeBytes = 429_496_731L,
                totalSwapBytes = 8_589_934_592L,
                usedSwapBytes = 7_730_941_132L,
                freeSwapBytes = 858_993_460L,
            ),
            widgetState = WidgetState.Ready,
        )

        val NoSwapAppliance = ViewState(
            memory = MemorySwapUi(
                totalBytes = 8_589_934_592L,
                usedBytes = 3_435_973_836L,
                cachedBytes = 3_006_477_107L,
                buffersBytes = 536_870_912L,
                freeBytes = 1_610_612_737L,
                totalSwapBytes = 0L,
                usedSwapBytes = 0L,
                freeSwapBytes = 0L,
            ),
            widgetState = WidgetState.Ready,
        )

        /** Latest poll failed: values from an earlier one, with the stale tag. */
        val StaleState = NominalServer.copy(widgetState = WidgetState.Stale)

        val UnavailableState = ViewState(widgetState = WidgetState.Unavailable)

        /** Host that reports no cached or buffers figures: the cells show a placeholder. */
        val NoCacheInfoState = NominalServer.copy(
            memory = NominalServer.memory.copy(cachedBytes = null, buffersBytes = null),
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
