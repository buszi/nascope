package io.buszi.nascope.ui.dashboard.widgets.storage

import io.buszi.nascope.data.telemetry.StorageMountData
import io.buszi.nascope.ui.common.components.badges.StatusLevel

internal const val StorageWarningThreshold: Float = 0.80f
internal const val StorageCriticalThreshold: Float = 0.90f

/** Fraction of the mount's capacity that is in use, in `[0.0, 1.0]`. */
internal val StorageMountData.usageRatio: Float
    get() = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

/** Health status of the mount derived from its usage and reported health. */
internal val StorageMountData.status: StatusLevel
    get() = when {
        !isHealthy || usageRatio >= StorageCriticalThreshold -> StatusLevel.Critical
        usageRatio >= StorageWarningThreshold -> StatusLevel.Degraded
        else -> StatusLevel.Operational
    }

/** Worst [StorageMountData.status] across the mounts, [StatusLevel.Operational] when there are none. */
internal val List<StorageMountData>.overallStatus: StatusLevel
    get() = when {
        any { it.status == StatusLevel.Critical } -> StatusLevel.Critical
        any { it.status == StatusLevel.Degraded } -> StatusLevel.Degraded
        else -> StatusLevel.Operational
    }
