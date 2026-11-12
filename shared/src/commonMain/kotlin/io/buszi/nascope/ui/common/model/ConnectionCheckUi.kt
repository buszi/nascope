package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.connection.TestConnectionUseCase
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.connection_check_authenticate
import io.buszi.nascope.resources.connection_check_detect_glances
import io.buszi.nascope.resources.connection_check_reach_host
import org.jetbrains.compose.resources.StringResource

/** One step of the connection test, as listed in the check list. */
@Immutable
enum class ConnectionCheckStepUi(val labelRes: StringResource) {
    ReachHost(Res.string.connection_check_reach_host),
    DetectGlances(Res.string.connection_check_detect_glances),
    Authenticate(Res.string.connection_check_authenticate),
    ;

    companion object {
        fun from(data: TestConnectionUseCase.Step): ConnectionCheckStepUi = when (data) {
            TestConnectionUseCase.Step.ReachHost -> ReachHost
            TestConnectionUseCase.Step.DetectGlances -> DetectGlances
            TestConnectionUseCase.Step.Authenticate -> Authenticate
        }
    }
}

/** Progress of a [ConnectionCheckUi]. */
@Immutable
enum class ConnectionCheckStatusUi {
    Pending,
    Running,
    Passed,
    Failed,
}

/** One row of the check list: a [step], how far it got and, when [status] is Failed, why. */
@Immutable
data class ConnectionCheckUi(
    val step: ConnectionCheckStepUi,
    val status: ConnectionCheckStatusUi = ConnectionCheckStatusUi.Pending,
    val failure: ConnectionFailureUi? = null,
) {
    companion object {
        /** The check list before anything ran: every step pending, in execution order. */
        val initial: List<ConnectionCheckUi> = ConnectionCheckStepUi.entries.map(::ConnectionCheckUi)
    }
}
