package io.buszi.nascope.network

import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.ServerCredentials
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import io.ktor.client.statement.bodyAsText

internal sealed interface GlancesHttpResult {
    /** [body] is only read for a 200; it is empty for every other status. */
    data class Response(val status: HttpStatusCode, val body: String) : GlancesHttpResult
    data class Failed(val failure: ConnectionFailure) : GlancesHttpResult
}

/**
 * One GET shared by the Glances clients: timeouts, Basic auth, exception mapping and cancellation handling.
 * Ktor's request timeout is a [CancellationException]; any other cancellation propagates.
 */
internal suspend fun HttpClient.glancesGet(url: String, credentials: ServerCredentials?): GlancesHttpResult {
    val response = try {
        get(url) {
            timeout {
                connectTimeoutMillis = GLANCES_TIMEOUT_MILLIS
                requestTimeoutMillis = GLANCES_TIMEOUT_MILLIS
                socketTimeoutMillis = GLANCES_TIMEOUT_MILLIS
            }
            if (credentials != null) basicAuth(credentials.username, credentials.password)
        }
    } catch (e: CancellationException) {
        if (e.toConnectionFailureOrNull() == null) throw e
        return GlancesHttpResult.Failed(e.toConnectionFailure())
    } catch (e: Exception) {
        return GlancesHttpResult.Failed(e.toConnectionFailure())
    }
    if (response.status != HttpStatusCode.OK) return GlancesHttpResult.Response(response.status, "")
    val body = try {
        response.bodyAsText()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        return GlancesHttpResult.Failed(e.toConnectionFailure())
    }
    return GlancesHttpResult.Response(response.status, body)
}

private const val GLANCES_TIMEOUT_MILLIS = 5_000L

private fun CancellationException.toConnectionFailureOrNull(): ConnectionFailure? =
    toConnectionFailure().takeUnless { it is ConnectionFailure.Unexpected }
