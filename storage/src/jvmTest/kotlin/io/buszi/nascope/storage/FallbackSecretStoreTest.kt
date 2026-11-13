package io.buszi.nascope.storage

import io.buszi.nascope.domain.model.connection.ServerProfileId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FallbackSecretStoreTest {

    private val id = ServerProfileId("a")

    /** A keyring that can be told to refuse, like a locked one. */
    private class FlakyStore : SecretStore {
        private val inner = InMemorySecretStore()
        var refuse = false
        override val isPersistent = true

        override suspend fun put(id: ServerProfileId, secret: String) {
            check(!refuse) { "keyring locked" }
            inner.put(id, secret)
        }

        override suspend fun get(id: ServerProfileId): String? {
            check(!refuse) { "keyring locked" }
            return inner.get(id)
        }

        override suspend fun remove(id: ServerProfileId) {
            check(!refuse) { "keyring locked" }
            inner.remove(id)
        }
    }

    @Test
    fun usesThePrimaryStoreWhileItWorks() = runTest {
        val primary = FlakyStore()
        val store = FallbackSecretStore(primary)

        store.put(id, "secret")

        assertTrue(store.isPersistent)
        assertEquals("secret", primary.get(id))
        assertEquals("secret", store.get(id))
    }

    @Test
    fun withoutAPrimaryStoreSecretsLastForTheSessionOnly() = runTest {
        val store = FallbackSecretStore(null)

        store.put(id, "secret")

        assertFalse(store.isPersistent)
        assertEquals("secret", store.get(id))
    }

    @Test
    fun aRefusedWriteKeepsTheSecretInMemoryAndSaysItIsNotPersisted() = runTest {
        val primary = FlakyStore().apply { refuse = true }
        val store = FallbackSecretStore(primary)

        store.put(id, "secret")

        assertFalse(store.isPersistent)
        assertEquals("secret", store.get(id))
    }

    @Test
    fun aWriteThatWorksAgainIsPersistedAndDropsTheMemoryCopy() = runTest {
        val primary = FlakyStore().apply { refuse = true }
        val store = FallbackSecretStore(primary)
        store.put(id, "old")

        primary.refuse = false
        store.put(id, "new")

        assertTrue(store.isPersistent)
        assertEquals("new", store.get(id))
        assertEquals("new", primary.get(id))
    }

    @Test
    fun aRefusedReadIsAMissingSecretNotACrash() = runTest {
        val primary = FlakyStore()
        val store = FallbackSecretStore(primary)
        store.put(id, "secret")

        primary.refuse = true

        assertNull(store.get(id))
    }

    @Test
    fun aRefusedRemoveStillForgetsTheSessionCopy() = runTest {
        val primary = FlakyStore().apply { refuse = true }
        val store = FallbackSecretStore(primary)
        store.put(id, "secret")

        store.remove(id)

        assertNull(store.get(id))
    }
}
