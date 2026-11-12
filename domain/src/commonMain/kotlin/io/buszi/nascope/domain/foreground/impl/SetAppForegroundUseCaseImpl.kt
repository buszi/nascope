package io.buszi.nascope.domain.foreground.impl

import io.buszi.nascope.domain.foreground.AppForegroundState
import io.buszi.nascope.domain.foreground.SetAppForegroundUseCase
import org.koin.core.annotation.Factory

@Factory
internal class SetAppForegroundUseCaseImpl(
    private val appForegroundState: AppForegroundState,
) : SetAppForegroundUseCase {

    override fun invoke(isForeground: Boolean) = appForegroundState.set(isForeground)
}
