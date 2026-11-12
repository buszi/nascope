package io.buszi.nascope.domain.connection

import io.buszi.nascope.domain.model.connection.ServerAuth
import io.buszi.nascope.domain.model.connection.ServerConnection
import io.buszi.nascope.domain.model.connection.ServerCredentials
import io.buszi.nascope.domain.model.connection.ServerProfile
import io.buszi.nascope.storage.SecretStore

/** Everything needed to call [this] server. The password is read from [secretStore] on every call. */
internal suspend fun ServerProfile.toServerConnection(secretStore: SecretStore): ServerConnection =
    ServerConnection(
        address = address,
        apiVersion = apiVersion,
        credentials = when (val auth = auth) {
            ServerAuth.None -> null
            is ServerAuth.Basic -> ServerCredentials(auth.username, secretStore.get(id).orEmpty())
        },
    )
