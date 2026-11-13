package io.buszi.nascope.storage

import io.buszi.nascope.domain.model.connection.ServerProfileId
import kotlin.coroutines.cancellation.CancellationException

/**
 * Uses the platform's [primary] secure store, and keeps secrets in memory when there is none or when it refuses a
 * write (a locked or denied keyring still lets the profile be saved: the password then lasts for this session only
 * and [isPersistent] turns `false`, so that the UI says so instead of failing).
 */
internal class FallbackSecretStore(private val primary: SecretStore?) : SecretStore {
    private val memory = InMemorySecretStore()

    @Volatile
    private var lastPutPersisted = true

    /** `false` without a secure store, or when the latest write had to fall back to memory. */
    override val isPersistent: Boolean get() = primary?.isPersistent == true && lastPutPersisted

    override suspend fun put(id: ServerProfileId, secret: String) {
        if (primary == null) return memory.put(id, secret)

        try {
            primary.put(id, secret)
            memory.remove(id)
            lastPutPersisted = true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            memory.put(id, secret)
            lastPutPersisted = false
        }
    }

    override suspend fun get(id: ServerProfileId): String? =
        memory.get(id) ?: primary?.let { safely { it.get(id) } }

    override suspend fun remove(id: ServerProfileId) {
        memory.remove(id)
        primary?.let { safely { it.remove(id) } }
    }

    private suspend fun <T> safely(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}
