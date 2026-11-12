package io.buszi.nascope.domain.settings.impl

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.model.settings.RefreshInterval
import io.buszi.nascope.storage.AppPreferencesStore
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveRefreshIntervalUseCaseImplTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val preferencesStore = mockk<AppPreferencesStore>()

    private val useCase by lazy {
        ObserveRefreshIntervalUseCaseImpl(
            preferencesStore = preferencesStore,
            dispatcher = coroutineRule.dispatcher,
        )
    }

    @Test
    fun observeRefreshIntervalUseCaseEmitsTheStoredIntervalAndItsChanges() = runTest {
        turbineScope {
            val stored = MutableStateFlow(RefreshInterval.S2)
            every { preferencesStore.refreshInterval } returns stored

            val turbine = useCase().testIn(this)
            runCurrent()
            stored.value = RefreshInterval.S10
            runCurrent()

            assertEquals(listOf(RefreshInterval.S2, RefreshInterval.S10), turbine.cancelAndConsumeItems())
        }
    }
}
