package io.buszi.nascope.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json

/** Creates the app's HTTP client on this platform's engine (OkHttp). */
internal fun createPlatformHttpClient(logger: Logger = NapierLogger): HttpClient =
    createPlatformHttpClient { configureGlancesClient(logger) }

/** Routes Ktor's log lines to Napier, so the app decides where they go. */
internal object NapierLogger : Logger {
    override fun log(message: String) {
        Napier.d(message, tag = "Network")
    }
}

internal expect fun createPlatformHttpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient

internal fun HttpClientConfig<*>.configureGlancesClient(logger: Logger) {
    // Redirects are not followed: they could carry the Authorization header to another host.
    followRedirects = false

    install(HttpTimeout)
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(Logging) {
        this.logger = logger
        // Request line and status only; headers and bodies are never logged, so credentials cannot leak.
        level = LogLevel.INFO
        sanitizeHeader { header -> header.equals(HttpHeaders.Authorization, ignoreCase = true) }
    }
}
