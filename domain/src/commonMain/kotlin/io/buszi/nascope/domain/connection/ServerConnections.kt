package io.buszi.nascope.domain.connection

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerConnection
import io.buszi.nascope.domain.model.connection.ServerCredentials
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.storage.SecretStore

/**
 * Everything needed to call [this] server. The password is read from [secretStore] on every call; a profile with
 * a username but no stored secret fails with [ConnectionFailure.PasswordMissing] (a stored empty password is fine).
 */
internal suspend fun ServerProfile.toServerConnection(
    secretStore: SecretStore,
): Result<ServerConnection, ConnectionFailure.PasswordMissing> {
    val credentials = when (val auth = auth) {
        ServerAuth.None -> null
        is ServerAuth.Basic -> {
            val password = secretStore.get(id) ?: return Err(ConnectionFailure.PasswordMissing)
            ServerCredentials(auth.username, password)
        }
    }
    return Ok(ServerConnection(address = address, apiVersion = apiVersion, credentials = credentials))
}
