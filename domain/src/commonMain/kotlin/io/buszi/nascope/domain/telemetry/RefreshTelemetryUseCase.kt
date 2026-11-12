package io.buszi.nascope.domain.telemetry

/** Makes every running telemetry loop poll now instead of waiting for its period. */
fun interface RefreshTelemetryUseCase {
    operator fun invoke()
}
