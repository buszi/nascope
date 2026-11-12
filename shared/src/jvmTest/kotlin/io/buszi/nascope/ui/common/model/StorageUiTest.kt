package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.telemetry.DiskIoRate
import io.buszi.nascope.domain.model.telemetry.FileSystemUsage
import io.buszi.nascope.domain.telemetry.ObserveStorageUseCase.Disk
import io.buszi.nascope.domain.telemetry.ObserveStorageUseCase.Data
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StorageUiTest {

    private val fileSystem = FileSystemUsage("/", "sda1", "ext4", 100L, 40L, 60L, null)

    @Test
    fun mapsTheMountsAndTheDiskIo() {
        val ui = StorageUi.from(Data(listOf(fileSystem), emptyList(), 100L, 50L, listOf(1f), listOf(2f)))

        assertEquals(listOf(StorageMountUi.from(fileSystem)), ui.mounts)
        assertEquals(DiskIoUi(100L, 50L, listOf(1f), listOf(2f)), ui.diskIo)
    }

    @Test
    fun hasNoDiskIoWhenTheHostProvidesNone() {
        val ui = StorageUi.from(Data(listOf(fileSystem), emptyList(), null, null, emptyList(), emptyList()))

        assertNull(ui.diskIo)
    }

    @Test
    fun listsTheDisksBusiestFirstWithTheirFigures() {
        val idle = Disk(DiskIoRate("sdb", 0, 0), emptyList(), emptyList())
        val reading = Disk(DiskIoRate("sda", 900, 10, 12, 3, 4, 5), listOf(1f, 900f), listOf(0f, 10f))
        val writing = Disk(DiskIoRate("nvme0n1", 5, 2_000), listOf(5f), listOf(2_000f))
        val ui = StorageUi.from(Data(listOf(fileSystem), listOf(idle, reading, writing), 905L, 2_010L, emptyList(), emptyList()))

        assertEquals(listOf("nvme0n1", "sda", "sdb"), ui.disks.map { it.name })
        val sda = ui.disks[1]
        assertEquals(DiskUi("sda", 900, 10, 12, 3, 4, 5, listOf(1f, 900f), listOf(0f, 10f)), sda)
        assertEquals(910L, sda.totalBytesPerSec)
        assertNull(ui.disks[0].readIops)
        assertNull(ui.disks[0].writeLatencyMs)
    }

    @Test
    fun hasNoDisksWhenTheHostReportsNone() {
        assertEquals(emptyList(), StorageUi.from(Data(listOf(fileSystem), emptyList(), null, null, emptyList(), emptyList())).disks)
    }

    private fun disk(name: String, read: Long = 1) = Disk(DiskIoRate(name, read, 0), emptyList(), emptyList())

    @Test
    fun leavesOutPartitionsAndVirtualDevicesButKeepsDisksArraysAndCaches() {
        // The device names of the real NAS capture.
        val names = listOf(
            "nvme0n1", "nvme0n1p1", "nvme0n1p2", "mmcblk0", "mmcblk0p1", "mmcblk0p128", "mmcblk0boot0", "mmcblk0boot1",
            "sdb", "sdb1", "sdb2", "sda", "sda1", "zram0", "zram3", "md1", "md2", "dm-0", "dm-3", "bcache0", "loop0", "nbd1", "vda", "vda1",
        )
        val ui = StorageUi.from(Data(listOf(fileSystem), names.map(::disk), 1L, 0L, emptyList(), emptyList()))

        assertEquals(
            setOf("nvme0n1", "mmcblk0", "sdb", "sda", "md1", "md2", "bcache0", "vda"),
            ui.disks.map { it.name }.toSet(),
        )
    }

    @Test
    fun keepsEveryDeviceWhenFilteringWouldLeaveNone() {
        val ui = StorageUi.from(Data(listOf(fileSystem), listOf(disk("sda1"), disk("dm-0")), 1L, 0L, emptyList(), emptyList()))
        assertEquals(listOf("sda1", "dm-0").toSet(), ui.disks.map { it.name }.toSet())
    }

    @Test
    fun theSummedThroughputStillCountsEveryDevice() {
        val ui = StorageUi.from(Data(listOf(fileSystem), listOf(disk("sda", 5), disk("sda1", 5)), 10L, 0L, emptyList(), emptyList()))
        assertEquals(1, ui.disks.size)
        assertEquals(10L, ui.diskIo?.readBytesPerSec)
    }
}
