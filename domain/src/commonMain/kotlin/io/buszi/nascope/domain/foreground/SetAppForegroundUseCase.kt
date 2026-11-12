package io.buszi.nascope.domain.foreground

/** Reports whether the app is visible. Polling loops in `:domain` run only while it is. */
fun interface SetAppForegroundUseCase {
    operator fun invoke(isForeground: Boolean)
}
