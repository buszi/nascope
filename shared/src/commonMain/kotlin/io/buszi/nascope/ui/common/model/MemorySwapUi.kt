package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.telemetry.ObserveMemoryUseCase

/** UI representation of RAM and swap usage. */
@Immutable
data class MemorySwapUi(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    /** `null` when the host does not report it (shown as a placeholder). */
    val cachedBytes: Long? = null,
    /** `null` when the host does not report it (shown as a placeholder). */
    val buffersBytes: Long? = null,
    val freeBytes: Long = 0L,
    /** All three are 0 on a host without swap. */
    val totalSwapBytes: Long = 0L,
    val usedSwapBytes: Long = 0L,
    val freeSwapBytes: Long = 0L,
) {
    /**
     * [usedBytes] without the cache, for the breakdown next to cached, buffers and free.
     *
     * Depending on the host, Glances' `used` already includes the cache, so the four parts add up to more than
     * [totalBytes]. Only in that case the cache and buffers are taken out of it; otherwise it is [usedBytes].
     */
    val usedExcludingCacheBytes: Long
        get() {
            val cache = (cachedBytes ?: 0L) + (buffersBytes ?: 0L)
            val overlaps = usedBytes + cache + freeBytes > totalBytes
            return if (overlaps) (usedBytes - cache).coerceAtLeast(0L) else usedBytes
        }

    companion object {
        fun from(data: ObserveMemoryUseCase.Data): MemorySwapUi = MemorySwapUi(
            totalBytes = data.memory.totalBytes,
            usedBytes = data.memory.usedBytes,
            cachedBytes = data.memory.cachedBytes,
            buffersBytes = data.memory.buffersBytes,
            freeBytes = data.memory.freeBytes,
            totalSwapBytes = data.swap?.totalBytes ?: 0L,
            usedSwapBytes = data.swap?.usedBytes ?: 0L,
            freeSwapBytes = data.swap?.freeBytes ?: 0L,
        )
    }
}
