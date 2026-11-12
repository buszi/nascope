package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.model.connection.ServerProtocol

/** UI representation of the protocol used to reach a server. */
@Immutable
enum class ServerProtocolUi(val label: String) {
    Http("HTTP"),
    Https("HTTPS"),
    ;

    /** The domain protocol this option stands for. */
    fun toDomain(): ServerProtocol = when (this) {
        Http -> ServerProtocol.Http
        Https -> ServerProtocol.Https
    }

    companion object {
        fun from(data: ServerProtocol): ServerProtocolUi = when (data) {
            ServerProtocol.Http -> Http
            ServerProtocol.Https -> Https
        }
    }
}
