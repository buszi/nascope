package io.buszi.nascope.network.telemetry

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal val GlancesJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

internal fun invalid(detail: String): Result<Nothing, TelemetryFailure> =
    Err(TelemetryFailure.InvalidResponse(detail))

internal fun parseRoot(body: String): Result<JsonElement, TelemetryFailure> =
    try {
        Ok(GlancesJson.parseToJsonElement(body))
    } catch (e: Exception) {
        invalid("Body is not JSON: ${e.message}")
    }

/** Decodes a JSON object into [D] and maps it. A mapper returning null means a required field is missing. */
internal fun <D, M : Any> parseObject(
    body: String,
    serializer: KSerializer<D>,
    map: (D) -> M?,
): Result<M, TelemetryFailure> {
    val root = parseRoot(body).getOrElse { return Err(it) }
    if (root !is JsonObject) return invalid("Expected a JSON object")
    return decodeObject(root, serializer, map)
}

internal fun <D, M : Any> decodeObject(
    root: JsonObject,
    serializer: KSerializer<D>,
    map: (D) -> M?,
): Result<M, TelemetryFailure> {
    val dto = try {
        GlancesJson.decodeFromJsonElement(serializer, root)
    } catch (e: Exception) {
        return invalid("Cannot decode object: ${e.message}")
    }
    val model = map(dto) ?: return invalid("Required field missing")
    return Ok(model)
}

/** Decodes a JSON array element by element: elements that fail to decode or map are skipped. */
internal fun <D, M : Any> parseList(
    body: String,
    serializer: KSerializer<D>,
    map: (D) -> M?,
): Result<List<M>, TelemetryFailure> {
    val root = parseRoot(body).getOrElse { return Err(it) }
    if (root !is JsonArray) return invalid("Expected a JSON array")
    return Ok(decodeItems(root, serializer, map))
}

internal fun <D, M : Any> decodeItems(
    array: JsonArray,
    serializer: KSerializer<D>,
    map: (D) -> M?,
): List<M> = array.mapNotNull { element ->
    try {
        map(GlancesJson.decodeFromJsonElement(serializer, element))
    } catch (e: Exception) {
        null
    }
}

/** Rate for v3 payloads that carry a per-interval delta; 0 when the interval is unknown. */
internal fun ratePerSecond(delta: Long?, timeSinceUpdate: Double?): Long {
    if (delta == null || timeSinceUpdate == null || timeSinceUpdate <= 0.0) return 0
    return (delta / timeSinceUpdate).toLong()
}
