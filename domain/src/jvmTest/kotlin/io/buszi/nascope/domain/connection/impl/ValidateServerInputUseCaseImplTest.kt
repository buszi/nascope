package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.domain.connection.ValidateServerInputUseCase.AliasError
import io.buszi.nascope.domain.connection.ValidateServerInputUseCase.HostError
import io.buszi.nascope.domain.connection.ValidateServerInputUseCase.PortError
import io.buszi.nascope.domain.connection.ValidateServerInputUseCase.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ValidateServerInputUseCaseImplTest {

    private val useCase = ValidateServerInputUseCaseImpl()

    @Test
    fun validateServerInputUseCaseAcceptsValidInput() {
        val result = useCase("NAS", "nas.local", "61208")

        assertEquals(Result(alias = null, host = null, port = null), result)
        assertTrue(result.isValid)
    }

    @Test
    fun validateServerInputUseCaseRejectsBlankAlias() {
        assertEquals(AliasError.Blank, useCase("", "nas.local", "61208").alias)
        assertEquals(AliasError.Blank, useCase("   ", "nas.local", "61208").alias)
    }

    @Test
    fun validateServerInputUseCaseReportsOneErrorPerField() {
        assertEquals(
            Result(alias = AliasError.Blank, host = HostError.Blank, port = PortError.Blank),
            useCase("", "", ""),
        )
    }

    @Test
    fun validateServerInputUseCaseAcceptsHostnamesAndIpv4Addresses() {
        validHostnamesAndIpv4.forEach { assertNull(validateHost(it), it) }
    }

    @Test
    fun validateServerInputUseCaseAcceptsIpv6Literals() {
        validIpv6.forEach { assertNull(validateHost(it), it) }
    }

    @Test
    fun validateServerInputUseCaseRejectsSchemePrefix() {
        assertEquals(HostError.ContainsScheme, validateHost("http://nas.local"))
        assertEquals(HostError.ContainsScheme, validateHost("https://nas.local"))
    }

    @Test
    fun validateServerInputUseCaseRejectsPathsAndTrailingSlash() {
        assertEquals(HostError.ContainsPath, validateHost("nas.local/"))
        assertEquals(HostError.ContainsPath, validateHost("nas.local/api"))
        assertEquals(HostError.ContainsPath, validateHost("nas.local?x=1"))
    }

    @Test
    fun validateServerInputUseCaseRejectsWhitespace() {
        assertEquals(HostError.ContainsWhitespace, validateHost("nas local"))
        assertEquals(HostError.ContainsWhitespace, validateHost(" nas.local"))
        assertEquals(HostError.ContainsWhitespace, validateHost("nas.local "))
        assertEquals(HostError.Blank, validateHost("   "))
    }

    @Test
    fun validateServerInputUseCaseRejectsMalformedHosts() {
        invalidHosts.forEach { assertEquals(HostError.Invalid, validateHost(it), it) }
    }

    @Test
    fun validateServerInputUseCaseEnforcesHostnameLabelLength() {
        assertEquals(HostError.Invalid, validateHost("a".repeat(64) + ".local"))
        assertNull(validateHost("a".repeat(63) + ".local"))
    }

    @Test
    fun validateServerInputUseCaseAcceptsPortsWithinRange() {
        listOf("1", "80", "61208", "65535").forEach { assertNull(validatePort(it), it) }
    }

    @Test
    fun validateServerInputUseCaseRejectsPortsOutOfRange() {
        outOfRangePorts.forEach { assertEquals(PortError.OutOfRange, validatePort(it), it) }
    }

    @Test
    fun validateServerInputUseCaseRejectsNonNumericPorts() {
        nonNumericPorts.forEach { assertEquals(PortError.NotANumber, validatePort(it), it) }
        assertEquals(PortError.Blank, validatePort(""))
    }

    private fun validateHost(host: String) = useCase("alias", host, "61208").host

    private fun validatePort(port: String) = useCase("alias", "nas.local", port).port

    companion object {
        private val validHostnamesAndIpv4 = listOf(
            "nas", "nas.local", "my-nas.example.com", "NAS_01.lan",
            "192.168.1.10", "10.0.0.1", "0.0.0.0", "a1.b2",
        )
        private val validIpv6 = listOf(
            "::1", "[::1]", "fe80::1", "2001:db8::8a2e:370:7334",
            "2001:0db8:0000:0000:0000:ff00:0042:8329", "::ffff:192.168.1.1", "[2001:db8::1]",
        )
        private val invalidHosts = listOf(
            "999.1.1.1", "1.2.3", "1.2.3.4.5", "-nas.local", "nas-.local", "nas..local", ".nas",
            "nas!", "nas:61208", "user@nas", "[::1", "::1]", "1::2::3", ":::", "12345::1",
            "1:2:3:4:5:6:7", "1:2:3:4:5:6:7:8:9", "gggg::1", "[nas]",
        )
        private val outOfRangePorts = listOf("0", "65536", "99999", "99999999999999999999")
        private val nonNumericPorts = listOf("abc", "80a", "-1", "+80", "6.1", " 80", "٨٠")
    }
}
