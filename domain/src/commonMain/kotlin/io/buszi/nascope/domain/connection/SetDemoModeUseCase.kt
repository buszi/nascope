package io.buszi.nascope.domain.connection

fun interface SetDemoModeUseCase {
    suspend operator fun invoke(enabled: Boolean)
}
