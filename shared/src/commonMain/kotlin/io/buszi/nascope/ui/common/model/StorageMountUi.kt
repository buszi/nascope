package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.data.telemetry.StorageMountData
import io.buszi.nascope.ui.common.components.badges.StatusLevel

/**
 * UI representation of a mounted filesystem.
 */
@Immutable
data class StorageMountUi(
    val mountPoint: String,
    val deviceName: String,
    val fsType: String,
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val isHealthy: Boolean = true,
    val mountOptions: String = "",
    val inodeUsagePercent: Float = 0f,
) {
    /** Fraction of the capacity that is in use, in `[0.0, 1.0]`. */
    val usageRatio: Float
        get() = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    /** Health status derived from [usageRatio] and [isHealthy]. */
    val status: StatusLevel
        get() = when {
            !isHealthy || usageRatio >= CriticalThreshold -> StatusLevel.Critical
            usageRatio >= WarningThreshold -> StatusLevel.Degraded
            else -> StatusLevel.Operational
        }

    companion object {
        const val WarningThreshold: Float = 0.80f
        const val CriticalThreshold: Float = 0.90f

        fun from(data: StorageMountData) = StorageMountUi(
            mountPoint = data.mountPoint,
            deviceName = data.deviceName,
            fsType = data.fsType,
            totalBytes = data.totalBytes,
            usedBytes = data.usedBytes,
            freeBytes = data.freeBytes,
            isHealthy = data.isHealthy,
            mountOptions = data.mountOptions,
            inodeUsagePercent = data.inodeUsagePercent,
        )
    }
}

/** Worst [StorageMountUi.status] across the mounts, [StatusLevel.Operational] when there are none. */
val List<StorageMountUi>.overallStatus: StatusLevel
    get() = when {
        any { it.status == StatusLevel.Critical } -> StatusLevel.Critical
        any { it.status == StatusLevel.Degraded } -> StatusLevel.Degraded
        else -> StatusLevel.Operational
    }
