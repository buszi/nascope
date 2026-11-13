package io.buszi.nascope.network

import io.buszi.nascope.domain.model.connection.ConnectionFailure
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

internal actual fun Throwable.platformConnectionFailure(): ConnectionFailure? = when (this) {
    is UnknownHostException -> ConnectionFailure.UnknownHost
    is ConnectException -> ConnectionFailure.ConnectionRefused
    is SocketTimeoutException -> ConnectionFailure.Timeout
    is UntrustedServerCertificateException -> toConnectionFailure()
    // The TLS stack wraps the trust manager's exception: an untrusted certificate is found among the causes.
    is SSLException -> untrustedCertificateCause()?.toConnectionFailure() ?: ConnectionFailure.TlsError
    else -> null
}

private fun Throwable.untrustedCertificateCause(): UntrustedServerCertificateException? =
    generateSequence(cause) { it.cause }
        .take(MAX_CAUSE_DEPTH)
        .filterIsInstance<UntrustedServerCertificateException>()
        .firstOrNull()

private const val MAX_CAUSE_DEPTH = 8
