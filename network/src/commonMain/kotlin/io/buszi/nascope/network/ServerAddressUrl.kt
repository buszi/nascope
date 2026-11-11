package io.buszi.nascope.network

import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerProtocol

internal fun ServerAddress.apiUrl(apiVersion: GlancesApiVersion, endpoint: String): String {
    val scheme = when (protocol) {
        ServerProtocol.Http -> "http"
        ServerProtocol.Https -> "https"
    }
    val authority = if (':' in host && !host.startsWith("[")) "[$host]" else host
    return "$scheme://$authority:$port/api/${apiVersion.pathSegment}/$endpoint"
}
