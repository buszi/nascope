package io.buszi.nascope.storage

import io.buszi.nascope.domain.model.connection.ServerProfileId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

abstract class SecretStoreContract {

    protected abstract fun createStore(): SecretStore

    @Test
    fun unknownIdReturnsNull() = runTest {
        assertNull(createStore().get(ID_A))
    }

    @Test
    fun storedSecretCanBeRead() = runTest {
        val store = createStore()
        store.put(ID_A, "secret-a")
        assertEquals("secret-a", store.get(ID_A))
    }

    @Test
    fun twoIdsHoldDifferentSecretsIndependently() = runTest {
        val store = createStore()
        store.put(ID_A, "secret-a")
        store.put(ID_B, "secret-b")

        assertEquals("secret-a", store.get(ID_A))
        assertEquals("secret-b", store.get(ID_B))

        store.put(ID_A, "secret-a2")
        assertEquals("secret-a2", store.get(ID_A))
        assertEquals("secret-b", store.get(ID_B))
    }

    @Test
    fun removeAffectsOnlyItsOwnId() = runTest {
        val store = createStore()
        store.put(ID_A, "secret-a")
        store.put(ID_B, "secret-b")

        store.remove(ID_A)

        assertNull(store.get(ID_A))
        assertEquals("secret-b", store.get(ID_B))
    }

    @Test
    fun removingUnknownIdDoesNothing() = runTest {
        val store = createStore()
        store.remove(ID_A)
        assertNull(store.get(ID_A))
    }

    private companion object {
        val ID_A = ServerProfileId("a")
        val ID_B = ServerProfileId("b")
    }
}
