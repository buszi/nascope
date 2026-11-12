package io.buszi.nascope.domain.settings.impl

import io.buszi.nascope.domain.model.settings.RefreshInterval
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

class SetRefreshIntervalUseCaseImplTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val preferencesStore = mockk<AppPreferencesStore>()

    private val useCase by lazy {
        SetRefreshIntervalUseCaseImpl(
            preferencesStore = preferencesStore,
            dispatcher = coroutineRule.dispatcher,
        )
    }

    @Test
    fun setRefreshIntervalUseCaseWritesTheInterval() = runTest {
        coEvery { preferencesStore.setRefreshInterval(RefreshInterval.S5) } just runs

        useCase(RefreshInterval.S5)

        coVerifySequence {
            preferencesStore.setRefreshInterval(RefreshInterval.S5)
        }
    }
}
