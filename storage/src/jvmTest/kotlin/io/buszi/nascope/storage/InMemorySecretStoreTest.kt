package io.buszi.nascope.storage

import kotlin.test.Test
import kotlin.test.assertFalse

class InMemorySecretStoreTest : SecretStoreContract() {

    override fun createStore(): SecretStore = InMemorySecretStore()

    @Test
    fun isNotPersistent() {
        assertFalse(createStore().isPersistent)
    }
}
