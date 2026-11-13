package io.buszi.nascope.network

import io.buszi.nascope.domain.model.connection.CertificateFingerprint
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createPlatformHttpClient(
    trustedCertificate: CertificateFingerprint?,
    configure: HttpClientConfig<*>.() -> Unit,
): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            val trust = CertificateTrust(trustedCertificate)
            sslSocketFactory(trust.sslSocketFactory, trust.trustManager)
            hostnameVerifier(trust.hostnameVerifier)
        }
    }
    configure()
}
