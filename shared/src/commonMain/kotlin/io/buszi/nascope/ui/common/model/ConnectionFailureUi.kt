package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.connection_failure_connection_refused
import io.buszi.nascope.resources.connection_failure_not_glances
import io.buszi.nascope.resources.connection_failure_timeout
import io.buszi.nascope.resources.connection_failure_tls_error
import io.buszi.nascope.resources.connection_failure_unauthorized
import io.buszi.nascope.resources.connection_failure_unexpected
import io.buszi.nascope.resources.connection_failure_unknown_host
import org.jetbrains.compose.resources.StringResource

/** UI representation of why a connection test failed, with the message shown to the user. */
@Immutable
enum class ConnectionFailureUi(val messageRes: StringResource) {
    UnknownHost(Res.string.connection_failure_unknown_host),
    ConnectionRefused(Res.string.connection_failure_connection_refused),
    Timeout(Res.string.connection_failure_timeout),
    TlsError(Res.string.connection_failure_tls_error),
    NotGlances(Res.string.connection_failure_not_glances),
    Unauthorized(Res.string.connection_failure_unauthorized),
    Unexpected(Res.string.connection_failure_unexpected),
    ;

    companion object {
        fun from(data: ConnectionFailure): ConnectionFailureUi = when (data) {
            ConnectionFailure.UnknownHost -> UnknownHost
            ConnectionFailure.ConnectionRefused -> ConnectionRefused
            ConnectionFailure.Timeout -> Timeout
            ConnectionFailure.TlsError -> TlsError
            ConnectionFailure.NotGlances -> NotGlances
            ConnectionFailure.Unauthorized -> Unauthorized
            is ConnectionFailure.Unexpected -> Unexpected
        }
    }
}
