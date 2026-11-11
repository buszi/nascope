package io.buszi.nascope.di

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.plugin.module.dsl.koinConfiguration

/**
 * Starts logging and dependency injection. Every platform entry point calls this once, before any UI is created:
 * `Application.onCreate` on Android, the start of `main` on desktop.
 *
 * @param platformConfiguration extra Koin configuration supplied by the platform,
 * e.g. the Android application `Context`.
 */
fun initNascope(platformConfiguration: KoinAppDeclaration = {}) {
    Napier.base(DebugAntilog())
    startKoin(koinConfiguration<NascopeApp>(platformConfiguration))
}
