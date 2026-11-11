package io.buszi.nascope.network

import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerProtocol
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerAddressUrlTest {

    @Test
    fun buildsHttpUrlWithPortAndVersion() {
        val url = ServerAddress(ServerProtocol.Http, "nas.local", 61208).apiUrl(GlancesApiVersion.V4, "status")
        assertEquals("http://nas.local:61208/api/4/status", url)
    }

    @Test
    fun buildsHttpsUrlForV3() {
        val url = ServerAddress(ServerProtocol.Https, "10.0.0.2", 8443).apiUrl(GlancesApiVersion.V3, "pluginslist")
        assertEquals("https://10.0.0.2:8443/api/3/pluginslist", url)
    }

    @Test
    fun bracketsIpv6Literal() {
        val url = ServerAddress(ServerProtocol.Http, "fe80::1", 61208).apiUrl(GlancesApiVersion.V4, "status")
        assertEquals("http://[fe80::1]:61208/api/4/status", url)
    }

    @Test
    fun doesNotBracketTwice() {
        val url = ServerAddress(ServerProtocol.Http, "[::1]", 61208).apiUrl(GlancesApiVersion.V4, "status")
        assertEquals("http://[::1]:61208/api/4/status", url)
    }
}
