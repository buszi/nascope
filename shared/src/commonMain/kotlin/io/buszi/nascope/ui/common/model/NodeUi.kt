package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId

/**
 * UI representation of a stored server.
 *
 * @param isActive Whether the app currently shows this server's data.
 * @param connectionStatus Live status; only known for the active server.
 */
@Immutable
data class NodeUi(
    val id: String,
    val alias: String,
    val host: String,
    val port: Int,
    val protocol: ServerProtocolUi,
    val isActive: Boolean = false,
    val connectionStatus: ConnectionStatusUi? = null,
) {
    /** `host:port`, with IPv6 literals in brackets. */
    val address: String
        get() = if (':' in host && !host.startsWith("[")) "[$host]:$port" else "$host:$port"

    /** The server needs a password that is not stored on this device; the card offers to enter it. */
    val isPasswordMissing: Boolean
        get() = connectionStatus == ConnectionStatusUi.PasswordMissing

    /** The server's certificate is not trusted; the card offers to review it in the editor. */
    val isCertificateUntrusted: Boolean
        get() = connectionStatus == ConnectionStatusUi.CertificateUntrusted

    /** Something only the editor can fix: the card leads there with a primary action instead of "Edit". */
    val needsAttention: Boolean
        get() = isPasswordMissing || isCertificateUntrusted

    companion object {
        fun from(
            data: ServerProfile,
            isActive: Boolean = false,
            connectionStatus: ConnectionStatusUi? = null,
        ) = NodeUi(
            id = data.id.value,
            alias = data.alias,
            host = data.address.host,
            port = data.address.port,
            protocol = ServerProtocolUi.from(data.address.protocol),
            isActive = isActive,
            connectionStatus = connectionStatus.takeIf { isActive },
        )

        /** Maps the stored profiles; only the one with [activeId] is marked active and carries [activeStatus]. */
        fun from(
            data: List<ServerProfile>,
            activeId: ServerProfileId?,
            activeStatus: ConnectionStatusUi?,
        ): List<NodeUi> = data.map { from(it, isActive = it.id == activeId, connectionStatus = activeStatus) }
    }
}
