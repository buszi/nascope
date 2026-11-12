package io.buszi.nascope.network

import com.github.michaelbull.result.getError
import com.github.michaelbull.result.getOrElse
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerConnection
import io.buszi.nascope.domain.model.connection.ServerCredentials
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.network.telemetry.Fixtures
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import java.net.ConnectException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class KtorGlancesTelemetryClientTest {

    private class Call(
        val plugin: String,
        val fixture: String,
        val invoke: suspend (GlancesTelemetryClient, ServerConnection) -> com.github.michaelbull.result.Result<*, TelemetryFailure>,
    )

    private val calls = listOf(
        Call("system", "system") { c, s -> c.fetchSystem(s) },
        Call("uptime", "uptime") { c, s -> c.fetchUptime(s) },
        Call("load", "load") { c, s -> c.fetchLoad(s) },
        Call("cpu", "cpu") { c, s -> c.fetchCpu(s) },
        Call("percpu", "percpu") { c, s -> c.fetchCpuCores(s) },
        Call("quicklook", "quicklook") { c, s -> c.fetchCpuInfo(s) },
        Call("mem", "mem") { c, s -> c.fetchMemory(s) },
        Call("memswap", "memswap") { c, s -> c.fetchSwap(s) },
        Call("fs", "fs") { c, s -> c.fetchFileSystems(s) },
        Call("diskio", "diskio") { c, s -> c.fetchDiskIo(s) },
        Call("network", "network") { c, s -> c.fetchNetworkInterfaces(s) },
        Call("sensors", "sensors-synthetic") { c, s -> c.fetchSensors(s) },
        Call("containers", "containers") { c, s -> c.fetchContainers(s) },
        Call("processcount", "processcount") { c, s -> c.fetchProcessCount(s) },
        Call("processlist", "processlist") { c, s -> c.fetchProcesses(s) },
    )

    private fun versions() = listOf(GlancesApiVersion.V3 to "v3-docker", GlancesApiVersion.V4 to "v4-docker")

    private class Harness(handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData) {
        val requests = mutableListOf<HttpRequestData>()
        val logLines = mutableListOf<String>()
        val client: GlancesTelemetryClient = KtorGlancesTelemetryClient(
            HttpClient(MockEngine { r -> requests += r; handler(r) }) { configureGlancesClient(
                object : io.ktor.client.plugins.logging.Logger {
                    override fun log(message: String) { logLines += message }
                },
            ) },
        )
    }

    private fun connection(version: GlancesApiVersion, credentials: ServerCredentials? = null) = ServerConnection(
        ServerAddress(ServerProtocol.Http, "nas.local", 61208),
        version,
        credentials,
    )

    private fun responding(status: HttpStatusCode, body: String = "") = Harness {
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    @Test
    fun everyFunctionCallsItsPluginPathAndParsesTheFixture() = runTest {
        for ((version, dir) in versions()) for (call in calls) {
            val harness = Harness { request ->
                respond(
                    Fixtures.read(dir, if (call.plugin == "sensors" && dir == "v4-docker") "sensors-docker" else call.fixture),
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val result = call.invoke(harness.client, connection(version))
            result.getError()?.let { fail("${call.plugin} $dir failed: $it") }
            assertEquals(
                "http://nas.local:61208/api/${version.pathSegment}/${call.plugin}",
                harness.requests.single().url.toString(),
            )
        }
    }

    @Test
    fun basicAuthIsSentOnlyWithCredentials() = runTest {
        val anonymous = responding(HttpStatusCode.OK, Fixtures.read("v4-docker", "load"))
        anonymous.client.fetchLoad(connection(GlancesApiVersion.V4))
        assertNull(anonymous.requests.single().headers[HttpHeaders.Authorization])

        val authed = responding(HttpStatusCode.OK, Fixtures.read("v4-docker", "load"))
        authed.client.fetchLoad(connection(GlancesApiVersion.V4, ServerCredentials("glances", "secret")))
        // base64("glances:secret")
        assertEquals("Basic Z2xhbmNlczpzZWNyZXQ=", authed.requests.single().headers[HttpHeaders.Authorization])
        assertTrue(authed.logLines.none { "secret" in it || "Z2xhbmNlczpzZWNyZXQ" in it })
    }

    @Test
    fun unauthorizedMapsToConnectionUnauthorized() = runTest {
        val result = responding(HttpStatusCode.Unauthorized).client.fetchCpu(connection(GlancesApiVersion.V4))
        assertEquals(TelemetryFailure.Connection(ConnectionFailure.Unauthorized), result.getError())
    }

    @Test
    fun badRequestMapsToPluginUnavailable() = runTest {
        for ((version, _) in versions()) {
            val result = responding(HttpStatusCode.BadRequest).client.fetchSensors(connection(version))
            assertEquals(TelemetryFailure.PluginUnavailable, result.getError())
        }
    }

    @Test
    fun notFoundMapsToNotGlancesAsTheApiVersionDoesNotExist() = runTest {
        for ((version, _) in versions()) {
            val result = responding(HttpStatusCode.NotFound).client.fetchCpu(connection(version))
            assertEquals(TelemetryFailure.Connection(ConnectionFailure.NotGlances), result.getError())
        }
    }

    @Test
    fun otherStatusesMapToUnexpected() = runTest {
        val result = responding(HttpStatusCode.InternalServerError).client.fetchCpu(connection(GlancesApiVersion.V4))
        assertEquals(TelemetryFailure.Connection(ConnectionFailure.Unexpected("HTTP 500")), result.getError())
    }

    @Test
    fun unreadableBodyIsInvalidResponse() = runTest {
        val result = responding(HttpStatusCode.OK, "<html>nope</html>").client.fetchCpu(connection(GlancesApiVersion.V4))
        assertIs<TelemetryFailure.InvalidResponse>(result.getError())
    }

    @Test
    fun transportFailuresAreMapped() = runTest {
        suspend fun failureOf(error: Throwable): TelemetryFailure? =
            Harness { throw error }.client.fetchCpu(connection(GlancesApiVersion.V4)).getError()

        assertEquals(
            TelemetryFailure.Connection(ConnectionFailure.UnknownHost),
            failureOf(UnknownHostException("nas.local")),
        )
        assertEquals(
            TelemetryFailure.Connection(ConnectionFailure.ConnectionRefused),
            failureOf(ConnectException("refused")),
        )
        assertEquals(
            TelemetryFailure.Connection(ConnectionFailure.TlsError),
            failureOf(SSLHandshakeException("bad cert")),
        )
        assertIs<TelemetryFailure.Connection>(failureOf(IllegalStateException("boom")))
    }

    @Test
    fun cancellationPropagates() = runTest {
        assertFailsWith<CancellationException> {
            Harness { throw CancellationException("cancelled") }.client.fetchCpu(connection(GlancesApiVersion.V4))
        }
    }

    @Test
    fun requestTimeoutIsMappedNotPropagated() = runTest {
        val result = Harness { throw HttpRequestTimeoutException("url", 5_000) }.client.fetchCpu(connection(GlancesApiVersion.V4))
        assertEquals(TelemetryFailure.Connection(ConnectionFailure.Timeout), result.getError())
    }
}
