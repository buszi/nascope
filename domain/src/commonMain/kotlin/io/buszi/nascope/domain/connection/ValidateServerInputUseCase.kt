package io.buszi.nascope.domain.connection

fun interface ValidateServerInputUseCase {
    operator fun invoke(alias: String, host: String, port: String): Result

    data class Result(
        val alias: AliasError?,
        val host: HostError?,
        val port: PortError?,
    ) {
        val isValid: Boolean get() = alias == null && host == null && port == null
    }

    enum class AliasError { Blank }

    enum class HostError { Blank, ContainsScheme, ContainsPath, ContainsWhitespace, Invalid }

    enum class PortError { Blank, NotANumber, OutOfRange }
}
