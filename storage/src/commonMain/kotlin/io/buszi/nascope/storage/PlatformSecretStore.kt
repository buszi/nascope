package io.buszi.nascope.storage

/** Returns the platform's secure secret store, or `null` when none is available. */
internal expect fun createPlatformSecretStore(directory: StorageDirectory): SecretStore?
