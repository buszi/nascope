package io.buszi.nascope.domain.telemetry

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerConnection
import io.buszi.nascope.domain.model.connection.ServerCredentials
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import io.buszi.nascope.domain.model.telemetry.LoadAverage
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.network.GlancesTelemetryClient
import io.buszi.nascope.storage.SecretStore
import io.buszi.nascope.test.TestCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiveTelemetrySourceTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val client = mockk<GlancesTelemetryClient>()
    private val secretStore = mockk<SecretStore>()
    private val address = ServerAddress(ServerProtocol.Https, "nas.local", 61208)
    private val id = ServerProfileId("p1")

    private fun source(auth: ServerAuth) = LiveTelemetrySource(
        ServerProfile(id, "nas", address, GlancesApiVersion.V3, auth),
        client, secretStore, coroutineRule.dispatcher,
    )

    @Test
    fun passesAddressVersionAndStoredSecret() = runTest {
        val captured = slot<ServerConnection>()
        coEvery { secretStore.get(id) } returns "pw"
        coEvery { client.fetchLoad(capture(captured)) } returns Ok(LoadAverage(1f, 1f, 1f, 4))

        source(ServerAuth.Basic("glances")).fetchLoad()

        assertEquals(address, captured.captured.address)
        assertEquals(GlancesApiVersion.V3, captured.captured.apiVersion)
        assertEquals(ServerCredentials("glances", "pw"), captured.captured.credentials)
    }

    @Test
    fun rereadsTheSecretOnEveryCall() = runTest {
        val captured = mutableListOf<ServerConnection>()
        coEvery { secretStore.get(id) } returnsMany listOf("old", "new")
        coEvery { client.fetchLoad(capture(captured)) } returns Ok(LoadAverage(1f, 1f, 1f, 4))
        val source = source(ServerAuth.Basic("glances"))

        source.fetchLoad()
        source.fetchLoad()

        assertEquals(listOf("old", "new"), captured.map { it.credentials?.password })
    }

    @Test
    fun sendsNoCredentialsWithoutAuth() = runTest {
        val captured = slot<ServerConnection>()
        coEvery { client.fetchLoad(capture(captured)) } returns Ok(LoadAverage(1f, 1f, 1f, 4))

        source(ServerAuth.None).fetchLoad()

        assertNull(captured.captured.credentials)
        coVerifyOrder(inverse = true) { secretStore.get(any()) }
    }

    @Test
    fun passesFailuresThrough() = runTest {
        coEvery { client.fetchSensors(any()) } returns Err(TelemetryFailure.PluginUnavailable)
        assertEquals(Err(TelemetryFailure.PluginUnavailable), source(ServerAuth.None).fetchSensors())
    }
}
