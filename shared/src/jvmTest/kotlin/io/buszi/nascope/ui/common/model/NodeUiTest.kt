package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.domain.model.connection.ServerProtocol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NodeUiTest {

    @Test
    fun nodeIsMappedFromProfile() {
        assertEquals(
            NodeUi(
                id = "alpha",
                alias = "Alpha",
                host = "alpha.local",
                port = 61208,
                protocol = ServerProtocolUi.Http,
            ),
            NodeUi.from(alpha),
        )
    }

    @Test
    fun addressJoinsHostAndPort() {
        assertEquals("alpha.local:61208", NodeUi.from(alpha).address)
    }

    @Test
    fun addressBracketsIpv6Hosts() {
        assertEquals("[fe80::1]:8443", NodeUi.from(ipv6).address)
        assertEquals("[::1]:8443", NodeUi.from(ipv6.copy(address = ipv6.address.copy(host = "[::1]"))).address)
    }

    @Test
    fun statusIsOnlyKeptForTheActiveNode() {
        assertEquals(ConnectionStatusUi.Online, NodeUi.from(alpha, isActive = true, connectionStatus = ConnectionStatusUi.Online).connectionStatus)
        assertNull(NodeUi.from(alpha, isActive = false, connectionStatus = ConnectionStatusUi.Online).connectionStatus)
    }

    @Test
    fun passwordIsMissingOnlyForThePasswordMissingStatus() {
        val missing = NodeUi.from(alpha, isActive = true, connectionStatus = ConnectionStatusUi.PasswordMissing)
        val offline = NodeUi.from(alpha, isActive = true, connectionStatus = ConnectionStatusUi.Offline)

        assertEquals(true, missing.isPasswordMissing)
        assertEquals(false, offline.isPasswordMissing)
        assertEquals(false, NodeUi.from(alpha).isPasswordMissing)
    }

    @Test
    fun onlyTheProfileWithTheActiveIdIsMarkedActive() {
        val nodes = NodeUi.from(listOf(alpha, ipv6), alpha.id, ConnectionStatusUi.Offline)

        assertEquals(listOf(true, false), nodes.map { it.isActive })
        assertEquals(listOf<ConnectionStatusUi?>(ConnectionStatusUi.Offline, null), nodes.map { it.connectionStatus })
    }

    @Test
    fun noProfileIsActiveWithoutAnActiveId() {
        assertEquals(listOf(false, false), NodeUi.from(listOf(alpha, ipv6), null, null).map { it.isActive })
    }

    companion object {
        private val alpha = ServerProfile(
            id = ServerProfileId("alpha"),
            alias = "Alpha",
            address = ServerAddress(ServerProtocol.Http, "alpha.local", 61208),
            apiVersion = GlancesApiVersion.V4,
            auth = ServerAuth.None,
        )
        private val ipv6 = alpha.copy(
            id = ServerProfileId("ipv6"),
            address = ServerAddress(ServerProtocol.Https, "fe80::1", 8443),
        )
    }
}
