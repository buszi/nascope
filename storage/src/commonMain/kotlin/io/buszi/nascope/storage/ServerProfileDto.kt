package io.buszi.nascope.storage

import io.buszi.nascope.domain.model.connection.CertificateFingerprint
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import kotlinx.serialization.Serializable

@Serializable
internal data class ServerProfileDto(
    val id: String,
    val alias: String,
    val protocol: String,
    val host: String,
    val port: Int,
    val apiVersion: String,
    val authUsername: String? = null,
    /** Added in 1.1.0. Left out when `null`, so a profile without one is stored as before. */
    val trustedCertSha256: String? = null,
) {
    /**
     * Returns `null` when the stored values no longer map to a valid profile. An unreadable fingerprint is dropped
     * instead: the profile then falls back to the platform's validation and asks to trust the certificate again.
     */
    fun toDomain(): ServerProfile? {
        val protocol = ServerProtocol.entries.firstOrNull { it.name == protocol } ?: return null
        val apiVersion = GlancesApiVersion.entries.firstOrNull { it.name == apiVersion } ?: return null
        return ServerProfile(
            id = ServerProfileId(id),
            alias = alias,
            address = ServerAddress(protocol = protocol, host = host, port = port),
            apiVersion = apiVersion,
            auth = authUsername?.let(ServerAuth::Basic) ?: ServerAuth.None,
            trustedCertificate = trustedCertSha256?.let(CertificateFingerprint::parse),
        )
    }

    companion object {
        fun from(profile: ServerProfile) = ServerProfileDto(
            id = profile.id.value,
            alias = profile.alias,
            protocol = profile.address.protocol.name,
            host = profile.address.host,
            port = profile.address.port,
            apiVersion = profile.apiVersion.name,
            authUsername = (profile.auth as? ServerAuth.Basic)?.username,
            trustedCertSha256 = profile.trustedCertificate?.sha256,
        )
    }
}
