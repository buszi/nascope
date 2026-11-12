package io.buszi.nascope.domain.foreground.impl

import io.buszi.nascope.domain.foreground.AppForegroundState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifySequence
import kotlin.test.Test

class SetAppForegroundUseCaseImplTest {

    private val appForegroundState = mockk<AppForegroundState>()
    private val useCase = SetAppForegroundUseCaseImpl(appForegroundState)

    @Test
    fun setAppForegroundUseCaseForwardsBothValues() {
        every { appForegroundState.set(any()) } just runs

        useCase(true)
        useCase(false)

        verifySequence {
            appForegroundState.set(true)
            appForegroundState.set(false)
        }
    }
}
