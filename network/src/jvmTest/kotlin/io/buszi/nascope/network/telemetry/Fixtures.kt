package io.buszi.nascope.network.telemetry

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.getOrElse
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import kotlin.test.assertIs
import kotlin.test.fail

object Fixtures {
    fun read(dir: String, plugin: String): String =
        checkNotNull(Fixtures::class.java.getResource("/glances/$dir/$plugin.json")) { "Missing fixture $dir/$plugin" }
            .readText()
}

fun <T> Result<T, TelemetryFailure>.ok(): T = getOrElse { fail("Expected success but was $it") }

fun Result<*, TelemetryFailure>.assertInvalid() {
    assertIs<TelemetryFailure.InvalidResponse>(getError() ?: fail("Expected failure"))
}
