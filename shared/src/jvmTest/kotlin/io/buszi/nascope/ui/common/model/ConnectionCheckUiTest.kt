package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.connection.TestConnectionUseCase.Step
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectionCheckUiTest {

    @Test
    fun initialChecksAreAllPendingInExecutionOrder() {
        assertEquals(
            listOf(
                ConnectionCheckUi(ConnectionCheckStepUi.ReachHost),
                ConnectionCheckUi(ConnectionCheckStepUi.DetectGlances),
                ConnectionCheckUi(ConnectionCheckStepUi.Authenticate),
            ),
            ConnectionCheckUi.initial,
        )
        assertEquals(
            setOf(ConnectionCheckStatusUi.Pending),
            ConnectionCheckUi.initial.map { it.status }.toSet(),
        )
    }

    @Test
    fun everyStepMapsToItsOwnUiStep() {
        assertEquals(ConnectionCheckStepUi.ReachHost, ConnectionCheckStepUi.from(Step.ReachHost))
        assertEquals(ConnectionCheckStepUi.DetectGlances, ConnectionCheckStepUi.from(Step.DetectGlances))
        assertEquals(ConnectionCheckStepUi.Authenticate, ConnectionCheckStepUi.from(Step.Authenticate))
    }
}
