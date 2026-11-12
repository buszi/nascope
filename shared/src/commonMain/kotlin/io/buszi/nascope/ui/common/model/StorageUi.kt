package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.telemetry.ObserveStorageUseCase

/** UI representation of the storage brick data: the mounts and, when the host provides it, the disk I/O. */
@Immutable
data class StorageUi(
    val mounts: List<StorageMountUi> = emptyList(),
    /** `null` when `diskio` is not available: the disk I/O section is left out. */
    val diskIo: DiskIoUi? = null,
) {
    companion object {
        fun from(data: ObserveStorageUseCase.Data): StorageUi = StorageUi(
            mounts = data.fileSystems.map(StorageMountUi::from).withoutContainerInjectedDuplicates(),
            diskIo = if (data.readBytesPerSec != null && data.writeBytesPerSec != null) {
                DiskIoUi(
                    readBytesPerSec = data.readBytesPerSec!!,
                    writeBytesPerSec = data.writeBytesPerSec!!,
                    readHistory = data.readHistory,
                    writeHistory = data.writeHistory,
                )
            } else {
                null
            },
        )
    }
}

/** Disk throughput summed over all disks, in bytes per second, with its history (oldest first). */
@Immutable
data class DiskIoUi(
    val readBytesPerSec: Long,
    val writeBytesPerSec: Long,
    val readHistory: List<Float>,
    val writeHistory: List<Float>,
)
