package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase.Status
import io.buszi.nascope.domain.model.connection.CertificateFingerprint
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.ServerCertificate
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ConnectionStatusUiTest {

    @Test
    fun statusesMapToTheirUiStatus() {
        assertEquals(ConnectionStatusUi.Connecting, ConnectionStatusUi.from(Status.Connecting))
        assertEquals(ConnectionStatusUi.Online, ConnectionStatusUi.from(Status.Online))
        assertEquals(ConnectionStatusUi.Offline, ConnectionStatusUi.from(Status.Offline(ConnectionFailure.Timeout)))
        assertEquals(
            ConnectionStatusUi.PasswordMissing,
            ConnectionStatusUi.from(Status.Offline(ConnectionFailure.PasswordMissing)),
        )
        val certificate = ServerCertificate(
            CertificateFingerprint.parse("ab".repeat(32))!!, "CN=nas", "CN=nas", Instant.fromEpochSeconds(0),
        )
        assertEquals(
            ConnectionStatusUi.CertificateUntrusted,
            ConnectionStatusUi.from(Status.Offline(ConnectionFailure.UntrustedCertificate(certificate))),
        )
        assertEquals(
            ConnectionStatusUi.CertificateUntrusted,
            ConnectionStatusUi.from(Status.Offline(ConnectionFailure.CertificateChanged(certificate))),
        )
        assertEquals(ConnectionStatusUi.Offline, ConnectionStatusUi.from(Status.Offline(ConnectionFailure.TlsError)))
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
        assertEquals(StatusLevel.Critical, ConnectionStatusUi.PasswordMissing.level)
    }

    @Test
    fun onlyLiveStatusesPulse() {
        assertEquals(true, ConnectionStatusUi.Online.pulsing)
        assertEquals(true, ConnectionStatusUi.Connecting.pulsing)
        assertEquals(false, ConnectionStatusUi.Offline.pulsing)
        assertEquals(false, ConnectionStatusUi.PasswordMissing.pulsing)
    }
}
