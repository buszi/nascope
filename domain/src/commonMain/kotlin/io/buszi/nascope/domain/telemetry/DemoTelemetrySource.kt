package io.buszi.nascope.domain.telemetry

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.buszi.nascope.domain.model.telemetry.ContainerStatus
import io.buszi.nascope.domain.model.telemetry.ContainerUsage
import io.buszi.nascope.domain.model.telemetry.CpuCoreUsage
import io.buszi.nascope.domain.model.telemetry.CpuInfo
import io.buszi.nascope.domain.model.telemetry.CpuUsage
import io.buszi.nascope.domain.model.telemetry.DiskIoRate
import io.buszi.nascope.domain.model.telemetry.FileSystemUsage
import io.buszi.nascope.domain.model.telemetry.GpuUsage
import io.buszi.nascope.domain.model.telemetry.LoadAverage
import io.buszi.nascope.domain.model.telemetry.MemoryUsage
import io.buszi.nascope.domain.model.telemetry.NetworkInterfaceUsage
import io.buszi.nascope.domain.model.telemetry.ProcessCount
import io.buszi.nascope.domain.model.telemetry.ProcessUsage
import io.buszi.nascope.domain.model.telemetry.SensorKind
import io.buszi.nascope.domain.model.telemetry.SensorReading
import io.buszi.nascope.domain.model.telemetry.SwapUsage
import io.buszi.nascope.domain.model.telemetry.SystemInfo
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/**
 * A fake server for demo mode. Values start at the old hardcoded baseline and drift in a small bounded
 * random walk on every call, so charts move. Totals and capacities stay consistent. Never fails and never
 * touches the network.
 */
