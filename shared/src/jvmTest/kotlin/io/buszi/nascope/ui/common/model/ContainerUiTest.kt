package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.telemetry.ContainerStatus
import io.buszi.nascope.domain.model.telemetry.ContainerUsage
import io.buszi.nascope.domain.telemetry.ObserveContainersUseCase.Data
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContainerUiTest {

    @Test
    fun stateStatusReflectsSeverity() {
        assertEquals(StatusLevel.Operational, ContainerStateUi.Running.status)
        assertEquals(StatusLevel.Degraded, ContainerStateUi.Paused.status)
        assertEquals(StatusLevel.Degraded, ContainerStateUi.Exited.status)
        assertEquals(StatusLevel.Degraded, ContainerStateUi.Created.status)
        assertEquals(StatusLevel.Degraded, ContainerStateUi.Unknown.status)
        assertEquals(StatusLevel.Critical, ContainerStateUi.Restarting.status)
        assertEquals(StatusLevel.Critical, ContainerStateUi.Dead.status)
    }

    @Test
    fun everyDomainStatusHasAUiState() {
        assertEquals(
            ContainerStatus.entries.map { it.name },
            ContainerStatus.entries.map { ContainerStateUi.from(it).name },
        )
    }

    @Test
    fun memoryRatioIsUsageOverLimit() {
        assertEquals(0.25f, container(usage = 25L, limit = 100L).memoryRatio)
    }

    @Test
    fun memoryRatioIsClampedToUnitRange() {
        assertEquals(1f, container(usage = 300L, limit = 100L).memoryRatio)
    }

    @Test
    fun memoryRatioIsNullWithoutLimitOrUsage() {
        assertNull(container(usage = 25L, limit = null).memoryRatio)
        assertNull(container(usage = 25L, limit = 0L).memoryRatio)
        assertNull(container(usage = null, limit = 100L).memoryRatio)
    }

    @Test
    fun engineLabelListsTheDistinctEnginesCapitalized() {
        fun usage(engine: String?) = ContainerUsage("i", "n", null, engine, ContainerStatus.Running, null, null, null, null)

        assertEquals("Docker", ContainersUi.from(Data(listOf(usage("docker"), usage("docker")))).engine)
        assertEquals("Docker / Podman", ContainersUi.from(Data(listOf(usage("docker"), usage("podman")))).engine)
        assertEquals("", ContainersUi.from(Data(listOf(usage(null)))).engine)
    }

    private fun container(usage: Long?, limit: Long?) = ContainerUi(
        id = "id",
        name = "name",
        image = "image",
        state = ContainerStateUi.Running,
        cpuPercent = 1f,
        memoryUsageBytes = usage,
        memoryLimitBytes = limit,
        uptime = "Up",
    )
}
