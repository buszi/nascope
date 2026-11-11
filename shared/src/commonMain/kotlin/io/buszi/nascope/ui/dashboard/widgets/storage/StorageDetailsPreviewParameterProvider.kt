package io.buszi.nascope.ui.dashboard.widgets.storage

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.model.StorageMountUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.storage.StorageDetailsViewModel.ViewState

class StorageDetailsPreviewParameterProvider : PreviewParameterProvider<ViewState> {
    override val values: Sequence<ViewState> = sequenceOf(
        // 1. Mixed health, first mount expanded
        ViewState(
            mounts = previewMounts,
            readRateBytesPerSec = 42_500_000L,
            writeRateBytesPerSec = 128_300_000L,
            readHistory = listOf(31f, 36f, 40f, 45f, 39f, 36f, 42f, 52f, 58f, 49f, 43f, 41f),
            writeHistory = listOf(96f, 110f, 125f, 138f, 131f, 120f, 127f, 143f, 152f, 139f, 128f, 122f),
            expandedMountPoint = "/zpool/media",
            widgetState = WidgetState.Ready,
        ),

        // 2. No mounts
        ViewState(widgetState = WidgetState.Ready),

        // 3. Loading
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
                inodeUsagePercent = 12.4f,
            ),
            StorageMountUi(
                mountPoint = "/volume1",
                deviceName = "/dev/mapper/vg0-data",
                fsType = "btrfs",
                totalBytes = 3_800_000_000_000L,
                usedBytes = 2_470_000_000_000L,
                freeBytes = 1_330_000_000_000L,
                mountOptions = "rw,relatime,compress=zstd:3",
                inodeUsagePercent = 3.1f,
            ),
            StorageMountUi(
                mountPoint = "/zpool/media",
                deviceName = "tank/media",
                fsType = "zfs",
                totalBytes = 8_000_000_000_000L,
                usedBytes = 6_880_000_000_000L,
                freeBytes = 1_120_000_000_000L,
                isHealthy = false,
                mountOptions = "rw,xattr,noacl",
                inodeUsagePercent = 41.8f,
            ),
        )
    }
}
