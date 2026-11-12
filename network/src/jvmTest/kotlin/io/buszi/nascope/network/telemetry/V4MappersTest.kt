package io.buszi.nascope.network.telemetry

import io.buszi.nascope.domain.model.telemetry.ContainerStatus
import io.buszi.nascope.network.telemetry.v4.parseV4Containers
import io.buszi.nascope.network.telemetry.v4.parseV4DiskIo
import io.buszi.nascope.network.telemetry.v4.parseV4Network
import io.buszi.nascope.network.telemetry.v4.parseV4Processes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class V4MappersTest {

    @Test
    fun diskIoMapsFixture() {
        val disks = parseV4DiskIo(Fixtures.read("v4-docker", "diskio")).ok()
        assertEquals(19, disks.size)
        assertEquals("nbd0", disks.first().diskName)
        assertEquals(0, disks.first().readBytesPerSec)
    }

    @Test
    fun diskIoUsesRatesAndSkipsBrokenItems() {
        val disks = parseV4DiskIo(
            """[{"disk_name":"sda","read_bytes_rate_per_sec":1500.7,"write_bytes_rate_per_sec":null},{"read_bytes_rate_per_sec":1},5]""",
        ).ok()
        assertEquals(1, disks.size)
        assertEquals(1500, disks.single().readBytesPerSec)
        assertEquals(0, disks.single().writeBytesPerSec)
        assertEquals(emptyList(), parseV4DiskIo("[]").ok())
        parseV4DiskIo("{}").assertInvalid()
    }

    @Test
    fun networkMapsFixture() {
        val interfaces = parseV4Network(Fixtures.read("v4-docker", "network")).ok()
        val lo = interfaces.first { it.name == "lo" }
        assertNull(lo.speedBitsPerSec) // speed 0 -> unknown
        val eth0 = interfaces.first { it.name == "eth0" }
        assertEquals(10485760000, eth0.speedBitsPerSec)
        assertEquals(1119, eth0.rxBytesPerSec)
        assertEquals(5815, eth0.txBytesPerSec)
        assertEquals(13186, eth0.totalRxBytes)
        assertNull(eth0.isUp)
    }

    @Test
    fun networkToleratesMissingFields() {
        val item = parseV4Network("""[{"interface_name":"x","speed":null,"unknown":true}]""").ok().single()
        assertEquals(0, item.rxBytesPerSec)
        assertNull(item.speedBitsPerSec)
    }

    @Test
    fun containersMapFixture() {
        val containers = parseV4Containers(Fixtures.read("v4-docker", "containers")).ok()
        assertEquals(listOf("g4", "g3", "sample"), containers.map { it.name })
        val first = containers.first()
        assertEquals(ContainerStatus.Running, first.status)
        assertEquals("nicolargo/glances:latest-full", first.image)
        assertEquals("docker", first.engine)
        assertEquals(8319238144, first.memoryLimitBytes)
        assertEquals("33 secs", first.uptimeText)
    }

    @Test
    fun containerStatusStrings() {
        fun status(s: String) =
            parseV4Containers("""[{"id":"1","name":"n","status":"$s"}]""").ok().single().status
        assertEquals(ContainerStatus.Paused, status("paused"))
        assertEquals(ContainerStatus.Restarting, status("restarting"))
        assertEquals(ContainerStatus.Exited, status("exited"))
        assertEquals(ContainerStatus.Created, status("created"))
        assertEquals(ContainerStatus.Dead, status("dead"))
        assertEquals(ContainerStatus.Running, status("Running"))
        assertEquals(ContainerStatus.Unknown, status("removing"))
    }

    @Test
    fun containersSkipBrokenAndAcceptEmpty() {
        val list = parseV4Containers("""[{"id":"1","name":"ok","image":[]},{"name":"noid"},{"id":"2","name":"bad","image":"str"}]""").ok()
        assertEquals(listOf("ok"), list.map { it.name })
        assertNull(list.single().image)
        assertEquals(emptyList(), parseV4Containers("[]").ok())
        parseV4Containers("""{"containers":[]}""").assertInvalid()
    }

    @Test
    fun processesMapFixture() {
        val process = parseV4Processes(Fixtures.read("v4-docker", "processlist")).ok().single()
        assertEquals(1, process.pid)
        assertEquals("python3.14", process.name)
        assertEquals(113848320, process.memoryRssBytes)
        assertEquals("S", process.statusCode)
        assertEquals(listOf("/venv/bin/python3.14", "-m", "glances", "-w"), process.commandLine)
    }

    @Test
    fun processesSkipItemsWithoutPidOrName() {
        val list = parseV4Processes("""[{"pid":2,"name":"a","cpu_percent":null},{"pid":3},{"name":"b"}]""").ok()
        assertEquals(1, list.size)
        assertEquals(0f, list.single().cpuPercent)
        assertNull(list.single().memoryRssBytes)
        parseV4Processes("not json").assertInvalid()
    }
}
