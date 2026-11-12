package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.model.telemetry.DiskIoRate
import io.buszi.nascope.domain.model.telemetry.FileSystemUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import kotlinx.coroutines.flow.Flow

/**
 * Primary: `fs`. Secondary: `diskio`, which also feeds the histories: the summed one and one per disk (one sample per
 * fresh disk I/O poll). The history of a disk that vanishes is dropped.
 */
fun interface ObserveStorageUseCase {
    operator fun invoke(): Flow<TelemetryState<Data>>

    data class Data(
        val fileSystems: List<FileSystemUsage>,
        /** In the order Glances reports them; empty when `diskio` is not available. */
        val disks: List<Disk>,
        /** Sums over [disks]; `null` when `diskio` is not available. */
        val readBytesPerSec: Long?,
        val writeBytesPerSec: Long?,
        val readHistory: List<Float>,
        val writeHistory: List<Float>,
    )

    data class Disk(
        val io: DiskIoRate,
        /** Bytes per second, oldest first, at most 60 samples. */
        val readHistory: List<Float>,
        val writeHistory: List<Float>,
    )
}
