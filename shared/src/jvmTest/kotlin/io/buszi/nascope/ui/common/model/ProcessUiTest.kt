package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.telemetry.ProcessUsage
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class ProcessUiTest {

    @Test
    fun stateIsParsedFromStatusCode() {
        assertEquals(ProcessStateUi.Running, ProcessStateUi.fromCode("R"))
        assertEquals(ProcessStateUi.Sleeping, ProcessStateUi.fromCode("s"))
        assertEquals(ProcessStateUi.DiskSleep, ProcessStateUi.fromCode("D"))
        assertEquals(ProcessStateUi.Idle, ProcessStateUi.fromCode("I"))
        assertEquals(ProcessStateUi.Stopped, ProcessStateUi.fromCode("T"))
        assertEquals(ProcessStateUi.Zombie, ProcessStateUi.fromCode("Z"))
        assertEquals(ProcessStateUi.Unknown, ProcessStateUi.fromCode("X"))
        assertEquals(ProcessStateUi.Unknown, ProcessStateUi.fromCode(""))
    }

    @Test
    fun stateIsExposedOnProcess() {
        assertEquals(ProcessStateUi.Zombie, process(cpu = 0f, memory = 0f, status = "Z").state)
    }

    @Test
    fun loadStatusFollowsThresholdsOfCpuAndMemory() {
        assertEquals(StatusLevel.Operational, process(cpu = 49.9f, memory = 10f).loadStatus)
        assertEquals(StatusLevel.Degraded, process(cpu = 50f, memory = 10f).loadStatus)
        assertEquals(StatusLevel.Degraded, process(cpu = 10f, memory = 79.9f).loadStatus)
        assertEquals(StatusLevel.Critical, process(cpu = 10f, memory = 80f).loadStatus)
        assertEquals(StatusLevel.Critical, process(cpu = 85f, memory = 55f).loadStatus)
    }

    @Test
    fun fromMapsAFullProcessAndFillsInWhatTheHostDoesNotReport() {
        assertEquals(
            ProcessUi(7, "nginx", "www", 1.5f, 2.5f, 1024L, "S"),
            ProcessUi.from(ProcessUsage(7, "nginx", "www", 1.5f, 2.5f, 1024L, "S", listOf("nginx"))),
        )
        assertEquals(
            ProcessUi(8, "kworker", ProcessUi.MissingUser, 0f, 0f, null, ""),
            ProcessUi.from(ProcessUsage(8, "kworker", null, 0f, 0f, null, null, null)),
        )
    }

    private fun process(cpu: Float, memory: Float, status: String = "R") = ProcessUi(
        pid = 1,
        name = "proc",
        user = "root",
        cpuPercent = cpu,
        memoryPercent = memory,
        status = status,
    )
}
