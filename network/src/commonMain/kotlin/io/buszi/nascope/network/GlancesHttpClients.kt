package io.buszi.nascope.network

import io.buszi.nascope.domain.model.connection.CertificateFingerprint
import io.ktor.client.HttpClient
import org.koin.core.annotation.Single
import kotlin.jvm.Synchronized

/** The HTTP client for a server: its TLS trust is fixed when the client is built, so it depends on the pin. */
internal fun interface GlancesHttpClients {
    fun clientFor(trustedCertificate: CertificateFingerprint?): HttpClient
}

/**
 * One client per trusted certificate, built on first use and kept for the process: the default one (no
 * certificate) plus one per certificate the user trusted, which is a handful at most.
 */
@Single(binds = [GlancesHttpClients::class])
internal class CachingGlancesHttpClients : GlancesHttpClients {
    private val clients = mutableMapOf<CertificateFingerprint?, HttpClient>()

    @Synchronized
    override fun clientFor(trustedCertificate: CertificateFingerprint?): HttpClient =
        clients.getOrPut(trustedCertificate) { createGlancesHttpClient(trustedCertificate) }
}
