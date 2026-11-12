package io.buszi.nascope.network.telemetry

import io.buszi.nascope.domain.model.telemetry.ContainerStatus
import io.buszi.nascope.network.telemetry.v3.parseV3Containers
import io.buszi.nascope.network.telemetry.v3.parseV3DiskIo
import io.buszi.nascope.network.telemetry.v3.parseV3Network
import io.buszi.nascope.network.telemetry.v3.parseV3Processes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class V3MappersTest {

    @Test
    fun diskIoMapsFixture() {
        val disks = parseV3DiskIo(Fixtures.read("v3-docker", "diskio")).ok()
        assertEquals(19, disks.size)
        assertEquals(0, disks.first().writeBytesPerSec)
    }

    @Test
    fun diskIoRateIsDeltaOverInterval() {
        val disk = parseV3DiskIo("""[{"disk_name":"sda","read_bytes":2000,"write_bytes":500,"time_since_update":2.0}]""").ok().single()
        assertEquals(1000, disk.readBytesPerSec)
        assertEquals(250, disk.writeBytesPerSec)
    }

    @Test
    fun rateIsZeroWhenIntervalIsZeroOrMissing() {
        val zero = parseV3DiskIo("""[{"disk_name":"a","read_bytes":2000,"write_bytes":5,"time_since_update":0}]""").ok().single()
        assertEquals(0, zero.readBytesPerSec)
        val missing = parseV3Network("""[{"interface_name":"a","rx":100,"tx":100}]""").ok().single()
        assertEquals(0, missing.rxBytesPerSec)
        assertEquals(0, missing.txBytesPerSec)
    }

    @Test
    fun networkMapsFixture() {
        val interfaces = parseV3Network(Fixtures.read("v3-docker", "network")).ok()
        val lo = interfaces.first { it.name == "lo" }
        assertNull(lo.speedBitsPerSec)
        assertEquals(true, lo.isUp)
        val eth0 = interfaces.first { it.name == "eth0" }
        assertEquals(10485760000, eth0.speedBitsPerSec)
        assertEquals(7416 / 3.6372125148773193, eth0.rxBytesPerSec.toDouble(), 1.0)
        assertEquals(10959, eth0.totalRxBytes)
        assertEquals(true, eth0.isUp)
    }

    @Test
    fun networkSkipsBrokenItemsAndRejectsWrongShape() {
        assertEquals(1, parseV3Network("""[{"interface_name":"a"},{"rx":1},7]""").ok().size)
        assertEquals(emptyList(), parseV3Network("[]").ok())
        parseV3Network("""{"x":1}""").assertInvalid()
    }

    @Test
    fun containersMapFixtureFromWrappedObject() {
        val containers = parseV3Containers(Fixtures.read("v3-docker", "containers")).ok()
        assertEquals(listOf("g4", "g3", "sample"), containers.map { it.name })
        val first = containers.first()
        assertEquals(ContainerStatus.Running, first.status)
        assertEquals("nicolargo/glances:latest-full", first.image)
        assertEquals(8319238144, first.memoryLimitBytes)
        assertEquals(177045504, first.memoryUsageBytes)
        assertEquals("34 secs", first.uptimeText)
        assertTrue(first.id.startsWith("dbf2f0"))
    }

    @Test
    fun containersEmptySkipAndWrongShape() {
        assertEquals(emptyList(), parseV3Containers("""{"version":{},"containers":[]}""").ok())
        val list = parseV3Containers("""{"containers":[{"Id":"1","name":"ok","Status":"weird"},{"name":"noid"}]}""").ok()
        assertEquals(ContainerStatus.Unknown, list.single().status)
        parseV3Containers("[]").assertInvalid()
        parseV3Containers("""{"other":1}""").assertInvalid()
    }

    @Test
    fun processesReadRssFromArray() {
        val process = parseV3Processes(Fixtures.read("v3-docker", "processlist")).ok().single()
        assertEquals("python3", process.name)
        assertEquals(39620608, process.memoryRssBytes)
        assertEquals("R", process.statusCode)
    }

    @Test
    fun processesTolerateUnexpectedMemoryInfoShape() {
        val list = parseV3Processes("""[{"pid":1,"name":"a","memory_info":{"rss":5}},{"pid":2,"name":"b","memory_info":[]},{"pid":3,"name":"c"}]""").ok()
        assertEquals(3, list.size)
        assertTrue(list.all { it.memoryRssBytes == null })
        parseV3Processes("{}").assertInvalid()
    }
}
