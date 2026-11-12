package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.telemetry.ObserveStorageUseCase

/** UI representation of the storage brick data: the mounts and, when the host provides it, the disk I/O. */
@Immutable
data class StorageUi(
    val mounts: List<StorageMountUi> = emptyList(),
    /** `null` when `diskio` is not available: the disk I/O section is left out. */
    val diskIo: DiskIoUi? = null,
    /** Busiest first; empty when `diskio` is not available or reports no disk: the disks section is left out. */
    val disks: List<DiskUi> = emptyList(),
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
            disks = data.disks.map(DiskUi::from).withoutPartitionsAndVirtualDevices().sortedByDescending { it.totalBytesPerSec },
        )
    }
}

/** One disk's I/O: rates, operations per second and latency (each `null` when the host cannot tell) and history. */
@Immutable
data class DiskUi(
    val name: String,
    val readBytesPerSec: Long,
    val writeBytesPerSec: Long,
    val readIops: Long?,
    val writeIops: Long?,
    val readLatencyMs: Long?,
    val writeLatencyMs: Long?,
    val readHistory: List<Float>,
    val writeHistory: List<Float>,
) {
    val totalBytesPerSec: Long get() = readBytesPerSec + writeBytesPerSec

    companion object {
        private val PartitionOrVirtual = listOf(
            Regex("""(nvme\d+n\d+|mmcblk\d+)p\d+"""), // nvme0n1p1, mmcblk0p3
            Regex("""(sd|hd|vd|xvd)[a-z]+\d+"""), // sda1
            Regex("""mmcblk\d+boot\d+"""), // eMMC boot areas
            Regex("""(loop|ram|zram|nbd)\d+"""),
            Regex("""dm-\d+"""),
        )

        fun isPartitionOrVirtual(name: String): Boolean = PartitionOrVirtual.any { it.matches(name) }

        fun from(disk: ObserveStorageUseCase.Disk) = DiskUi(
            name = disk.io.diskName,
            readBytesPerSec = disk.io.readBytesPerSec,
            writeBytesPerSec = disk.io.writeBytesPerSec,
            readIops = disk.io.readIops,
            writeIops = disk.io.writeIops,
            readLatencyMs = disk.io.readLatencyMs,
            writeLatencyMs = disk.io.writeLatencyMs,
            readHistory = disk.readHistory,
            writeHistory = disk.writeHistory,
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

/**
 * Whole disks and the arrays and caches built on them: partitions (their traffic is the disk's again) and virtual
 * devices (zram, loop, nbd, device mapper, boot areas of eMMC) are left out, because a NAS reports dozens of them.
 * When that would leave nothing, every device is kept.
 */
internal fun List<DiskUi>.withoutPartitionsAndVirtualDevices(): List<DiskUi> =
    filterNot { DiskUi.isPartitionOrVirtual(it.name) }.ifEmpty { this }
