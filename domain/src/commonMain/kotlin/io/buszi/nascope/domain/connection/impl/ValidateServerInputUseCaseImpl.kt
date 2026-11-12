package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.domain.connection.ValidateServerInputUseCase
import io.buszi.nascope.domain.connection.ValidateServerInputUseCase.AliasError
import io.buszi.nascope.domain.connection.ValidateServerInputUseCase.HostError
import io.buszi.nascope.domain.connection.ValidateServerInputUseCase.PortError
import org.koin.core.annotation.Factory

@Factory
internal class ValidateServerInputUseCaseImpl : ValidateServerInputUseCase {

    override fun invoke(alias: String, host: String, port: String) = ValidateServerInputUseCase.Result(
        alias = if (alias.isBlank()) AliasError.Blank else null,
        host = validateHost(host),
        port = validatePort(port),
    )

    private fun validateHost(host: String): HostError? {
        return when {
            host.isBlank() -> HostError.Blank
            "://" in host -> HostError.ContainsScheme
            host.any { it == '/' || it == '?' || it == '#' } -> HostError.ContainsPath
            host.any { it.isWhitespace() } -> HostError.ContainsWhitespace
            isValidHost(host) -> null
            else -> HostError.Invalid
        }
    }

    private fun validatePort(port: String): PortError? {
        return when {
            port.isEmpty() -> PortError.Blank
            !port.all { it in '0'..'9' } -> PortError.NotANumber
            (port.toIntOrNull() ?: Int.MAX_VALUE) !in MIN_PORT..MAX_PORT -> PortError.OutOfRange
            else -> null
        }
    }

    private fun isValidHost(host: String): Boolean = when {
        host.startsWith("[") -> host.endsWith("]") && isValidIpv6(host.substring(1, host.length - 1))
        ':' in host -> isValidIpv6(host)
        host.all { it in '0'..'9' || it == '.' } -> isValidIpv4(host)
        else -> isValidHostname(host)
    }

    private fun isValidIpv4(value: String): Boolean {
        val parts = value.split('.')
        return parts.size == 4 && parts.all { part ->
            part.isNotEmpty() && part.length <= 3 && (part.toIntOrNull() ?: -1) in 0..255
        }
    }

    private fun isValidIpv6(value: String): Boolean {
        if (value.isEmpty() || value.count { it == ':' } < 2) return false
        val compressed = value.split("::")
        if (compressed.size > 2) return false

        fun groups(part: String): Int? {
            if (part.isEmpty()) return 0
            val pieces = part.split(':')
            var count = 0
            for ((index, piece) in pieces.withIndex()) {
                val isLast = index == pieces.lastIndex
                count += when {
                    isLast && '.' in piece -> if (isValidIpv4(piece)) 2 else return null
                    piece.length in 1..4 && piece.all { it.isHexDigit() } -> 1
                    else -> return null
                }
            }
            return count
        }

        val total = compressed.sumOf { groups(it) ?: return false }
        return if (compressed.size == 2) total <= 7 else total == 8
    }

    private fun isValidHostname(value: String): Boolean {
        if (value.length > MAX_HOSTNAME_LENGTH) return false
        return value.split('.').all { label ->
            label.length in 1..MAX_LABEL_LENGTH &&
                !label.startsWith('-') && !label.endsWith('-') &&
                label.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '-' || it == '_' }
        }
    }

    private fun Char.isHexDigit() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    private companion object {
        const val MIN_PORT = 1
        const val MAX_PORT = 65535
        const val MAX_HOSTNAME_LENGTH = 253
        const val MAX_LABEL_LENGTH = 63
    }
}
