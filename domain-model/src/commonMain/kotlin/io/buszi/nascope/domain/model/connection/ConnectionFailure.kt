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

    data class Unexpected(val detail: String) : ConnectionFailure
}
