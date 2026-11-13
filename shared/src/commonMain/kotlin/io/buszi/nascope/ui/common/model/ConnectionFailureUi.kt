package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.connection_failure_certificate_changed
import io.buszi.nascope.resources.connection_failure_connection_refused
import io.buszi.nascope.resources.connection_failure_not_glances
import io.buszi.nascope.resources.connection_failure_password_missing
import io.buszi.nascope.resources.connection_failure_timeout
import io.buszi.nascope.resources.connection_failure_tls_error
import io.buszi.nascope.resources.connection_failure_unauthorized
import io.buszi.nascope.resources.connection_failure_unexpected
import io.buszi.nascope.resources.connection_failure_unknown_host
import io.buszi.nascope.resources.connection_failure_untrusted_certificate
import org.jetbrains.compose.resources.StringResource

/** UI representation of why a connection test failed, with the message shown to the user. */
@Immutable
enum class ConnectionFailureUi(val messageRes: StringResource) {
    UnknownHost(Res.string.connection_failure_unknown_host),
    ConnectionRefused(Res.string.connection_failure_connection_refused),
    Timeout(Res.string.connection_failure_timeout),
    TlsError(Res.string.connection_failure_tls_error),
    UntrustedCertificate(Res.string.connection_failure_untrusted_certificate),
    CertificateChanged(Res.string.connection_failure_certificate_changed),
    NotGlances(Res.string.connection_failure_not_glances),
    Unauthorized(Res.string.connection_failure_unauthorized),
    PasswordMissing(Res.string.connection_failure_password_missing),
    Unexpected(Res.string.connection_failure_unexpected),
    ;

    companion object {
        fun from(data: ConnectionFailure): ConnectionFailureUi = when (data) {
            ConnectionFailure.UnknownHost -> UnknownHost
            ConnectionFailure.ConnectionRefused -> ConnectionRefused
            ConnectionFailure.Timeout -> Timeout
            ConnectionFailure.TlsError -> TlsError
            is ConnectionFailure.UntrustedCertificate -> UntrustedCertificate
            is ConnectionFailure.CertificateChanged -> CertificateChanged
            ConnectionFailure.NotGlances -> NotGlances
            ConnectionFailure.Unauthorized -> Unauthorized
            ConnectionFailure.PasswordMissing -> PasswordMissing
            is ConnectionFailure.Unexpected -> Unexpected
        }
    }
}
