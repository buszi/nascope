package io.buszi.nascope.snapshot

import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.storage.SecretStore

/** Replaces the OS keychain during a snapshot run: a password saved by a script never leaves the process. */
internal class InMemorySnapshotSecretStore : SecretStore {
    private val secrets = mutableMapOf<ServerProfileId, String>()

    override val isPersistent: Boolean = false

    override suspend fun put(id: ServerProfileId, secret: String) {
        synchronized(secrets) { secrets[id] = secret }
    }

    override suspend fun get(id: ServerProfileId): String? = synchronized(secrets) { secrets[id] }

    override suspend fun remove(id: ServerProfileId) {
        synchronized(secrets) { secrets.remove(id) }
    }
}
