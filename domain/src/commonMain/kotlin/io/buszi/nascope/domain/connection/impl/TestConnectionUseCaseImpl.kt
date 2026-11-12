package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.domain.connection.TestConnectionUseCase
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.GlancesApiVersion
import io.buszi.nascope.domain.model.connection.ServerAddress
import io.buszi.nascope.domain.model.connection.ServerCredentials
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.network.GlancesConnectionClient
import io.buszi.nascope.storage.SecretStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.annotation.Factory

@Factory
internal class TestConnectionUseCaseImpl(
    private val client: GlancesConnectionClient,
    private val secretStore: SecretStore,
    private val dispatcher: CoroutineDispatcher,
) : TestConnectionUseCase {

    override fun invoke(
        address: ServerAddress,
        credentials: ServerCredentials?,
        existingProfileId: ServerProfileId?,
    ): Flow<TestConnectionUseCase.Event> = flow {
        val reach = TestConnectionUseCase.Step.ReachHost
        val detect = TestConnectionUseCase.Step.DetectGlances
        val authenticate = TestConnectionUseCase.Step.Authenticate

        emit(TestConnectionUseCase.Event.StepStarted(reach))
        val v4 = client.fetchStatus(address, GlancesApiVersion.V4)
        if (v4 != null && v4.isTransportFailure()) {
            emit(TestConnectionUseCase.Event.Failed(reach, v4))
            return@flow
        }
        emit(TestConnectionUseCase.Event.StepPassed(reach))

        emit(TestConnectionUseCase.Event.StepStarted(detect))
        val apiVersion = if (v4 == null) {
            GlancesApiVersion.V4
        } else {
            val v3 = client.fetchStatus(address, GlancesApiVersion.V3)
            // A password-protected Glances 3 also protects its status endpoint, so 401 still means "found".
            if (v3 != null && v3 != ConnectionFailure.Unauthorized) {
                emit(TestConnectionUseCase.Event.Failed(detect, v3))
                return@flow
            }
            GlancesApiVersion.V3
        }
        emit(TestConnectionUseCase.Event.StepPassed(detect))

        emit(TestConnectionUseCase.Event.StepStarted(authenticate))
        val failure = client.verifyAccess(address, apiVersion, resolveCredentials(credentials, existingProfileId))
        if (failure != null) {
            emit(TestConnectionUseCase.Event.Failed(authenticate, failure))
            return@flow
        }
        emit(TestConnectionUseCase.Event.StepPassed(authenticate))

        emit(TestConnectionUseCase.Event.Succeeded(apiVersion))
    }.flowOn(dispatcher)

    /** An empty password on an existing profile means "keep the stored one". */
    private suspend fun resolveCredentials(
        credentials: ServerCredentials?,
        existingProfileId: ServerProfileId?,
    ): ServerCredentials? {
        if (credentials == null || credentials.password.isNotEmpty() || existingProfileId == null) return credentials
        val stored = secretStore.get(existingProfileId) ?: return credentials
        return credentials.copy(password = stored)
    }

    private fun ConnectionFailure.isTransportFailure(): Boolean = when (this) {
        ConnectionFailure.UnknownHost,
        ConnectionFailure.ConnectionRefused,
        ConnectionFailure.Timeout,
        ConnectionFailure.TlsError,
        -> true

        else -> false
    }
}
