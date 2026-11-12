package io.buszi.nascope.network

import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerCredentials
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.koin.core.annotation.Single

@Single
internal class KtorGlancesConnectionClient(
    private val client: HttpClient,
) : GlancesConnectionClient {

    override suspend fun fetchStatus(
        address: ServerAddress,
        apiVersion: GlancesApiVersion,
    ): ConnectionFailure? = call(address.apiUrl(apiVersion, STATUS_ENDPOINT), credentials = null) { body ->
        isGlancesStatus(body)
    }

    override suspend fun verifyAccess(
        address: ServerAddress,
        apiVersion: GlancesApiVersion,
        credentials: ServerCredentials?,
    ): ConnectionFailure? = call(address.apiUrl(apiVersion, PROTECTED_ENDPOINT), credentials) { body ->
        body.trimStart().startsWith("[")
    }

    /** One GET. A 200 only counts when [isGlancesBody] accepts the body. */
    private suspend fun call(
        url: String,
        credentials: ServerCredentials?,
        isGlancesBody: (String) -> Boolean,
    ): ConnectionFailure? = when (val result = client.glancesGet(url, credentials)) {
        is GlancesHttpResult.Failed -> result.failure
        is GlancesHttpResult.Response -> when (result.status) {
            HttpStatusCode.OK -> if (isGlancesBody(result.body)) null else ConnectionFailure.NotGlances
            HttpStatusCode.Unauthorized -> ConnectionFailure.Unauthorized
            HttpStatusCode.NotFound -> ConnectionFailure.NotGlances
            else -> ConnectionFailure.Unexpected("HTTP ${result.status.value}")
        }
    }

    private companion object {
        const val STATUS_ENDPOINT = "status"
        const val PROTECTED_ENDPOINT = "pluginslist"
    }
}

/**
 * Glances 4 answers `{"version": "..."}`, Glances 3 answers the text `Active`.
 */
private fun isGlancesStatus(body: String): Boolean {
    if (body.trim() == "Active") return true
    val json = try {
        Json.parseToJsonElement(body)
    } catch (_: Exception) {
        return false
    }
    return json is JsonObject && "version" in json.jsonObject
}
