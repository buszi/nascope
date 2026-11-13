package io.buszi.nascope.domain.model.connection

import kotlin.jvm.JvmInline

/**
 * SHA-256 of a server's leaf certificate (its DER encoding), as 64 lowercase hex characters.
 *
 * A profile that stores one trusts exactly that certificate, also when it is self-signed or issued for another
 * name; any other certificate is rejected. It is public data, so it lives in the profile, not in the secret store.
 */
@JvmInline
value class CertificateFingerprint private constructor(val sha256: String) {

    companion object {
        private val Separators = Regex("[\\s:]")
        private val Sha256Hex = Regex("[0-9a-f]{64}")

        /** Accepts plain or colon-separated hex in any case; `null` when [value] is not a SHA-256 fingerprint. */
        fun parse(value: String): CertificateFingerprint? =
            value.replace(Separators, "").lowercase()
                .takeIf { Sha256Hex.matches(it) }
                ?.let(::CertificateFingerprint)

        fun of(sha256: ByteArray): CertificateFingerprint {
            require(sha256.size == 32) { "A SHA-256 digest has 32 bytes, got ${sha256.size}" }
            return CertificateFingerprint(sha256.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') })
        }
    }
}
