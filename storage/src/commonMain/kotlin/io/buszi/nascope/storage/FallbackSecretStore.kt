package io.buszi.nascope.storage

import io.buszi.nascope.domain.model.connection.ServerProfileId

/** Uses the platform's [primary] secure store, or the in-memory fallback when there is none. */
internal class FallbackSecretStore(primary: SecretStore?) : SecretStore {
    private val delegate: SecretStore = primary ?: InMemorySecretStore()

    override val isPersistent: Boolean get() = delegate.isPersistent

    override suspend fun put(id: ServerProfileId, secret: String) = delegate.put(id, secret)

    override suspend fun get(id: ServerProfileId): String? = delegate.get(id)

    override suspend fun remove(id: ServerProfileId) = delegate.remove(id)
}
