package io.buszi.nascope.domain.model.connection

sealed interface ServerAuth {
    data object None : ServerAuth

    /** The password is not part of the profile; it lives in the secret store. */
    data class Basic(val username: String) : ServerAuth
}
