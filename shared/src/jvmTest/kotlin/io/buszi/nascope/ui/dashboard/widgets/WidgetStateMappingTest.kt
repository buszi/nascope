package io.buszi.nascope.ui.dashboard.widgets

import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WidgetStateMappingTest {

    @Test
    fun loadingMapsToLoading() {
        assertEquals(WidgetState.Loading, TelemetryState.Loading.toWidgetState {})
    }

    @Test
    fun freshReadyMapsToReadyAndStaleReadyToStale() {
        assertEquals(WidgetState.Ready, TelemetryState.Ready("x", isStale = false).toWidgetState {})
        assertEquals(WidgetState.Stale, TelemetryState.Ready("x", isStale = true).toWidgetState {})
    }

    @Test
    fun staleContentCountsAsReady() {
        assertTrue(WidgetState.Stale.isReady)
        assertTrue(WidgetState.Ready.isReady)
        assertEquals(false, WidgetState.Unavailable.isReady)
    }

    @Test
    fun pluginUnavailableMapsToUnavailable() {
        assertEquals(
            WidgetState.Unavailable,
            TelemetryState.Failed(TelemetryFailure.PluginUnavailable).toWidgetState {},
        )
    }

    @Test
    fun connectionFailureMapsToOfflineWithAReconnectCallback() {
        var retried = 0

        val state = TelemetryState.Failed(TelemetryFailure.Connection(ConnectionFailure.Timeout))
            .toWidgetState { retried++ }

        assertIs<WidgetState.Offline>(state)
        state.onReconnect!!.invoke()
        assertEquals(1, retried)
    }

    @Test
    fun invalidResponseMapsToErrorWithARetryCallback() {
        var retried = 0

        val state = TelemetryState.Failed(TelemetryFailure.InvalidResponse("bad")).toWidgetState { retried++ }

        assertIs<WidgetState.Error>(state)
        assertNotNull(state.onRetry).invoke()
        assertEquals(1, retried)
    }
}