internal class DemoTelemetrySource(
    private val random: Random,
) : TelemetrySource {

    private val lock = Mutex()
    private val started = TimeSource.Monotonic.markNow()

    private fun walk(value: Double, step: Double, min: Double, max: Double): Double =
        (value + random.nextDouble(-step, step)).coerceIn(min, max)

    private fun walkLong(value: Long, step: Long, min: Long, max: Long): Long =
        (value + random.nextLong(-step, step + 1)).coerceIn(min, max)

    private suspend fun <T> demo(block: () -> T): Result<T, TelemetryFailure> = lock.withLock { Ok(block()) }

    /** Every plugin this source implements. */
    override suspend fun fetchPlugins(): Result<Set<String>, TelemetryFailure> = Ok(
        setOf(
            "system", "uptime", "load", "cpu", "percpu", "quicklook", "mem", "memswap", "fs", "diskio", "network",
            "sensors", "containers", "gpu", "processcount", "processlist",
        ),
    )

    // region CPU

    private val coreUsage = DoubleArray(CORE_COUNT) { BASE_CORE_USAGE[it] }

    private fun stepCores() {
        for (i in coreUsage.indices) coreUsage[i] = walk(coreUsage[i], 6.0, 2.0, 98.0)
    }

    override suspend fun fetchCpuCores(): Result<List<CpuCoreUsage>, TelemetryFailure> = demo {
        stepCores()
        coreUsage.mapIndexed { index, usage -> CpuCoreUsage(index, usage.toFloat()) }
    }

    override suspend fun fetchCpu(): Result<CpuUsage, TelemetryFailure> = demo {
        stepCores()
        val total = coreUsage.average()
        CpuUsage(
            totalPercent = total.toFloat(),
            userPercent = (total * 0.70).toFloat(),
            systemPercent = (total * 0.25).toFloat(),
            ioWaitPercent = (total * 0.05).toFloat(),
            idlePercent = (100.0 - total).toFloat(),
            logicalCores = CORE_COUNT,
        )
    }

    override suspend fun fetchCpuInfo(): Result<CpuInfo, TelemetryFailure> = Ok(
        CpuInfo(name = "AMD Ryzen 9 5950X (16-Core)", currentFrequencyHz = 3_800_000_000, maxFrequencyHz = 4_900_000_000),
    )

    private var load1 = 0.42
    private var load5 = 0.58
    private var load15 = 0.71

    override suspend fun fetchLoad(): Result<LoadAverage, TelemetryFailure> = demo {
        load1 = walk(load1, 0.08, 0.05, 3.0)
        load5 = walk(load5, 0.04, 0.05, 3.0)
        load15 = walk(load15, 0.02, 0.05, 3.0)
        LoadAverage(load1.toFloat(), load5.toFloat(), load15.toFloat(), CORE_COUNT)
    }

    // endregion

    override suspend fun fetchSystem(): Result<SystemInfo, TelemetryFailure> = Ok(
        SystemInfo(
            hostname = "alpha-node-01.local",
            osName = "Linux",
            osVersion = "6.8.0-45-generic",
            distribution = "Ubuntu 24.04.1 LTS",
            architecture = "64bit",
            displayName = "Ubuntu 24.04.1 LTS 64bit / Linux 6.8.0-45-generic",
        ),
    )

    override suspend fun fetchUptime(): Result<Duration, TelemetryFailure> =
        Ok(14.days + 6.hours + 32.minutes + started.elapsedNow())

    // region Memory

    private var memUsed = 13_743_895_347L
    private var memCached = 9_126_805_504L
    private var memBuffers = 1_288_490_188L
    private var swapUsed = 2_576_980_377L

    override suspend fun fetchMemory(): Result<MemoryUsage, TelemetryFailure> = demo {
        memUsed = walkLong(memUsed, 80_000_000, 8_000_000_000, 24_000_000_000)
        memCached = walkLong(memCached, 40_000_000, 4_000_000_000, 12_000_000_000)
        memBuffers = walkLong(memBuffers, 10_000_000, 500_000_000, 2_000_000_000)
        val free = MEM_TOTAL - memUsed
        MemoryUsage(
            totalBytes = MEM_TOTAL,
            usedBytes = memUsed,
            freeBytes = free,
            availableBytes = free,
            cachedBytes = memCached,
            buffersBytes = memBuffers,
        )
    }

    override suspend fun fetchSwap(): Result<SwapUsage, TelemetryFailure> = demo {
        swapUsed = walkLong(swapUsed, 20_000_000, 1_000_000_000, 6_000_000_000)
        SwapUsage(SWAP_TOTAL, swapUsed, SWAP_TOTAL - swapUsed)
    }

    // endregion

    // region Storage

    private class Mount(
        val path: String, val device: String, val type: String, val total: Long, var used: Long, val options: String,
    )

    private val mounts = listOf(
        Mount("/", "/dev/nvme0n1p2", "ext4", 512_000_000_000, 153_600_000_000, "rw,noatime,discard"),
        Mount("/volume1", "/dev/mapper/vg0-data", "btrfs", 3_800_000_000_000, 2_470_000_000_000, "rw,relatime,compress=zstd:3"),
        Mount("/zpool/media", "tank/media", "zfs", 8_000_000_000_000, 6_880_000_000_000, "rw,xattr,noacl"),
        Mount("/mnt/backup", "/dev/sdc1", "ext4", 2_000_000_000_000, 420_000_000_000, "rw,relatime,nofail"),
    )

    override suspend fun fetchFileSystems(): Result<List<FileSystemUsage>, TelemetryFailure> = demo {
        mounts.map { m ->
            m.used = walkLong(m.used, m.total / 5_000, m.total / 20, m.total - m.total / 20)
            FileSystemUsage(m.path, m.device, m.type, m.total, m.used, m.total - m.used, m.options)
        }
    }

    private var nvmeRead = 30_000_000L
    private var nvmeWrite = 100_000_000L
    private var sdaRead = 12_500_000L
    private var sdaWrite = 28_300_000L

    override suspend fun fetchDiskIo(): Result<List<DiskIoRate>, TelemetryFailure> = demo {
        nvmeRead = walkLong(nvmeRead, 6_000_000, 1_000_000, 90_000_000)
        nvmeWrite = walkLong(nvmeWrite, 12_000_000, 1_000_000, 250_000_000)
        sdaRead = walkLong(sdaRead, 3_000_000, 0, 60_000_000)
        sdaWrite = walkLong(sdaWrite, 4_000_000, 0, 80_000_000)
        listOf(DiskIoRate("nvme0n1", nvmeRead, nvmeWrite), DiskIoRate("sda", sdaRead, sdaWrite))
    }

    // endregion

    // region Network

    private var eth0Rx = 14_850_000L
    private var eth0Tx = 3_210_000L
    private var eth1Rx = 520_000L
    private var eth1Tx = 85_000L
    private var eth0TotalRx = 842_300_000_000L
    private var eth0TotalTx = 195_600_000_000L
    private var eth1TotalRx = 124_000_000_000L
    private var eth1TotalTx = 42_000_000_000L

    override suspend fun fetchNetworkInterfaces(): Result<List<NetworkInterfaceUsage>, TelemetryFailure> = demo {
        eth0Rx = walkLong(eth0Rx, 2_500_000, 500_000, 60_000_000)
        eth0Tx = walkLong(eth0Tx, 600_000, 100_000, 20_000_000)
        eth1Rx = walkLong(eth1Rx, 90_000, 10_000, 2_000_000)
        eth1Tx = walkLong(eth1Tx, 20_000, 5_000, 500_000)
        eth0TotalRx += eth0Rx
        eth0TotalTx += eth0Tx
        eth1TotalRx += eth1Rx
        eth1TotalTx += eth1Tx
        listOf(
            NetworkInterfaceUsage("eth0", null, true, 1_000_000_000, eth0Rx, eth0Tx, eth0TotalRx, eth0TotalTx),
            NetworkInterfaceUsage("eth1", null, true, 100_000_000, eth1Rx, eth1Tx, eth1TotalRx, eth1TotalTx),
            NetworkInterfaceUsage("wlan0", null, false, null, 0, 0, 1_200_000_000, 450_000_000),
        )
    }

    // endregion

    // region GPU

    private var gpuLoad = 18.0
    private var gpuTemp = 47.0

    override suspend fun fetchGpus(): Result<List<GpuUsage>, TelemetryFailure> = demo {
        gpuLoad = walk(gpuLoad, 9.0, 0.0, 85.0)
        gpuTemp = walk(gpuTemp, 0.8, 40.0, 68.0)
        listOf(GpuUsage("intel0", "Intel UHD Graphics", gpuLoad.toFloat(), null, gpuTemp.toFloat(), null))
    }

    // endregion

    // region Sensors

    private var cpuTemp = 58.0
    private var nvmeTemp = 41.0
    private var sdaTemp = 34.0
    private var sdbTemp = 36.0
    private var cpuFan = 1380.0
    private var chassis1 = 920.0
    private var chassis2 = 890.0

    override suspend fun fetchSensors(): Result<List<SensorReading>, TelemetryFailure> = demo {
        cpuTemp = walk(cpuTemp, 1.5, 42.0, 78.0)
        nvmeTemp = walk(nvmeTemp, 0.6, 34.0, 55.0)
        sdaTemp = walk(sdaTemp, 0.4, 28.0, 45.0)
        sdbTemp = walk(sdbTemp, 0.4, 28.0, 45.0)
        cpuFan = walk(cpuFan, 40.0, 900.0, 2_100.0)
        chassis1 = walk(chassis1, 20.0, 600.0, 1_700.0)
        chassis2 = walk(chassis2, 20.0, 600.0, 1_700.0)
        listOf(
            SensorReading("CPU Package", SensorKind.CpuTemperature, cpuTemp.toFloat(), "C", 75f, 85f),
            SensorReading("nvme0n1 (NVMe)", SensorKind.DriveTemperature, nvmeTemp.toFloat(), "C", 65f, 75f),
            SensorReading("sda (WD Red 8TB)", SensorKind.DriveTemperature, sdaTemp.toFloat(), "C", 50f, 60f),
            SensorReading("sdb (WD Red 8TB)", SensorKind.DriveTemperature, sdbTemp.toFloat(), "C", 50f, 60f),
            SensorReading("CPU Fan", SensorKind.FanSpeed, cpuFan.toFloat(), "R", null, null),
            SensorReading("Chassis 1", SensorKind.FanSpeed, chassis1.toFloat(), "R", null, null),
            SensorReading("Chassis 2", SensorKind.FanSpeed, chassis2.toFloat(), "R", null, null),
        )
    }

    // endregion

    // region Containers & processes

    private class Container(
        val id: String, val name: String, val image: String, val status: ContainerStatus,
        var cpu: Double, var memory: Long, val limit: Long, val uptime: String,
    )

    private val containers = listOf(
        Container("7f3b8a1c", "traefik", "traefik:v3.1", ContainerStatus.Running, 1.4, 48_500_000, 256_000_000, "14 days"),
        Container("e4a2d981", "nextcloud", "nextcloud:29.0-fpm", ContainerStatus.Running, 3.2, 285_000_000, 1_024_000_000, "14 days"),
        Container("b1c7e052", "postgres-db", "postgres:16-alpine", ContainerStatus.Running, 0.8, 194_000_000, 512_000_000, "14 days"),
        Container("a90d3f64", "plex-server", "linuxserver/plex:latest", ContainerStatus.Running, 8.5, 640_000_000, 2_048_000_000, "3 days"),
        Container("c5e8a217", "vaultwarden", "vaultwarden/server:latest", ContainerStatus.Running, 0.2, 32_000_000, 128_000_000, "14 days"),
        Container("d2f4b690", "redis-cache", "redis:7.2-alpine", ContainerStatus.Running, 0.4, 18_000_000, 128_000_000, "14 days"),
        Container("f8a1c3d5", "qbittorrent", "linuxserver/qbittorrent:latest", ContainerStatus.Paused, 0.0, 112_000_000, 1_024_000_000, "2 days"),
        Container("09be7d4a", "borgmatic-backup", "b3vis/borgmatic:latest", ContainerStatus.Exited, 0.0, 0, 512_000_000, "6 hours"),
    )

    override suspend fun fetchContainers(): Result<List<ContainerUsage>, TelemetryFailure> = demo {
        containers.map { c ->
            if (c.status == ContainerStatus.Running) {
                c.cpu = walk(c.cpu, 0.6, 0.0, 40.0)
                c.memory = walkLong(c.memory, c.limit / 100, c.limit / 20, c.limit * 9 / 10)
            }
            ContainerUsage(
                id = c.id, name = c.name, image = c.image, engine = "docker", status = c.status,
                cpuPercent = c.cpu.toFloat(), memoryUsageBytes = c.memory, memoryLimitBytes = c.limit,
                uptimeText = c.uptime,
            )
        }
    }

    private var runningProcesses = 3

    override suspend fun fetchProcessCount(): Result<ProcessCount, TelemetryFailure> = demo {
        runningProcesses = (runningProcesses + random.nextInt(-1, 2)).coerceIn(1, 6)
        ProcessCount(total = PROCESS_TOTAL, running = runningProcesses, sleeping = PROCESS_TOTAL - runningProcesses, threads = 612)
    }

    private class Process(
        val pid: Int, val name: String, val user: String, var cpu: Double, var rss: Long, val status: String,
    )

    private val processes = listOf(
        Process(1042, "qemu-system-x86", "libvirt", 28.4, 4_870_000_000, "R"),
        Process(2391, "plex-media-srv", "plex", 16.8, 2_920_000_000, "S"),
        Process(1820, "dockerd", "root", 6.2, 1_060_000_000, "S"),
        Process(3410, "postgres", "postgres", 4.5, 1_990_000_000, "S"),
        Process(4125, "traefik", "traefik", 2.1, 412_000_000, "S"),
        Process(5122, "node", "app", 1.9, 1_300_000_000, "S"),
        Process(890, "systemd-journal", "root", 0.8, 515_000_000, "S"),
        Process(6210, "redis-server", "redis", 0.5, 824_000_000, "S"),
        Process(7015, "sshd", "root", 0.3, 38_000_000, "S"),
        Process(7344, "tailscaled", "root", 0.9, 138_000_000, "S"),
    )

    override suspend fun fetchProcesses(): Result<List<ProcessUsage>, TelemetryFailure> = demo {
        processes.map { p ->
            p.cpu = walk(p.cpu, p.cpu * 0.15 + 0.1, 0.0, 95.0)
            p.rss = walkLong(p.rss, p.rss / 100, p.rss / 2, p.rss * 3 / 2)
            ProcessUsage(
                pid = p.pid, name = p.name, user = p.user, cpuPercent = p.cpu.toFloat(),
                memoryPercent = (p.rss.toDouble() / MEM_TOTAL * 100).toFloat(), memoryRssBytes = p.rss,
                statusCode = p.status, commandLine = listOf("/usr/bin/${p.name}"),
            )
        }
    }

    // endregion

    private companion object {
        const val CORE_COUNT = 16
        const val MEM_TOTAL = 34_359_738_368L
        const val SWAP_TOTAL = 17_179_869_184L
        const val PROCESS_TOTAL = 184
        val BASE_CORE_USAGE = doubleArrayOf(
            34.2, 48.1, 12.0, 88.5, 24.0, 62.3, 41.0, 19.8, 55.4, 31.2, 76.8, 92.1, 15.6, 43.9, 28.7, 64.5,
        )
    }
}
