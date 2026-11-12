package io.buszi.nascope.network

import com.github.michaelbull.result.getError
import com.github.michaelbull.result.getOrElse
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerConnection
import io.buszi.nascope.domain.model.connection.ServerCredentials
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.domain.model.telemetry.AlertState
import io.buszi.nascope.domain.model.telemetry.GpuUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.network.telemetry.Fixtures
import io.buszi.nascope.network.telemetry.assertInvalid
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
        Call("pluginslist", "pluginslist") { c, s -> c.fetchPlugins(s) },
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
        Call("alert", "alert") { c, s -> c.fetchAlerts(s) },
        Call("now", "now") { c, s -> c.fetchServerTime(s) },
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
    fun pluginsAreTheNamesInTheArray() = runTest {
        for ((version, dir) in versions() + (GlancesApiVersion.V4 to "v4-nas")) {
            val plugins = responding(HttpStatusCode.OK, Fixtures.read(dir, "pluginslist"))
                .client.fetchPlugins(connection(version)).getOrElse { fail("$dir: $it") }
            assertTrue("cpu" in plugins && "processlist" in plugins && "gpu" in plugins, dir)
        }
    }

    @Test
    fun pluginsSkipNonStringAndBlankElementsAndAnEmptyArrayIsAnEmptySet() = runTest {
        val skipped = responding(HttpStatusCode.OK, """["cpu", 3, null, "", "mem", {"a": 1}, "cpu"]""")
            .client.fetchPlugins(connection(GlancesApiVersion.V4)).getOrElse { fail("$it") }
        assertEquals(setOf("cpu", "mem"), skipped)
        val none = responding(HttpStatusCode.OK, "[]").client.fetchPlugins(connection(GlancesApiVersion.V4))
            .getOrElse { fail("$it") }
        assertTrue(none.isEmpty())
    }

    @Test
    fun pluginsRejectABodyThatIsNotAnArray() = runTest {
        for (body in listOf("""{"cpu": true}""", "not json", "")) {
            responding(HttpStatusCode.OK, body).client.fetchPlugins(connection(GlancesApiVersion.V3)).assertInvalid()
        }
    }

    @Test
    fun pluginsNotFoundMapsToNotGlancesAndUnauthorizedToUnauthorized() = runTest {
        assertEquals(
            TelemetryFailure.Connection(ConnectionFailure.NotGlances),
            responding(HttpStatusCode.NotFound).client.fetchPlugins(connection(GlancesApiVersion.V3)).getError(),
        )
        assertEquals(
            TelemetryFailure.Connection(ConnectionFailure.Unauthorized),
            responding(HttpStatusCode.Unauthorized).client.fetchPlugins(connection(GlancesApiVersion.V4)).getError(),
        )
    }

    private fun alertsOf(body: String) =
        responding(HttpStatusCode.OK, body).let { kotlinx.coroutines.runBlocking { it.client.fetchAlerts(connection(GlancesApiVersion.V4)) } }
            .getOrElse { fail("$it") }

    @Test
    fun alertsParseTheNasPayloadWithTwoActiveWarnings() {
        val alerts = alertsOf(Fixtures.read("v4-nas", "alert"))
        assertEquals(listOf("MEMSWAP", "MEM"), alerts.map { it.type })
        assertTrue(alerts.all { it.isActive && it.state == AlertState.Warning })
        val first = alerts.first()
        assertEquals(1794475982L, first.beginEpochSeconds)
        assertEquals("High swap (paging) usage", first.message)
        assertEquals(emptyList(), first.topProcesses)
        assertEquals(72.79, first.min!!, 0.01)
    }

    @Test
    fun alertsParseEndedUnknownAndIncompleteEntries() {
        val alerts = alertsOf(
            """[
              {"begin": 10, "end": 20, "state": "CRITICAL", "type": "CPU_USER", "top": ["ffmpeg", 5, null], "global_msg": ""},
              {"begin": 30, "end": -1, "state": "SOMETHING", "type": "NEW_METRIC"},
              {"begin": 40, "state": "WARNING"},
              {"state": "WARNING", "type": "LOAD"},
              "junk"
            ]""",
        )
        assertEquals(2, alerts.size)
        assertEquals(20L, alerts[0].endEpochSeconds)
        assertEquals(AlertState.Critical, alerts[0].state)
        assertEquals(listOf("ffmpeg"), alerts[0].topProcesses)
        assertEquals(null, alerts[0].message)
        assertEquals(AlertState.Unknown, alerts[1].state)
        assertEquals(null, alerts[1].endEpochSeconds)
        assertEquals("NEW_METRIC", alerts[1].type)
    }

    @Test
    fun alertsAnEmptyListIsNoAlerts() {
        assertEquals(emptyList(), alertsOf("[]"))
    }

    @Test
    fun alertsAnObjectIsInvalid() = runTest {
        responding(HttpStatusCode.OK, "{}").client.fetchAlerts(connection(GlancesApiVersion.V4)).assertInvalid()
    }

    @Test
    fun serverTimeIsReadFromTheIsoField() = runTest {
        val time = responding(HttpStatusCode.OK, Fixtures.read("v4-nas", "now"))
            .client.fetchServerTime(connection(GlancesApiVersion.V4)).getOrElse { fail("$it") }
        assertEquals(1794511447L, time.epochSeconds) // 2026-11-12T19:24:07Z of the capture
        responding(HttpStatusCode.OK, """{"iso": "nope"}""").client.fetchServerTime(connection(GlancesApiVersion.V4)).assertInvalid()
        responding(HttpStatusCode.OK, """{"custom": "x"}""").client.fetchServerTime(connection(GlancesApiVersion.V4)).assertInvalid()
    }

    @Test
    fun gpuParsesWhatAnIntegratedGpuReportsAndLeavesTheRestNull() = runTest {
        val body = """[{"key": "gpu_id", "gpu_id": "intel0", "name": "Intel GPU", "mem": null, "proc": 12.5,""" +
            """ "temperature": null, "fan_speed": null}]"""
        for ((version, _) in versions()) {
            val gpus = responding(HttpStatusCode.OK, body).client.fetchGpus(connection(version)).getOrElse { fail("$it") }

            assertEquals(
                listOf(GpuUsage("intel0", "Intel GPU", 12.5f, null, null, null)),
                gpus,
            )
        }
    }

    @Test
    fun gpuParsesADiscreteGpuAndFallsBackToTheIdWithoutAName() = runTest {
        val body = """[{"gpu_id": "nvidia0", "name": "RTX 3060", "mem": 40.0, "proc": 7, "temperature": 51, "fan_speed": 30},""" +
            """ {"gpu_id": "amd1", "proc": 1}, {"name": ""}, 5]"""
        val gpus = responding(HttpStatusCode.OK, body).client.fetchGpus(connection(GlancesApiVersion.V4))
            .getOrElse { fail("$it") }

        assertEquals(
            listOf(
                GpuUsage("nvidia0", "RTX 3060", 7f, 40f, 51f, 30f),
                GpuUsage("amd1", "amd1", 1f, null, null, null),
            ),
            gpus,
        )
    }

    @Test
    fun gpuIsAnEmptyListWhenTheHostHasNoGpu() = runTest {
        for (body in listOf("[]", "{}")) {
            val gpus = responding(HttpStatusCode.OK, body).client.fetchGpus(connection(GlancesApiVersion.V4))
                .getOrElse { fail("$it") }
            assertEquals(emptyList(), gpus)
        }
    }

    @Test
    fun gpuIsUnavailableWhenTheHostDoesNotKnowThePlugin() = runTest {
        val result = responding(HttpStatusCode.BadRequest).client.fetchGpus(connection(GlancesApiVersion.V4))

        assertEquals(TelemetryFailure.PluginUnavailable, result.getError())
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
