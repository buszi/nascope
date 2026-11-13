package io.buszi.nascope.network

import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerProtocol
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.io.IOException
import java.net.InetAddress
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A real HTTPS server on 127.0.0.1 with generated certificates. The server is always reached by IP, which none
 * of the certificates names unless a test says so, as with a NAS reached by its LAN address.
 */
class CertificateTrustTest {

    private val server = MockWebServer()

    @AfterTest
    fun tearDown() {
        server.close()
    }

    // End to end through the Glances client and the platform's real trust store.

    @Test
    fun aSelfSignedCertificateIsReportedAsUntrustedWithItsDetails() = runTest {
        serve(selfSigned)

        val failure = glancesClient().fetchStatus(address(), GlancesApiVersion.V4, trustedCertificate = null)

        val untrusted = assertIs<ConnectionFailure.UntrustedCertificate>(failure)
        assertEquals(selfSigned.certificate.fingerprint(), untrusted.certificate.fingerprint)
        assertEquals("CN=nas.local", untrusted.certificate.subject)
        assertTrue(untrusted.certificate.isSelfSigned)
    }

    @Test
    fun theTrustedCertificateIsAcceptedAlthoughItIsSelfSignedAndNamesAnotherHost() = runTest {
        serve(selfSigned, body = """{"version": "4.5.9"}""")

        val failure = glancesClient().fetchStatus(
            address(),
            GlancesApiVersion.V4,
            trustedCertificate = selfSigned.certificate.fingerprint(),
        )

        assertNull(failure)
    }

    @Test
    fun anotherCertificateThanTheTrustedOneIsReportedAsChanged() = runTest {
        serve(selfSigned)
        val previous = HeldCertificate.Builder().commonName("nas.local").build()

        val failure = glancesClient().fetchStatus(
            address(),
            GlancesApiVersion.V4,
            trustedCertificate = previous.certificate.fingerprint(),
        )

        val changed = assertIs<ConnectionFailure.CertificateChanged>(failure)
        assertEquals(selfSigned.certificate.fingerprint(), changed.certificate.fingerprint)
    }

    @Test
    fun theTrustIsPerClientSoAnUnpinnedRequestStillFails() = runTest {
        serve(selfSigned, body = """{"version": "4.5.9"}""")
        serve(selfSigned)
        val client = glancesClient()

        assertNull(client.fetchStatus(address(), GlancesApiVersion.V4, selfSigned.certificate.fingerprint()))
        assertIs<ConnectionFailure.UntrustedCertificate>(
            client.fetchStatus(address(), GlancesApiVersion.V4, trustedCertificate = null),
        )
    }

    // Trust rules against a stand-in for the platform's trust store that trusts one private CA.

    @Test
    fun aCertificateThePlatformTrustsIsAccepted() {
        serve(issuedForTheIp)

        assertEquals(200, call(CertificateTrust(trusted = null, platform = platformTrustingCa)))
    }

    @Test
    fun trustingACertificateDoesNotStopThePlatformTrustedOneFromWorking() {
        serve(issuedForTheIp)
        val trust = CertificateTrust(trusted = selfSigned.certificate.fingerprint(), platform = platformTrustingCa)

        assertEquals(200, call(trust))
    }

    @Test
    fun aPlatformTrustedCertificateForAnotherNameIsATlsErrorNotAQuestion() {
        serve(issuedForAnotherName)

        val error = assertFailsWith<IOException> { call(CertificateTrust(trusted = null, platform = platformTrustingCa)) }

        assertEquals(ConnectionFailure.TlsError, error.toConnectionFailure())
    }

    @Test
    fun anUntrustedCertificateIsFoundBehindTheTlsStacksOwnException() {
        serve(selfSigned)

        val error = assertFailsWith<IOException> { call(CertificateTrust(trusted = null, platform = platformTrustingCa)) }

        assertIs<ConnectionFailure.UntrustedCertificate>(error.toConnectionFailure())
    }

    private fun serve(certificate: HeldCertificate, body: String = "") {
        if (!server.started) {
            // A CA-issued certificate is sent with its CA, as a server would send its chain.
            val chain = if (certificate === selfSigned) emptyArray() else arrayOf(ca.certificate)
            val handshake = HandshakeCertificates.Builder().heldCertificate(certificate, *chain).build()
            server.useHttps(handshake.sslSocketFactory())
            server.start(InetAddress.getByName(LOOPBACK), 0)
        }
        server.enqueue(MockResponse(body = body))
    }

    private fun address() = ServerAddress(ServerProtocol.Https, LOOPBACK, server.port)

    private fun glancesClient() = KtorGlancesConnectionClient(GlancesHttpClients { createGlancesHttpClient(it) })

    /** One request through a plain OkHttp client with [trust]; returns the status code. */
    private fun call(trust: CertificateTrust): Int {
        val client = OkHttpClient.Builder()
            .sslSocketFactory(trust.sslSocketFactory, trust.trustManager)
            .hostnameVerifier(trust.hostnameVerifier)
            .build()
        val request = Request.Builder().url("https://$LOOPBACK:${server.port}/").build()
        return client.newCall(request).execute().use { it.code }
    }

    private companion object {
        const val LOOPBACK = "127.0.0.1"

        val selfSigned: HeldCertificate = HeldCertificate.Builder()
            .commonName("nas.local")
            .addSubjectAlternativeName("nas.local")
            .build()

        val ca: HeldCertificate = HeldCertificate.Builder().commonName("Test CA").certificateAuthority(0).build()

        val issuedForTheIp: HeldCertificate = HeldCertificate.Builder()
            .commonName("nas")
            .addSubjectAlternativeName(LOOPBACK)
            .signedBy(ca)
            .build()

        val issuedForAnotherName: HeldCertificate = HeldCertificate.Builder()
            .commonName("nas.example.com")
            .addSubjectAlternativeName("nas.example.com")
            .signedBy(ca)
            .build()

        val platformTrustingCa = HandshakeCertificates.Builder()
            .addTrustedCertificate(ca.certificate)
            .build()
            .trustManager
    }
}
