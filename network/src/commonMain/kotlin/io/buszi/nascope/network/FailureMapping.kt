package io.buszi.nascope.network

import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException

/** Maps a transport exception (searching its cause chain) to a domain failure. */
internal fun Throwable.toConnectionFailure(): ConnectionFailure {
    var current: Throwable? = this
    var depth = 0
    while (current != null && depth < MAX_CAUSE_DEPTH) {
        val failure = when (current) {
            is HttpRequestTimeoutException,
            is ConnectTimeoutException,
            is SocketTimeoutException,
            -> ConnectionFailure.Timeout

            else -> current.platformConnectionFailure()
        }
        if (failure != null) return failure
        current = current.cause
        depth++
    }
    return ConnectionFailure.Unexpected(message ?: this::class.simpleName.orEmpty())
}

/** Recognises the platform's networking exceptions: unknown host, refused, timeout, TLS. */
internal expect fun Throwable.platformConnectionFailure(): ConnectionFailure?

private const val MAX_CAUSE_DEPTH = 8
