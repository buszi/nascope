package io.buszi.nascope.ui.common.model

import androidx.compose.runtime.Immutable
import io.buszi.nascope.domain.connection.ValidateServerInputUseCase.AliasError
import io.buszi.nascope.domain.connection.ValidateServerInputUseCase.HostError
import io.buszi.nascope.domain.connection.ValidateServerInputUseCase.PortError
import io.buszi.nascope.resources.Res
import io.buszi.nascope.resources.connection_error_alias_blank
import io.buszi.nascope.resources.connection_error_host_blank
import io.buszi.nascope.resources.connection_error_host_invalid
import io.buszi.nascope.resources.connection_error_host_path
import io.buszi.nascope.resources.connection_error_host_scheme
import io.buszi.nascope.resources.connection_error_host_whitespace
import io.buszi.nascope.resources.connection_error_port_blank
import io.buszi.nascope.resources.connection_error_port_not_a_number
import io.buszi.nascope.resources.connection_error_port_out_of_range
import io.buszi.nascope.resources.connection_error_username_blank
import org.jetbrains.compose.resources.StringResource

/** A validation problem of one server form field, with the message shown under it. */
@Immutable
enum class FormErrorUi(val messageRes: StringResource) {
    AliasBlank(Res.string.connection_error_alias_blank),
    HostBlank(Res.string.connection_error_host_blank),
    HostContainsScheme(Res.string.connection_error_host_scheme),
    HostContainsPath(Res.string.connection_error_host_path),
    HostContainsWhitespace(Res.string.connection_error_host_whitespace),
    HostInvalid(Res.string.connection_error_host_invalid),
    PortBlank(Res.string.connection_error_port_blank),
    PortNotANumber(Res.string.connection_error_port_not_a_number),
    PortOutOfRange(Res.string.connection_error_port_out_of_range),
    UsernameBlank(Res.string.connection_error_username_blank),
    ;

    companion object {
        fun from(data: AliasError): FormErrorUi = when (data) {
            AliasError.Blank -> AliasBlank
        }

        fun from(data: HostError): FormErrorUi = when (data) {
            HostError.Blank -> HostBlank
            HostError.ContainsScheme -> HostContainsScheme
            HostError.ContainsPath -> HostContainsPath
            HostError.ContainsWhitespace -> HostContainsWhitespace
            HostError.Invalid -> HostInvalid
        }

        fun from(data: PortError): FormErrorUi = when (data) {
            PortError.Blank -> PortBlank
            PortError.NotANumber -> PortNotANumber
            PortError.OutOfRange -> PortOutOfRange
        }
    }
}
