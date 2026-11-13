package io.buszi.nascope.domain.model.connection

import kotlin.time.Instant

/** The leaf certificate an HTTPS server presented, with what the user needs to decide whether to trust it. */
data class ServerCertificate(
    val fingerprint: CertificateFingerprint,
    /** Distinguished name, e.g. `CN=nas.local, O=UGREEN`. */
    val subject: String,
    val issuer: String,
    val validUntil: Instant,
) {
    /** Issued by itself: nobody vouches for it, which is the usual case for a NAS. */
    val isSelfSigned: Boolean get() = subject == issuer
}
