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
    is SSLException -> ConnectionFailure.TlsError
    else -> null
}
