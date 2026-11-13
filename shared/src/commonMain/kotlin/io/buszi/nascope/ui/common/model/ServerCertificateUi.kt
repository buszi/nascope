package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.model.connection.CertificateFingerprint
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.ServerCertificate

/** A server certificate as the trust prompt shows it. */
@Immutable
data class ServerCertificateUi(
    /** Uppercase hex pairs separated by colons, the form browsers and `openssl` print. */
    val fingerprint: String,
    val subject: String,
    val issuer: String,
    /** `yyyy-MM-dd`, in UTC. */
    val validUntil: String,
    val isSelfSigned: Boolean,
) {
    companion object {
        fun from(data: ServerCertificate) = ServerCertificateUi(
            fingerprint = formatFingerprint(data.fingerprint),
            subject = data.subject,
            issuer = data.issuer,
            validUntil = data.validUntil.toString().substringBefore('T'),
            isSelfSigned = data.isSelfSigned,
        )

        fun formatFingerprint(data: CertificateFingerprint): String =
            data.sha256.uppercase().chunked(2).joinToString(":")
    }
}

/** Offer to trust the certificate a connection test was refused with. */
@Immutable
data class CertificatePromptUi(
    val certificate: ServerCertificateUi,
    /** The profile trusted another certificate: a renewal, or someone in between. Warned about more strongly. */
    val isChange: Boolean,
) {
    companion object {
        /** `null` for failures that are not about a certificate the user could trust. */
        fun from(data: ConnectionFailure): CertificatePromptUi? = when (data) {
            is ConnectionFailure.UntrustedCertificate -> CertificatePromptUi(ServerCertificateUi.from(data.certificate), isChange = false)
            is ConnectionFailure.CertificateChanged -> CertificatePromptUi(ServerCertificateUi.from(data.certificate), isChange = true)
            else -> null
        }
    }
}
