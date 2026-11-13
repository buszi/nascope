package io.buszi.nascope.network

import io.buszi.nascope.domain.model.connection.CertificateFingerprint
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.ServerCertificate
import okhttp3.OkHttpClient
import java.net.Socket
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager
import kotlin.time.Instant

/**
 * TLS trust for one HTTP client: whatever the platform trusts (with its usual hostname check), plus the one
 * [trusted] certificate the user accepted for a server, whatever its issuer, expiry or name.
 *
 * Trusting a certificate only ever adds to the platform's trust: a server that later moves to a publicly trusted
 * certificate keeps working. A certificate neither trusts fails the handshake with
 * [UntrustedServerCertificateException], which carries the certificate so the user can be asked about it.
 */
internal class CertificateTrust(
    private val trusted: CertificateFingerprint?,
    platform: X509TrustManager = platformTrustManager(),
) {

    val trustManager: X509TrustManager = PinningTrustManager(platform, trusted)

    val sslSocketFactory = SSLContext.getInstance("TLS")
        .apply { init(null, arrayOf(trustManager), null) }
        .socketFactory!!

    /** The trusted certificate is accepted for any name (a NAS is often reached by IP); others need a match. */
    val hostnameVerifier = HostnameVerifier { host, session ->
        (trusted != null && session.leafFingerprint() == trusted) || DefaultHostnameVerifier.verify(host, session)
    }

    private companion object {
        /** OkHttp's own verifier, the one a client without this trust would use. */
        val DefaultHostnameVerifier: HostnameVerifier = OkHttpClient().hostnameVerifier
    }
}

/** The handshake failed on a certificate that neither the platform nor the profile trusts. */
internal class UntrustedServerCertificateException(
    val certificate: ServerCertificate,
    /** The profile trusts another certificate: the server's certificate changed. */
    val replacesTrusted: Boolean,
    cause: CertificateException,
) : CertificateException("Untrusted server certificate ${certificate.fingerprint.sha256}", cause) {

    fun toConnectionFailure(): ConnectionFailure =
        if (replacesTrusted) {
            ConnectionFailure.CertificateChanged(certificate)
        } else {
            ConnectionFailure.UntrustedCertificate(certificate)
        }
}

/**
 * Accepts the leaf certificate when it is [trusted], otherwise asks [platform]. The [Socket] and [SSLEngine]
 * variants are forwarded as such, so the platform keeps its own per-connection checks.
 */
@Suppress("CustomX509TrustManager")
private class PinningTrustManager(
    private val platform: X509TrustManager,
    private val trusted: CertificateFingerprint?,
) : X509ExtendedTrustManager() {

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) =
        check(chain) { platform.checkServerTrusted(chain, authType) }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket?) =
        check(chain) {
            if (platform is X509ExtendedTrustManager) {
                platform.checkServerTrusted(chain, authType, socket)
            } else {
                platform.checkServerTrusted(chain, authType)
            }
        }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine?) =
        check(chain) {
            if (platform is X509ExtendedTrustManager) {
                platform.checkServerTrusted(chain, authType, engine)
            } else {
                platform.checkServerTrusted(chain, authType)
            }
        }

    // This app is never a TLS server and never presents client certificates.
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
        platform.checkClientTrusted(chain, authType)

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket?) =
        platform.checkClientTrusted(chain, authType)

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine?) =
        platform.checkClientTrusted(chain, authType)

    override fun getAcceptedIssuers(): Array<X509Certificate> = platform.acceptedIssuers

    private inline fun check(chain: Array<X509Certificate>, platformCheck: () -> Unit) {
        val leaf = chain.firstOrNull() ?: throw CertificateException("The server sent no certificate")
        if (trusted != null && leaf.fingerprint() == trusted) return
        try {
            platformCheck()
        } catch (e: CertificateException) {
            throw UntrustedServerCertificateException(leaf.toServerCertificate(), replacesTrusted = trusted != null, e)
        }
    }
}

private fun platformTrustManager(): X509TrustManager {
    val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    factory.init(null as KeyStore?)
    return factory.trustManagers.filterIsInstance<X509TrustManager>().first()
}

internal fun X509Certificate.fingerprint(): CertificateFingerprint =
    CertificateFingerprint.of(MessageDigest.getInstance("SHA-256").digest(encoded))

internal fun X509Certificate.toServerCertificate() = ServerCertificate(
    fingerprint = fingerprint(),
    subject = subjectX500Principal.name,
    issuer = issuerX500Principal.name,
    validUntil = Instant.fromEpochMilliseconds(notAfter.time),
)

private fun SSLSession.leafFingerprint(): CertificateFingerprint? =
    runCatching { (peerCertificates.firstOrNull() as? X509Certificate)?.fingerprint() }.getOrNull()
