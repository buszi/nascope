package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.data.telemetry.ProcessInfo

/**
 * UI representation of a single host process.
 */
@Immutable
data class ProcessUi(
    val pid: Int,
    val name: String,
    val user: String,
    val cpuPercent: Float,
    val memoryPercent: Float,
    val memoryRssBytes: Long = 0L,
    val status: String = "R",
) {
    companion object {
        fun from(data: ProcessInfo) = ProcessUi(
            pid = data.pid,
            name = data.name,
            user = data.user,
            cpuPercent = data.cpuPercent,
            memoryPercent = data.memoryPercent,
            memoryRssBytes = data.memoryRssBytes,
            status = data.status,
        )
    }
}
