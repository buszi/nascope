package io.buszi.nascope.network.telemetry

import io.buszi.nascope.domain.model.telemetry.SensorKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class CommonMappersTest {

    private val dirs = listOf("v4-docker", "v3-docker")

    @Test
    fun systemMapsFixtures() = dirs.forEach { dir ->
        val system = parseSystem(Fixtures.read(dir, "system")).ok()
        assertEquals("Linux", system.osName)
        assertEquals("7.0.12-linuxkit", system.osVersion)
        assertEquals("64bit", system.architecture)
        assertTrue(system.hostname.isNotEmpty())
        assertTrue(system.distribution!!.startsWith("Alpine"))
    }

    @Test
    fun systemToleratesMissingAndNullOptionalFields() {
        val system = parseSystem("""{"hostname":"h","os_name":"Linux","os_version":null,"extra":1}""").ok()
        assertNull(system.osVersion)
        assertNull(system.displayName)
    }

    @Test
    fun systemWithoutHostnameIsInvalid() = parseSystem("""{"os_name":"Linux"}""").assertInvalid()

    @Test
    fun uptimeParsesBothForms() {
        assertEquals(3.minutes + 14.seconds, parseUptime("\"0:03:14\"").ok())
        assertEquals(2.days + 4.hours + 5.minutes + 6.seconds, parseUptime("\"2 days, 4:05:06\"").ok())
        assertEquals(1.days + 1.seconds, parseUptime("\"1 day, 0:00:01\"").ok())
        dirs.forEach { assertEquals(8.minutes + 33.seconds, parseUptime(Fixtures.read(it, "uptime")).ok()) }
    }

    @Test
    fun uptimeRejectsWrongShapes() {
        parseUptime("{}").assertInvalid()
        parseUptime("\"soon\"").assertInvalid()
        parseUptime("not json").assertInvalid()
    }

    @Test
    fun loadMapsFixtures() = dirs.forEach { dir ->
        val load = parseLoad(Fixtures.read(dir, "load")).ok()
        assertEquals(0f, load.min1)
        assertEquals(12, load.logicalCores)
    }

    @Test
    fun loadWithoutCoresHasNullCores() =
        assertNull(parseLoad("""{"min1":1.5,"min5":1,"min15":0.5}""").ok().logicalCores)

    @Test
    fun cpuMapsFixtures() = dirs.forEach { dir ->
        val cpu = parseCpu(Fixtures.read(dir, "cpu")).ok()
        assertTrue(cpu.totalPercent in 0f..100f)
        assertEquals(12, cpu.logicalCores)
        assertTrue(cpu.idlePercent!! > 90f)
    }

    @Test
    fun cpuWithOnlyTotal() {
        val cpu = parseCpu("""{"total":12.5,"user":null}""").ok()
        assertEquals(12.5f, cpu.totalPercent)
        assertNull(cpu.userPercent)
    }

    @Test
    fun cpuWrongShapeAndMissingTotalAreInvalid() {
        parseCpu("[]").assertInvalid()
        parseCpu("""{"user":1}""").assertInvalid()
    }

    @Test
    fun cpuCoresMapFixtures() = dirs.forEach { dir ->
        val cores = parseCpuCores(Fixtures.read(dir, "percpu")).ok()
        assertEquals((0..11).toList(), cores.map { it.index })
    }

    @Test
    fun cpuCoresSkipBrokenElementsAndAcceptEmpty() {
        val cores = parseCpuCores("""[{"cpu_number":0,"total":5.0},{"cpu_number":"x"},{"total":1.0},3,{"cpu_number":1,"total":7}]""").ok()
        assertEquals(listOf(0, 1), cores.map { it.index })
        assertEquals(emptyList(), parseCpuCores("[]").ok())
        parseCpuCores("{}").assertInvalid()
    }

    @Test
    fun cpuInfoMapsFixtures() = dirs.forEach { dir ->
        val info = parseCpuInfo(Fixtures.read(dir, "quicklook")).ok()
        assertNull(info.currentFrequencyHz)
    }

    @Test
    fun cpuInfoReadsFrequencies() {
        val info = parseCpuInfo("""{"cpu_name":"X","cpu_hz_current":2400000000.0,"cpu_hz":3600000000}""").ok()
        assertEquals(2_400_000_000, info.currentFrequencyHz)
        assertEquals(3_600_000_000, info.maxFrequencyHz)
    }

    @Test
    fun memoryMapsFixtures() = dirs.forEach { dir ->
        val mem = parseMemory(Fixtures.read(dir, "mem")).ok()
        assertEquals(8319238144, mem.totalBytes)
        assertEquals(42708992, mem.buffersBytes)
        assertTrue(mem.usedBytes > 0 && mem.availableBytes!! > 0 && mem.cachedBytes!! > 0)
    }

    @Test
    fun memoryOptionalFieldsMayBeMissingButRequiredOnesMayNot() {
        assertNull(parseMemory("""{"total":10,"used":4,"free":6,"cached":null}""").ok().cachedBytes)
        parseMemory("""{"total":10,"used":4}""").assertInvalid()
    }

    @Test
    fun swapMapsFixtures() = dirs.forEach { dir ->
        val swap = parseSwap(Fixtures.read(dir, "memswap")).ok()
        assertEquals(1073737728, swap.totalBytes)
        assertEquals(0, swap.usedBytes)
    }

    @Test
    fun fileSystemsMapFixtures() {
        val v4 = parseFileSystems(Fixtures.read("v4-docker", "fs")).ok()
        assertEquals(3, v4.size)
        assertEquals("/etc/resolv.conf", v4.first().mountPoint)
        assertEquals("rw,relatime,discard", v4.first().options)
        assertEquals(977896124416, v4.first().totalBytes)

        val v3 = parseFileSystems(Fixtures.read("v3-docker", "fs")).ok()
        assertEquals(3, v3.size)
        assertNull(v3.first().options)
        assertEquals("ext4", v3.first().fsType)
    }

    @Test
    fun fileSystemsSkipItemsWithoutRequiredFields() {
        val list = parseFileSystems(
            """[{"mnt_point":"/","size":10,"used":4,"free":6},{"size":1,"used":1,"free":0},{"mnt_point":"/x","size":1}]""",
        ).ok()
        assertEquals(listOf("/"), list.map { it.mountPoint })
        parseFileSystems("""{"a":1}""").assertInvalid()
    }

    @Test
    fun processCountReadsThreadKey() {
        val v4 = parseProcessCount(Fixtures.read("v4-docker", "processcount")).ok()
        assertEquals(1, v4.total)
        assertEquals(7, v4.threads)
        assertEquals(3, parseProcessCount(Fixtures.read("v3-docker", "processcount")).ok().threads)
        parseProcessCount("[]").assertInvalid()
    }

    @Test
    fun dockerSensorsAreEmpty() = dirs.forEach {
        assertEquals(emptyList(), parseSensors(Fixtures.read(it, "sensors-docker")).ok())
    }

    @Test
    fun syntheticSensorsMapKinds() = dirs.forEach { dir ->
        val sensors = parseSensors(Fixtures.read(dir, "sensors-synthetic")).ok()
        assertEquals(
            listOf(SensorKind.CpuTemperature, SensorKind.DriveTemperature, SensorKind.FanSpeed, SensorKind.Battery),
            sensors.map { it.kind },
        )
        assertEquals(80f, sensors[0].warning)
        assertEquals("R", sensors[2].unit)
        assertNull(sensors[2].warning)
    }

    @Test
    fun sensorsUnknownTypeIsOtherAndBrokenItemsAreSkipped() {
        val sensors = parseSensors(
            """[{"label":"a","value":1,"type":"weird"},{"label":"b"},{"value":2},{"label":"c","value":"x"}]""",
        ).ok()
        assertEquals(listOf("a"), sensors.map { it.label })
        assertEquals(SensorKind.Other, sensors.single().kind)
    }
}
