package io.buszi.nascope.network.telemetry

import io.buszi.nascope.domain.model.telemetry.ContainerStatus
import io.buszi.nascope.domain.model.telemetry.SensorKind
import io.buszi.nascope.network.telemetry.v4.parseV4Containers
import io.buszi.nascope.network.telemetry.v4.parseV4DiskIo
import io.buszi.nascope.network.telemetry.v4.parseV4Network
import io.buszi.nascope.network.telemetry.v4.parseV4Processes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Glances 4.5.9 on real hardware (an Intel N100 NAS), anonymised. */
class NasFixturesTest {

    private fun read(plugin: String) = Fixtures.read("v4-nas", plugin)

    @Test
    fun systemAndUptime() {
        val system = parseSystem(read("system")).ok()
        assertEquals("Debian GNU/Linux 12", system.distribution)
        assertEquals("6.18.15", system.osVersion)
        assertEquals(9.days + 3.hours + 59.minutes + 30.seconds, parseUptime(read("uptime")).ok())
    }

    @Test
    fun cpuFrequenciesAreInHz() {
        val info = parseCpuInfo(read("quicklook")).ok()
        assertEquals("Intel(R) N100", info.name)
        assertEquals(3_400_000_000, info.maxFrequencyHz)
        assertTrue(info.currentFrequencyHz!! in 400_000_000..4_000_000_000)
    }

    @Test
    fun cpuLoadMemoryAndSwap() {
        assertEquals(17.5f, parseCpu(read("cpu")).ok().totalPercent)
        assertEquals(4, parseCpuCores(read("percpu")).ok().size)
        assertEquals(4, parseLoad(read("load")).ok().logicalCores)
        val mem = parseMemory(read("mem")).ok()
        assertEquals(8059043840, mem.totalBytes)
        assertEquals(2262654976, mem.availableBytes)
        parseSwap(read("memswap")).ok()
        val count = parseProcessCount(read("processcount")).ok()
        assertEquals(458, count.total)
        assertEquals(1828, count.threads)
    }

    @Test
    fun sensorsAreAllTemperatureCoreAndKeepThresholds() {
        val sensors = parseSensors(read("sensors")).ok()
        assertEquals(8, sensors.size)
        assertTrue(sensors.all { it.kind == SensorKind.CpuTemperature && it.unit == "C" })
        val package0 = sensors.first { it.label == "Package id 0" }
        assertEquals(65f, package0.value)
        assertEquals(105f, package0.warning)
        assertNull(sensors.first { it.label == "acpitz 0" }.warning)
    }

    @Test
    fun fileSystemsDiskIoAndNetwork() {
        assertTrue(parseFileSystems(read("fs")).ok().isNotEmpty())
        val disks = parseV4DiskIo(read("diskio")).ok()
        assertEquals(31, disks.size)
        assertTrue(disks.any { it.diskName == "nvme0n1" })
        val network = parseV4Network(read("network")).ok()
        assertEquals(listOf("lo", "eth0"), network.map { it.name })
        assertEquals(10485760000, network.last().speedBitsPerSec)
    }

    @Test
    fun containersAcceptHealthStatesAndEmptyImages() {
        val containers = parseV4Containers(read("containers")).ok()
        assertEquals(23, containers.size)
        assertTrue(containers.all { it.status == ContainerStatus.Running })
        assertTrue(containers.any { it.image == null })
        assertTrue(containers.any { it.image != null })
        assertEquals("8 hours", containers.first().uptimeText)
    }

    @Test
    fun processesMapRealList() {
        val processes = parseV4Processes(read("processlist")).ok()
        assertEquals(60, processes.size)
        assertTrue(processes.all { it.memoryRssBytes != null })
    }
}
