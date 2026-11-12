package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.connection.ValidateServerInputUseCase.AliasError
import io.buszi.nascope.domain.connection.ValidateServerInputUseCase.HostError
import io.buszi.nascope.domain.connection.ValidateServerInputUseCase.PortError
import io.buszi.nascope.domain.model.connection.ServerProtocol
import kotlin.test.Test
import kotlin.test.assertEquals

class FormErrorUiTest {

    @Test
    fun aliasErrorsMapToTheirUiError() {
        assertEquals(FormErrorUi.AliasBlank, FormErrorUi.from(AliasError.Blank))
    }

    @Test
    fun hostErrorsMapToTheirUiErrors() {
        assertEquals(FormErrorUi.HostBlank, FormErrorUi.from(HostError.Blank))
        assertEquals(FormErrorUi.HostContainsScheme, FormErrorUi.from(HostError.ContainsScheme))
        assertEquals(FormErrorUi.HostContainsPath, FormErrorUi.from(HostError.ContainsPath))
        assertEquals(FormErrorUi.HostContainsWhitespace, FormErrorUi.from(HostError.ContainsWhitespace))
        assertEquals(FormErrorUi.HostInvalid, FormErrorUi.from(HostError.Invalid))
    }

    @Test
    fun portErrorsMapToTheirUiErrors() {
        assertEquals(FormErrorUi.PortBlank, FormErrorUi.from(PortError.Blank))
        assertEquals(FormErrorUi.PortNotANumber, FormErrorUi.from(PortError.NotANumber))
        assertEquals(FormErrorUi.PortOutOfRange, FormErrorUi.from(PortError.OutOfRange))
    }

    @Test
    fun protocolsRoundTripBetweenUiAndDomain() {
        ServerProtocolUi.entries.forEach { assertEquals(it, ServerProtocolUi.from(it.toDomain())) }
        assertEquals(ServerProtocol.Https, ServerProtocolUi.Https.toDomain())
        assertEquals("HTTPS", ServerProtocolUi.Https.label)
    }
}
