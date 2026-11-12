package io.buszi.nascope.domain.telemetry

import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.Result
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.days

class DemoTelemetrySourceTest {

    private fun source(seed: Int = 7) = DemoTelemetrySource(Random(seed))

    private fun <T> Result<T, TelemetryFailure>.ok(): T = getOrElse { fail("Demo source failed: $it") }

    @Test
    fun neverFailsAndAllFunctionsAnswer() = runTest {
        val s = source()
        repeat(50) {
            s.fetchSystem().ok(); s.fetchUptime().ok(); s.fetchLoad().ok(); s.fetchCpu().ok()
            s.fetchCpuCores().ok(); s.fetchCpuInfo().ok(); s.fetchMemory().ok(); s.fetchSwap().ok()
            s.fetchFileSystems().ok(); s.fetchDiskIo().ok(); s.fetchNetworkInterfaces().ok()
            s.fetchSensors().ok(); s.fetchContainers().ok(); s.fetchProcessCount().ok(); s.fetchProcesses().ok()
        }
    }

    @Test
    fun consecutiveCallsDiffer() = runTest {
        val s = source()
        assertNotEquals(s.fetchCpu().ok().totalPercent, s.fetchCpu().ok().totalPercent)
        assertNotEquals(s.fetchNetworkInterfaces().ok()[0].rxBytesPerSec, s.fetchNetworkInterfaces().ok()[0].rxBytesPerSec)
        assertNotEquals(s.fetchSensors().ok()[0].value, s.fetchSensors().ok()[0].value)
        assertNotEquals(s.fetchDiskIo().ok()[0].writeBytesPerSec, s.fetchDiskIo().ok()[0].writeBytesPerSec)
    }

    @Test
    fun sameSeedGivesSameValues() = runTest {
        assertEquals(source(3).fetchCpu().ok(), source(3).fetchCpu().ok())
    }

    @Test
    fun cpuStaysInBoundsAndAverageOfCoresMatchesTotal() = runTest {
        val s = source()
        repeat(500) {
            val cpu = s.fetchCpu().ok()
            assertTrue(cpu.totalPercent in 0f..100f)
            assertTrue(cpu.idlePercent!! in 0f..100f)
            assertEquals(100f, cpu.userPercent!! + cpu.systemPercent!! + cpu.ioWaitPercent!! + cpu.idlePercent!!, 0.01f)
            val cores = s.fetchCpuCores().ok()
            assertEquals(16, cores.size)
            assertTrue(cores.all { it.totalPercent in 0f..100f })
        }
    }

    @Test
    fun memoryAndSwapTotalsStayConsistent() = runTest {
        val s = source()
        repeat(500) {
            val mem = s.fetchMemory().ok()
            assertEquals(mem.totalBytes, mem.usedBytes + mem.freeBytes)
            assertTrue(mem.usedBytes > 0 && mem.freeBytes > 0)
            val swap = s.fetchSwap().ok()
            assertEquals(swap.totalBytes, swap.usedBytes + swap.freeBytes)
        }
    }

    @Test
    fun fileSystemsKeepTotalsAndCapacities() = runTest {
        val s = source()
        val first = s.fetchFileSystems().ok()
        repeat(500) { s.fetchFileSystems().ok() }
        val last = s.fetchFileSystems().ok()
        assertEquals(first.map { it.totalBytes }, last.map { it.totalBytes })
        assertTrue(last.all { it.usedBytes + it.freeBytes == it.totalBytes && it.usedBytes >= 0 && it.freeBytes > 0 })
    }

    @Test
    fun ratesAndTotalsBehave() = runTest {
        val s = source()
        var previous = s.fetchNetworkInterfaces().ok()
        repeat(200) {
            val now = s.fetchNetworkInterfaces().ok()
            assertTrue(now.all { it.rxBytesPerSec >= 0 && it.txBytesPerSec >= 0 })
            assertTrue(now.zip(previous).all { (n, p) -> n.totalRxBytes >= p.totalRxBytes && n.totalTxBytes >= p.totalTxBytes })
            assertTrue(s.fetchDiskIo().ok().all { it.readBytesPerSec >= 0 && it.writeBytesPerSec >= 0 })
            previous = now
        }
        assertEquals(false, previous.first { it.name == "wlan0" }.isUp)
    }

    @Test
    fun sensorsContainersAndProcessesStayPlausible() = runTest {
        val s = source()
        repeat(300) {
            assertTrue(s.fetchSensors().ok().all { it.value > 0f })
            s.fetchContainers().ok().forEach {
                assertTrue(it.memoryUsageBytes!! <= it.memoryLimitBytes!!)
                assertTrue(it.cpuPercent!! >= 0f)
            }
            val count = s.fetchProcessCount().ok()
            assertEquals(count.total, count.running + count.sleeping)
            assertTrue(s.fetchProcesses().ok().all { it.cpuPercent in 0f..100f && it.memoryPercent in 0f..100f })
        }
    }

    @Test
    fun uptimeStartsAtTheBaseline() = runTest {
        assertTrue(source().fetchUptime().ok() > 14.days)
    }
}
