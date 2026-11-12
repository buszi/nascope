package io.buszi.nascope.domain.connection.impl

import io.buszi.nascope.storage.AppPreferencesStore
import io.buszi.nascope.test.TestCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class SetDemoModeUseCaseImplTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val preferencesStore = mockk<AppPreferencesStore>()

    private val useCase by lazy {
        SetDemoModeUseCaseImpl(
            preferencesStore = preferencesStore,
            dispatcher = coroutineRule.dispatcher,
        )
    }

    @Test
    fun setDemoModeUseCaseWritesTheFlag() = runTest {
        coEvery { preferencesStore.setDemoMode(true) } just runs

        useCase(true)

        coVerifySequence {
            preferencesStore.setDemoMode(true)
        }
    }
}
