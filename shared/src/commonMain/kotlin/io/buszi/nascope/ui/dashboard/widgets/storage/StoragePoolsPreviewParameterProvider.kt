package io.buszi.nascope.ui.dashboard.widgets.storage

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.buszi.nascope.ui.common.model.StorageMountUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.storage.StoragePoolsViewModel.ViewState

class StoragePoolsPreviewParameterProvider : PreviewParameterProvider<ViewState> {
    override val values: Sequence<ViewState> = sequenceOf(
        // 1. Healthy / Nominal
        ViewState(
            mounts = listOf(
                StorageMountUi(
                    mountPoint = "/",
                    deviceName = "/dev/nvme0n1p2",
                    fsType = "ext4",
                    totalBytes = 512_000_000_000L,
                    usedBytes = 153_600_000_000L,
                    freeBytes = 358_400_000_000L,
                    isHealthy = true,
                ),
                StorageMountUi(
                    mountPoint = "/volume1",
                    deviceName = "/dev/mapper/vg0-data",
                    fsType = "btrfs",
                    totalBytes = 3_800_000_000_000L,
                    usedBytes = 2_470_000_000_000L,
                    freeBytes = 1_330_000_000_000L,
                    isHealthy = true,
                ),
                StorageMountUi(
                    mountPoint = "/mnt/backup",
                    deviceName = "/dev/sdc1",
                    fsType = "ext4",
                    totalBytes = 2_000_000_000_000L,
                    usedBytes = 420_000_000_000L,
                    freeBytes = 1_580_000_000_000L,
                    isHealthy = true,
                ),
            ),
            readRateBytesPerSec = 42_500_000L,
            writeRateBytesPerSec = 128_300_000L,
            widgetState = WidgetState.Ready,
        ),

        // 2. Degraded / Warning (ZFS pool degraded)
        ViewState(
            mounts = listOf(
                StorageMountUi(
                    mountPoint = "/",
                    deviceName = "/dev/nvme0n1p2",
                    fsType = "ext4",
                    totalBytes = 512_000_000_000L,
                    usedBytes = 153_600_000_000L,
                    freeBytes = 358_400_000_000L,
                    isHealthy = true,
                ),
                StorageMountUi(
                    mountPoint = "/volume1",
                    deviceName = "/dev/mapper/vg0-data",
                    fsType = "btrfs",
                    totalBytes = 3_800_000_000_000L,
                    usedBytes = 3_154_000_000_000L, // 83%
                    freeBytes = 646_000_000_000L,
                    isHealthy = true,
                ),
                StorageMountUi(
                    mountPoint = "/zpool/media",
                    deviceName = "tank/media",
                    fsType = "zfs",
                    totalBytes = 8_000_000_000_000L,
                    usedBytes = 6_880_000_000_000L, // 86%
                    freeBytes = 1_120_000_000_000L,
                    isHealthy = false,
                ),
            ),
            readRateBytesPerSec = 12_100_000L,
            writeRateBytesPerSec = 345_600_000L,
            widgetState = WidgetState.Ready,
        ),

        // 3. Critical (Mount nearly full >= 90%)
        ViewState(
            mounts = listOf(
                StorageMountUi(
                    mountPoint = "/",
                    deviceName = "/dev/nvme0n1p2",
                    fsType = "ext4",
                    totalBytes = 512_000_000_000L,
                    usedBytes = 486_400_000_000L, // 95%
                    freeBytes = 25_600_000_000L,
                    isHealthy = true,
                ),
            ),
            readRateBytesPerSec = 250_000_000L,
            writeRateBytesPerSec = 450_000_000L,
            widgetState = WidgetState.Ready,
        ),

        // 4. Many Mounts (Pagination test)
        ViewState(
            mounts = listOf(
                StorageMountUi("/", "/dev/nvme0n1p2", "ext4", 512_000_000_000L, 150_000_000_000L, 362_000_000_000L, true),
                StorageMountUi("/boot/efi", "/dev/nvme0n1p1", "vfat", 512_000_000L, 64_000_000L, 448_000_000L, true),
                StorageMountUi("/volume1", "/dev/sda1", "btrfs", 4_000_000_000_000L, 2_100_000_000_000L, 1_900_000_000_000L, true),
                StorageMountUi("/volume2", "/dev/sdb1", "btrfs", 4_000_000_000_000L, 3_400_000_000_000L, 600_000_000_000L, true),
                StorageMountUi("/zpool/data", "tank/data", "zfs", 12_000_000_000_000L, 8_400_000_000_000L, 3_600_000_000_000L, true),
                StorageMountUi("/mnt/nfs", "nas.local:/export", "nfs4", 20_000_000_000_000L, 14_000_000_000_000L, 6_000_000_000_000L, true),
            ),
            readRateBytesPerSec = 85_000_000L,
            writeRateBytesPerSec = 210_000_000L,
            currentPage = 0,
            pageSize = 3,
            widgetState = WidgetState.Ready,
        ),

        // 5. Loading
        ViewState(
            widgetState = WidgetState.Loading,
        ),

        // 6. Error
        ViewState(
            widgetState = WidgetState.Error("Failed to fetch storage telemetry"),
        ),
    )
}
