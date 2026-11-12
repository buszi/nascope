package io.buszi.nascope.domain.telemetry

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
import io.buszi.nascope.domain.model.telemetry.SensorReading
import io.buszi.nascope.domain.model.telemetry.SwapUsage
import io.buszi.nascope.domain.model.telemetry.SystemInfo
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/** One shared polling loop per Glances plugin. A loop exists only while something collects its flow. */
internal interface TelemetryPoller {
    val system: Flow<Polled<SystemInfo>>
    val uptime: Flow<Polled<Duration>>
    val load: Flow<Polled<LoadAverage>>
    val cpu: Flow<Polled<CpuUsage>>
    val cpuCores: Flow<Polled<List<CpuCoreUsage>>>
    val cpuInfo: Flow<Polled<CpuInfo>>
    val memory: Flow<Polled<MemoryUsage>>
    val swap: Flow<Polled<SwapUsage>>
    val fileSystems: Flow<Polled<List<FileSystemUsage>>>
    val diskIo: Flow<Polled<List<DiskIoRate>>>
    val networkInterfaces: Flow<Polled<List<NetworkInterfaceUsage>>>
    val sensors: Flow<Polled<List<SensorReading>>>
    val containers: Flow<Polled<List<ContainerUsage>>>
    val gpus: Flow<Polled<List<GpuUsage>>>
    val processCount: Flow<Polled<ProcessCount>>
    val processes: Flow<Polled<List<ProcessUsage>>>

    /** Makes every running loop poll now instead of waiting for its period. */
    fun refresh()
}
