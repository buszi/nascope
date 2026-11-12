package io.buszi.nascope.domain.telemetry

import io.buszi.nascope.domain.model.telemetry.DiskIoRate
import io.buszi.nascope.domain.model.telemetry.FileSystemUsage
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import kotlinx.coroutines.flow.Flow

/** Primary: `fs`. Secondary: `diskio`, which also feeds the history (one sample per fresh disk I/O poll). */
fun interface ObserveStorageUseCase {
    operator fun invoke(): Flow<TelemetryState<Data>>

    data class Data(
        val fileSystems: List<FileSystemUsage>,
        /** Empty when `diskio` is not available. */
        val disks: List<DiskIoRate>,
        /** Sums over [disks]; `null` when `diskio` is not available. */
        val readBytesPerSec: Long?,
        val writeBytesPerSec: Long?,
        val readHistory: List<Float>,
        val writeHistory: List<Float>,
    )
}
