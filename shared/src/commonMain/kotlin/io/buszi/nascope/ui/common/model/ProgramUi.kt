package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.model.telemetry.ProgramUsage
import io.buszi.nascope.ui.common.components.badges.StatusLevel

/** The processes of one program, summed by Glances, as the process details list them in PROGRAMS mode. */
@Immutable
data class ProgramUi(
    val name: String,
    val processCount: Int,
    /** Sum of the processes' CPU, per core like a process. */
    val cpuPercent: Float,
    val memoryPercent: Float,
    /** `null` when the host does not report it: the RSS is left out. */
    val memoryRssBytes: Long? = null,
    /** `null` when the host does not report it: the threads are left out. */
    val threads: Int? = null,
    /** A dash when the host does not report one user for the program. */
    val user: String = ProcessUi.MissingUser,
    val logicalCores: Int = 1,
) {
    /** [cpuPercent] as a share of the whole machine in `[0, 100]`, judged like a process. */
    val cpuMachinePercent: Float
        get() = (cpuPercent / logicalCores.coerceAtLeast(1)).coerceIn(0f, 100f)

    val loadStatus: StatusLevel
        get() = maxOf(loadStatusOf(cpuMachinePercent), loadStatusOf(memoryPercent))

    private fun loadStatusOf(percent: Float): StatusLevel = when {
        percent >= ProcessUi.CriticalLoadPercent -> StatusLevel.Critical
        percent >= ProcessUi.WarningLoadPercent -> StatusLevel.Degraded
        else -> StatusLevel.Operational
    }

    companion object {
        fun from(data: ProgramUsage, logicalCores: Int?) = ProgramUi(
            name = data.name,
            processCount = data.processCount,
            cpuPercent = data.cpuPercent,
            memoryPercent = data.memoryPercent,
            memoryRssBytes = data.memoryRssBytes,
            threads = data.threads,
            user = data.user ?: ProcessUi.MissingUser,
            logicalCores = logicalCores ?: 1,
        )
    }
}
