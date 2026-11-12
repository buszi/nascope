package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.connection.ConnectionFailure
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectionFailureUiTest {

    @Test
    fun everyConnectionFailureMapsToItsOwnUiFailure() {
        assertEquals(ConnectionFailureUi.UnknownHost, ConnectionFailureUi.from(ConnectionFailure.UnknownHost))
        assertEquals(ConnectionFailureUi.ConnectionRefused, ConnectionFailureUi.from(ConnectionFailure.ConnectionRefused))
        assertEquals(ConnectionFailureUi.Timeout, ConnectionFailureUi.from(ConnectionFailure.Timeout))
        assertEquals(ConnectionFailureUi.TlsError, ConnectionFailureUi.from(ConnectionFailure.TlsError))
        assertEquals(ConnectionFailureUi.NotGlances, ConnectionFailureUi.from(ConnectionFailure.NotGlances))
        assertEquals(ConnectionFailureUi.Unauthorized, ConnectionFailureUi.from(ConnectionFailure.Unauthorized))
    }

    @Test
    fun unexpectedFailureDropsTheTechnicalDetail() {
        assertEquals(ConnectionFailureUi.Unexpected, ConnectionFailureUi.from(ConnectionFailure.Unexpected("HTTP 500")))
    }
}
