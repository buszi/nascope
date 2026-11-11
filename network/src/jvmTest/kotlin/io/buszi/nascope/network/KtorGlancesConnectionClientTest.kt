package io.buszi.nascope.network

import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerCredentials
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KtorGlancesConnectionClientTest {

    // region fetchStatus

    @Test
    fun fetchStatusAcceptsGlances4JsonStatus() = runTest {
        val test = GlancesTestClient.responding(HttpStatusCode.OK, """{"version": "4.5.9"}""")
        assertNull(test.client.fetchStatus(ADDRESS, GlancesApiVersion.V4))
    }

    @Test
    fun fetchStatusAcceptsGlances3ActiveText() = runTest {
        val test = GlancesTestClient.responding(HttpStatusCode.OK, "Active")
        assertNull(test.client.fetchStatus(ADDRESS, GlancesApiVersion.V3))
    }

    @Test
    fun fetchStatusRejectsOtherBodiesAsNotGlances() = runTest {
        assertEquals(
            ConnectionFailure.NotGlances,
            GlancesTestClient.responding(HttpStatusCode.OK, "<html>hello</html>")
                .client.fetchStatus(ADDRESS, GlancesApiVersion.V4),
        )
        assertEquals(
            ConnectionFailure.NotGlances,
            GlancesTestClient.responding(HttpStatusCode.OK, """{"other": 1}""")
                .client.fetchStatus(ADDRESS, GlancesApiVersion.V4),
        )
    }

    @Test
    fun fetchStatusUsesVersionedPathWithoutAuthorization() = runTest {
        val test = GlancesTestClient.responding(HttpStatusCode.OK, "Active")

        test.client.fetchStatus(ADDRESS, GlancesApiVersion.V4)
        test.client.fetchStatus(ADDRESS, GlancesApiVersion.V3)

        assertEquals(listOf("/api/4/status", "/api/3/status"), test.requests.map { it.url.encodedPath })
        assertTrue(test.requests.all { HttpHeaders.Authorization !in it.headers })
    }

    @Test
    fun fetchStatusMapsHttpStatuses() = runTest {
        assertEquals(ConnectionFailure.NotGlances, status(HttpStatusCode.NotFound))
        assertEquals(ConnectionFailure.Unauthorized, status(HttpStatusCode.Unauthorized))
        assertEquals(ConnectionFailure.Unexpected("HTTP 500"), status(HttpStatusCode.InternalServerError))
        assertEquals(ConnectionFailure.Unexpected("HTTP 301"), status(HttpStatusCode.MovedPermanently))
    }

    @Test
    fun fetchStatusMapsTransportErrors() = runTest {
        assertEquals(ConnectionFailure.UnknownHost, statusFailing(UnknownHostException("nope")))
        assertEquals(ConnectionFailure.ConnectionRefused, statusFailing(ConnectException("refused")))
        assertEquals(ConnectionFailure.Timeout, statusFailing(SocketTimeoutException("slow")))
        assertEquals(ConnectionFailure.Timeout, statusFailing(HttpRequestTimeoutException("url", 5_000)))
        assertEquals(ConnectionFailure.TlsError, statusFailing(SSLHandshakeException("bad cert")))
    }

    @Test
    fun transportErrorsAreFoundInTheCauseChain() = runTest {
        assertEquals(
            ConnectionFailure.UnknownHost,
            statusFailing(IOException("wrapper", UnknownHostException("nope"))),
        )
    }

    @Test
    fun unknownExceptionsBecomeUnexpected() = runTest {
        assertEquals(ConnectionFailure.Unexpected("boom"), statusFailing(IllegalStateException("boom")))
    }

    @Test
    fun cancellationIsNotSwallowed() = runTest {
        val test = GlancesTestClient.failing(CancellationException("cancelled"))
        assertFailsWith<CancellationException> { test.client.fetchStatus(ADDRESS, GlancesApiVersion.V4) }
    }

    // endregion

    // region verifyAccess

    @Test
    fun verifyAccessSendsBasicAuthOnlyWhenCredentialsGiven() = runTest {
        val test = GlancesTestClient.responding(HttpStatusCode.OK, "[\"cpu\"]")

        test.client.verifyAccess(ADDRESS, GlancesApiVersion.V4, credentials = null)
        test.client.verifyAccess(ADDRESS, GlancesApiVersion.V4, ServerCredentials("glances", "secret"))

        assertFalse(HttpHeaders.Authorization in test.requests[0].headers)
        // base64("glances:secret")
        assertEquals("Basic Z2xhbmNlczpzZWNyZXQ=", test.requests[1].headers[HttpHeaders.Authorization])
    }

    @Test
    fun verifyAccessUsesProtectedEndpointPerVersion() = runTest {
        val test = GlancesTestClient.responding(HttpStatusCode.OK, "[]")

        test.client.verifyAccess(ADDRESS, GlancesApiVersion.V4, null)
        test.client.verifyAccess(ADDRESS, GlancesApiVersion.V3, null)

        assertEquals(listOf("/api/4/pluginslist", "/api/3/pluginslist"), test.requests.map { it.url.encodedPath })
    }

    @Test
    fun verifyAccessMapsResults() = runTest {
        assertNull(access(HttpStatusCode.OK, "[\"cpu\", \"mem\"]"))
        assertEquals(ConnectionFailure.NotGlances, access(HttpStatusCode.OK, "<html/>"))
        assertEquals(ConnectionFailure.Unauthorized, access(HttpStatusCode.Unauthorized))
        assertEquals(ConnectionFailure.NotGlances, access(HttpStatusCode.NotFound))
        assertEquals(ConnectionFailure.Unexpected("HTTP 503"), access(HttpStatusCode.ServiceUnavailable))
    }

    @Test
    fun verifyAccessMapsTransportErrors() = runTest {
        val test = GlancesTestClient.failing(ConnectException("refused"))
        assertEquals(
            ConnectionFailure.ConnectionRefused,
            test.client.verifyAccess(ADDRESS, GlancesApiVersion.V4, null),
        )
    }

    // endregion

    @Test
    fun credentialsNeverAppearInLogs() = runTest {
        val test = GlancesTestClient.responding(HttpStatusCode.OK, "[]")

        test.client.verifyAccess(ADDRESS, GlancesApiVersion.V4, ServerCredentials("glances", "hunter2-secret"))

        val log = test.logLines.joinToString("\n")
        assertTrue(log.isNotBlank(), "expected the request to be logged")
        assertFalse("hunter2-secret" in log)
        assertFalse("Z2xhbmNlczpodW50ZXIyLXNlY3JldA==" in log)
        assertFalse("Authorization" in log)
    }

    @Test
    fun redirectsAreNotFollowed() = runTest {
        val test = GlancesTestClient.responding(HttpStatusCode.Found)
        assertIs<ConnectionFailure.Unexpected>(test.client.fetchStatus(ADDRESS, GlancesApiVersion.V4))
        assertEquals(1, test.requests.size)
    }

    private suspend fun status(code: HttpStatusCode) =
        GlancesTestClient.responding(code).client.fetchStatus(ADDRESS, GlancesApiVersion.V4)

    private suspend fun statusFailing(error: Throwable) =
        GlancesTestClient.failing(error).client.fetchStatus(ADDRESS, GlancesApiVersion.V4)

    private suspend fun access(code: HttpStatusCode, body: String = "") =
        GlancesTestClient.responding(code, body).client.verifyAccess(ADDRESS, GlancesApiVersion.V4, null)

    private companion object {
        val ADDRESS = ServerAddress(ServerProtocol.Http, "nas.local", 61208)
    }
}
