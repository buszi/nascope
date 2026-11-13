package io.buszi.nascope.domain.model.connection

sealed interface ConnectionFailure {
    data object UnknownHost : ConnectionFailure
    data object ConnectionRefused : ConnectionFailure
    data object Timeout : ConnectionFailure
    data object TlsError : ConnectionFailure

    /** The host answers HTTP but not as a Glances API. */
    data object NotGlances : ConnectionFailure

    /** Missing or wrong credentials. */
    data object Unauthorized : ConnectionFailure

    /**
     * The profile uses a password, but none is stored for it (restored or moved to another device, cleared
     * keyring). Found before any request is sent.
     */
    data object PasswordMissing : ConnectionFailure

    data class Unexpected(val detail: String) : ConnectionFailure
}
