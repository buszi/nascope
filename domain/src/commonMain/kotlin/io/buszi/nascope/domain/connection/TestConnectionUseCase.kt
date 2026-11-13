package io.buszi.nascope.domain.connection

import io.buszi.nascope.domain.model.connection.CertificateFingerprint
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerCredentials
import io.buszi.nascope.domain.model.connection.ServerProfileId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun interface TestConnectionUseCase {
    /**
     * Cold flow that completes after [Event.Failed] or [Event.Succeeded]. [trustedCertificate] is trusted on top of
     * the platform's validation: the profile's own, or one the user just accepted for [address].
     */
    operator fun invoke(
        address: ServerAddress,
        credentials: ServerCredentials?,
        existingProfileId: ServerProfileId?,
        trustedCertificate: CertificateFingerprint?,
    ): Flow<Event>

    enum class Step { ReachHost, DetectGlances, Authenticate }

    sealed interface Event {
        data class StepStarted(val step: Step) : Event
        data class StepPassed(val step: Step) : Event
        data class Failed(val step: Step, val failure: ConnectionFailure) : Event
        data class Succeeded(val apiVersion: GlancesApiVersion) : Event
    }
}
