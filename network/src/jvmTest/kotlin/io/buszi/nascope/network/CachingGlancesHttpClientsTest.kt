package io.buszi.nascope.network

import io.buszi.nascope.domain.model.connection.CertificateFingerprint
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class CachingGlancesHttpClientsTest {

    private val clients = CachingGlancesHttpClients()
    private val first = CertificateFingerprint.parse("ab".repeat(32))
    private val second = CertificateFingerprint.parse("cd".repeat(32))

    @Test
    fun theSameTrustGetsTheSameClient() {
        assertSame(clients.clientFor(null), clients.clientFor(null))
        assertSame(clients.clientFor(first), clients.clientFor(CertificateFingerprint.parse("AB".repeat(32))))
    }

    @Test
    fun eachTrustGetsItsOwnClient() {
        assertNotSame(clients.clientFor(null), clients.clientFor(first))
        assertNotSame(clients.clientFor(first), clients.clientFor(second))
    }
}
