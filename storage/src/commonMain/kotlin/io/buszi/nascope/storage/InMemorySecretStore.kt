package io.buszi.nascope.storage

import io.buszi.nascope.domain.model.connection.ServerProfileId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Fallback when no secure backend exists. Secrets last for the session and are never written to disk. */
internal class InMemorySecretStore : SecretStore {
    private val mutex = Mutex()
    private val secrets = mutableMapOf<String, String>()

    override val isPersistent: Boolean = false

    override suspend fun put(id: ServerProfileId, secret: String) {
        mutex.withLock { secrets[id.value] = secret }
    }

    override suspend fun get(id: ServerProfileId): String? = mutex.withLock { secrets[id.value] }

    override suspend fun remove(id: ServerProfileId) {
        mutex.withLock { secrets.remove(id.value) }
    }
}
