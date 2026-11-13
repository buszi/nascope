package io.buszi.nascope.domain.model.connection

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CertificateFingerprintTest {

    @Test
    fun parseAcceptsPlainLowercaseHex() {
        assertEquals(HEX, CertificateFingerprint.parse(HEX)?.sha256)
    }

    @Test
    fun parseNormalizesColonSeparatedUppercaseHex() {
        val colons = HEX.uppercase().chunked(2).joinToString(":")

        assertEquals(HEX, CertificateFingerprint.parse(colons)?.sha256)
    }

    @Test
    fun parseRejectsAnythingButSha256Hex() {
        assertNull(CertificateFingerprint.parse(""))
        assertNull(CertificateFingerprint.parse(HEX.dropLast(2)))
        assertNull(CertificateFingerprint.parse(HEX + "00"))
        assertNull(CertificateFingerprint.parse(HEX.replaceFirst('3', 'g')))
    }

    @Test
    fun ofFormatsADigestAsLowercaseHex() {
        val digest = MessageDigest.getInstance("SHA-256").digest("nascope".encodeToByteArray())

        assertEquals(
            digest.joinToString("") { "%02x".format(it) },
            CertificateFingerprint.of(digest).sha256,
        )
    }

    @Test
    fun ofKeepsLeadingZeroBytes() {
        val digest = ByteArray(32) { if (it == 0) 0x0a else 0xff.toByte() }

        assertEquals("0a" + "ff".repeat(31), CertificateFingerprint.of(digest).sha256)
    }

    @Test
    fun ofRejectsADigestOfTheWrongSize() {
        assertFailsWith<IllegalArgumentException> { CertificateFingerprint.of(ByteArray(20)) }
    }

    private companion object {
        const val HEX = "37b1bda5ad60020cb08f056765f402f0ac0c9034c8aa2042502279e37600cae2"
    }
}
