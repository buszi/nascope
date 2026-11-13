package io.buszi.nascope.network

import io.buszi.nascope.domain.model.connection.CertificateFingerprint
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerCredentials

interface GlancesConnectionClient {
    /**
     * One unauthenticated request to the status endpoint of [apiVersion]. `null` means it answered as Glances.
     * [trustedCertificate] is trusted on top of the platform's validation (see `ServerProfile.trustedCertificate`).
     */
    suspend fun fetchStatus(
        address: ServerAddress,
        apiVersion: GlancesApiVersion,
        trustedCertificate: CertificateFingerprint?,
    ): ConnectionFailure?

    /** One request to an endpoint that is protected when the server has a password. `null` means access granted. */
    suspend fun verifyAccess(
        address: ServerAddress,
        apiVersion: GlancesApiVersion,
        credentials: ServerCredentials?,
        trustedCertificate: CertificateFingerprint?,
    ): ConnectionFailure?
}
