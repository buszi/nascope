package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.telemetry.FileSystemUsage
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class StorageMountUiTest {

    @Test
    fun usageRatioIsUsedOverTotal() {
        assertEquals(0.25f, mount(total = 400L, used = 100L).usageRatio)
    }

    @Test
    fun usageRatioIsZeroForEmptyCapacity() {
        assertEquals(0f, mount(total = 0L, used = 0L).usageRatio)
    }

    @Test
    fun statusFollowsUsageThresholds() {
        assertEquals(StatusLevel.Operational, mount(total = 100L, used = 79L).status)
        assertEquals(StatusLevel.Degraded, mount(total = 100L, used = 80L).status)
        assertEquals(StatusLevel.Critical, mount(total = 100L, used = 90L).status)
    }

    @Test
    fun fromMapsTheFileSystemAndLeavesMissingFieldsEmpty() {
        assertEquals(
            StorageMountUi("/volume1", "", "", 100L, 40L, 60L, ""),
            StorageMountUi.from(FileSystemUsage("/volume1", null, null, 100L, 40L, 60L, null)),
        )
        assertEquals(
            StorageMountUi("/", "sda1", "ext4", 100L, 40L, 60L, "rw,noatime"),
            StorageMountUi.from(FileSystemUsage("/", "sda1", "ext4", 100L, 40L, 60L, "rw,noatime")),
        )
    }

    @Test
    fun overallStatusIsWorstMountStatus() {
        assertEquals(StatusLevel.Operational, emptyList<StorageMountUi>().overallStatus)
        assertEquals(
            StatusLevel.Degraded,
            listOf(mount(100L, 10L), mount(100L, 85L)).overallStatus,
        )
        assertEquals(
            StatusLevel.Critical,
            listOf(mount(100L, 85L), mount(100L, 95L)).overallStatus,
        )
    }

    @Test
    fun containerInjectedMountsAreOnlyKeptWhenTheyAreTheOnlyMountOfTheDevice() {
        val injected = listOf("/etc/hosts", "/etc/hostname", "/etc/resolv.conf")
            .map { StorageMountUi(it, "/dev/bcache0", "btrfs", 100L, 65L, 35L) }
        val data = StorageMountUi("/mnt/data", "/dev/bcache0", "btrfs", 100L, 65L, 35L)
        val boot = StorageMountUi("/etc/hosts", "/dev/sda1", "ext4", 10L, 1L, 9L)

        // Only injected mounts of a device: one stays, shown under the device name.
        assertEquals(listOf(injected.first()), injected.withoutContainerInjectedDuplicates())
        assertEquals("/dev/bcache0", injected.first().displayName)
        // A real mount of the same device makes them redundant; other devices keep theirs.
        assertEquals(listOf(data, boot), (injected + data + boot).withoutContainerInjectedDuplicates())
        assertEquals("/mnt/data", data.displayName)
        assertEquals("/etc/hosts", boot.copy(deviceName = "").displayName)
    }

    private fun mount(total: Long, used: Long) = StorageMountUi(
        mountPoint = "/",
        deviceName = "/dev/sda1",
        fsType = "ext4",
        totalBytes = total,
        usedBytes = used,
        freeBytes = total - used,
    )
}
