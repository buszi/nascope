package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase.Status
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectionStatusUiTest {

    @Test
    fun statusesMapToTheirUiStatus() {
        assertEquals(ConnectionStatusUi.Connecting, ConnectionStatusUi.from(Status.Connecting))
        assertEquals(ConnectionStatusUi.Online, ConnectionStatusUi.from(Status.Online))
        assertEquals(ConnectionStatusUi.Offline, ConnectionStatusUi.from(Status.Offline(ConnectionFailure.Timeout)))
    }

    @Test
    fun idleIsReadAsConnecting() {
        assertEquals(ConnectionStatusUi.Connecting, ConnectionStatusUi.from(Status.Idle))
    }

    @Test
    fun statusLevelReflectsHealth() {
        assertEquals(StatusLevel.Operational, ConnectionStatusUi.Online.level)
        assertEquals(StatusLevel.Degraded, ConnectionStatusUi.Connecting.level)
        assertEquals(StatusLevel.Critical, ConnectionStatusUi.Offline.level)
    }

    @Test
    fun onlyLiveStatusesPulse() {
        assertEquals(true, ConnectionStatusUi.Online.pulsing)
        assertEquals(true, ConnectionStatusUi.Connecting.pulsing)
        assertEquals(false, ConnectionStatusUi.Offline.pulsing)
    }
}
