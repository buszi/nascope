package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.connection.CertificateFingerprint
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.ServerCertificate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class ServerCertificateUiTest {

    @Test
    fun certificateIsMappedForDisplay() {
        assertEquals(
            ServerCertificateUi(
                fingerprint = "0A:" + "FF:".repeat(30) + "FF",
                subject = "CN=nas.local",
                issuer = "CN=nas.local",
                validUntil = "2027-03-01",
                isSelfSigned = true,
            ),
            ServerCertificateUi.from(certificate),
        )
    }

    @Test
    fun aCertificateFromAnotherIssuerIsNotSelfSigned() {
        assertEquals(false, ServerCertificateUi.from(certificate.copy(issuer = "CN=Home CA")).isSelfSigned)
    }

    @Test
    fun validUntilIsTheUtcDate() {
        val lateEvening = certificate.copy(validUntil = Instant.parse("2027-03-01T23:59:59Z"))

        assertEquals("2027-03-01", ServerCertificateUi.from(lateEvening).validUntil)
    }

    @Test
    fun aWrappableFingerprintOnlyAddsBreakOpportunitiesAfterTheColons() {
        val wrappable = ServerCertificateUi.wrappableFingerprint("0A:FF:12")

        assertEquals("0A:\u200BFF:\u200B12", wrappable)
        assertEquals("0A:FF:12", wrappable.replace("\u200B", ""))
    }

    @Test
    fun onlyCertificateFailuresPrompt() {
        assertEquals(
            CertificatePromptUi(ServerCertificateUi.from(certificate), isChange = false),
            CertificatePromptUi.from(ConnectionFailure.UntrustedCertificate(certificate)),
        )
        assertEquals(
            CertificatePromptUi(ServerCertificateUi.from(certificate), isChange = true),
            CertificatePromptUi.from(ConnectionFailure.CertificateChanged(certificate)),
        )
        assertNull(CertificatePromptUi.from(ConnectionFailure.TlsError))
        assertNull(CertificatePromptUi.from(ConnectionFailure.Unauthorized))
    }

    private companion object {
        val certificate = ServerCertificate(
            fingerprint = CertificateFingerprint.parse("0a" + "ff".repeat(31))!!,
            subject = "CN=nas.local",
            issuer = "CN=nas.local",
            validUntil = Instant.parse("2027-03-01T12:00:00Z"),
        )
    }
}
