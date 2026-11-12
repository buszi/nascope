package io.buszi.nascope.network.telemetry

import io.buszi.nascope.domain.model.telemetry.ContainerStatus
import io.buszi.nascope.domain.model.telemetry.SensorKind
import io.buszi.nascope.network.telemetry.v3.parseV3Containers
import io.buszi.nascope.network.telemetry.v3.parseV3DiskIo
import io.buszi.nascope.network.telemetry.v3.parseV3Network
import io.buszi.nascope.network.telemetry.v3.parseV3Processes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Glances 3.4.0.5 on real hardware (an Intel N100 NAS), anonymised. */
class NasV3FixturesTest {

    private fun read(plugin: String) = Fixtures.read("v3-nas", plugin)

    @Test
    fun systemUptimeLoadAndMemory() {
        assertEquals("6.18.15", parseSystem(read("system")).ok().osVersion)
        assertEquals(9.days + 4.hours + 16.minutes + 42.seconds, parseUptime(read("uptime")).ok())
        assertEquals(4, parseLoad(read("load")).ok().logicalCores)
        assertEquals(8059043840, parseMemory(read("mem")).ok().totalBytes)
        val swap = parseSwap(read("memswap")).ok()
        assertEquals(6173995008, swap.totalBytes)
        assertEquals(1894, parseProcessCount(read("processcount")).ok().threads)
        assertEquals(4, parseCpuCores(read("percpu")).ok().size)
        parseCpu(read("cpu")).ok()
        parseFileSystems(read("fs")).ok()
    }

    @Test
    fun sensorsAreAllTemperatureCore() {
        val sensors = parseSensors(read("sensors")).ok()
        assertEquals(8, sensors.size)
        assertTrue(sensors.all { it.kind == SensorKind.CpuTemperature && it.unit == "C" })
    }

    @Test
    fun diskIoRatesAreDeltaOverInterval() {
        val disks = parseV3DiskIo(read("diskio")).ok()
        assertEquals(31, disks.size)
        val sda = disks.first { it.diskName == "sda" }
        assertEquals((2216448 / 5.785326719284058).toLong(), sda.writeBytesPerSec)
        assertEquals(0, sda.readBytesPerSec)
    }

    @Test
    fun networkRatesAndTotals() {
        val network = parseV3Network(read("network")).ok()
        val eth0 = network.first { it.name == "eth0" }
        assertEquals((15482 / 5.785899639129639).toLong(), eth0.rxBytesPerSec)
        assertEquals(80332, eth0.totalRxBytes)
        assertEquals(true, eth0.isUp)
        assertEquals(10485760000, eth0.speedBitsPerSec)
    }

    @Test
    fun containersAndProcesses() {
        val containers = parseV3Containers(read("containers")).ok()
        assertEquals(23, containers.size)
        assertTrue(containers.all { it.status == ContainerStatus.Running })
        assertEquals(8059043840, containers.first().memoryLimitBytes)
        val processes = parseV3Processes(read("processlist")).ok()
        assertEquals(60, processes.size)
        assertTrue(processes.all { it.memoryRssBytes != null })
    }
}
