package io.buszi.nascope.ui.dashboard.widgets.storage

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.model.DiskIoUi
import io.buszi.nascope.ui.common.model.DiskUi
import io.buszi.nascope.ui.common.model.StorageMountUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.storage.StorageDetailsViewModel.ViewState

class StorageDetailsPreviewParameterProvider : PreviewParameterProvider<ViewState> {
    override val values: Sequence<ViewState> = sequenceOf(
        // 1. Mixed usage, one mount expanded
        ViewState(
            mounts = previewMounts,
            diskIo = DiskIoUi(
                readBytesPerSec = 42_500_000L,
                writeBytesPerSec = 128_300_000L,
                readHistory = listOf(31e6f, 36e6f, 40e6f, 45e6f, 39e6f, 36e6f, 42e6f, 52e6f, 58e6f, 49e6f, 43e6f, 41e6f),
                writeHistory = listOf(96e6f, 110e6f, 125e6f, 138e6f, 131e6f, 120e6f, 127e6f, 143e6f, 152e6f, 139e6f, 128e6f, 122e6f),
            ),
            expandedMountPoint = "/zpool/media",
            widgetState = WidgetState.Ready,
        ),

        // 1b. Per-disk I/O: a v4 disk with IOPS and latency, a v3 one without latency, an idle one; second page of two
        ViewState(
            mounts = previewMounts,
            disks = listOf(
                DiskUi("nvme0n1", 42_500_000L, 128_300_000L, 1_240, 3_900, 1, 2, listOf(30e6f, 41e6f, 38e6f, 45e6f), listOf(90e6f, 120e6f, 130e6f, 128e6f)),
                DiskUi("sda", 8_000_000L, 2_000_000L, 64, 20, null, null, listOf(7e6f, 8e6f, 9e6f), listOf(1e6f, 2e6f, 2e6f)),
                DiskUi("sdb", 0L, 0L, 0, 0, 0, 0, emptyList(), emptyList()),
            ),
            widgetState = WidgetState.Ready,
        ),

        // 2. No disk I/O and no mount metadata (Glances 3 without diskio)
        ViewState(
            mounts = listOf(StorageMountUi("/", "", "", 512_000_000_000L, 150_000_000_000L, 362_000_000_000L)),
            expandedMountPoint = "/",
            widgetState = WidgetState.Stale,
        ),

        // 3. No mounts
        ViewState(widgetState = WidgetState.Ready),

        // 4. Unavailable
        ViewState(widgetState = WidgetState.Unavailable),

        // 5. Loading
        ViewState(widgetState = WidgetState.Loading),
    )

    private companion object {
        val previewMounts = listOf(
            StorageMountUi(
                mountPoint = "/",
                deviceName = "/dev/nvme0n1p2",
                fsType = "ext4",
                totalBytes = 512_000_000_000L,
                usedBytes = 153_600_000_000L,
                freeBytes = 358_400_000_000L,
                mountOptions = "rw,noatime,discard",
            ),
            StorageMountUi(
                mountPoint = "/volume1",
                deviceName = "/dev/mapper/vg0-data",
                fsType = "btrfs",
                totalBytes = 3_800_000_000_000L,
                usedBytes = 2_470_000_000_000L,
                freeBytes = 1_330_000_000_000L,
                mountOptions = "rw,relatime,compress=zstd:3",
            ),
            StorageMountUi(
                mountPoint = "/zpool/media",
                deviceName = "tank/media",
                fsType = "zfs",
                totalBytes = 8_000_000_000_000L,
                usedBytes = 6_880_000_000_000L,
                freeBytes = 1_120_000_000_000L,
                mountOptions = "rw,xattr,noacl",
            ),
        )
    }
}
