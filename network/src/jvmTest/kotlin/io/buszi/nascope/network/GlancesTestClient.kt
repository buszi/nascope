package io.buszi.nascope.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

/** A Glances client wired to a [MockEngine] that records requests and log lines. */
internal class GlancesTestClient(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
) {
    val requests = mutableListOf<HttpRequestData>()
    val logLines = mutableListOf<String>()

    val client = KtorGlancesConnectionClient(
        HttpClient(
            MockEngine { request ->
                requests += request
                handler(request)
            },
        ) {
            configureGlancesClient(
                object : Logger {
                    override fun log(message: String) {
                        logLines += message
                    }
                },
            )
        },
    )

    companion object {
        fun responding(status: HttpStatusCode, body: String = "") = GlancesTestClient {
            respond(body, status, headersOf(HttpHeaders.ContentType, "text/plain"))
        }

        fun failing(error: Throwable) = GlancesTestClient { throw error }
    }
}
