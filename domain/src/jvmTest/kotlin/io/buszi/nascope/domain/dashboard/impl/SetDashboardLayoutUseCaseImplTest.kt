package io.buszi.nascope.domain.dashboard.impl

import io.buszi.nascope.domain.model.dashboard.DashboardBrickKey
import io.buszi.nascope.domain.model.dashboard.DashboardLayout
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

class SetDashboardLayoutUseCaseImplTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val preferencesStore = mockk<AppPreferencesStore>()

    private val useCase by lazy {
        SetDashboardLayoutUseCaseImpl(
            preferencesStore = preferencesStore,
            dispatcher = coroutineRule.dispatcher,
        )
    }

    @Test
    fun setDashboardLayoutUseCaseWritesTheLayout() = runTest {
        val layout = DashboardLayout(
            order = listOf(DashboardBrickKey("storage"), DashboardBrickKey("cpu")),
            hidden = setOf(DashboardBrickKey("cpu")),
        )
        coEvery { preferencesStore.setDashboardLayout(layout) } just runs

        useCase(layout)

        coVerifySequence {
            preferencesStore.setDashboardLayout(layout)
        }
    }

    @Test
    fun setDashboardLayoutUseCaseWritesTheDefaultOnReset() = runTest {
        coEvery { preferencesStore.setDashboardLayout(DashboardLayout.Default) } just runs

        useCase(DashboardLayout.Default)

        coVerifySequence {
            preferencesStore.setDashboardLayout(DashboardLayout.Default)
        }
    }
}
