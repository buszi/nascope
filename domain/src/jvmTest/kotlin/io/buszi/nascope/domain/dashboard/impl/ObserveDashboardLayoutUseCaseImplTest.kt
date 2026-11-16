package io.buszi.nascope.domain.dashboard.impl

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.model.dashboard.DashboardBrickKey
import io.buszi.nascope.domain.model.dashboard.DashboardLayout
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
class ObserveDashboardLayoutUseCaseImplTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val preferencesStore = mockk<AppPreferencesStore>()

    private val useCase by lazy {
        ObserveDashboardLayoutUseCaseImpl(
            preferencesStore = preferencesStore,
            dispatcher = coroutineRule.dispatcher,
        )
    }

    @Test
    fun observeDashboardLayoutUseCaseEmitsTheStoredLayoutAndItsChanges() = runTest {
        turbineScope {
            val custom = DashboardLayout(
                order = listOf(DashboardBrickKey("processes"), DashboardBrickKey("cpu")),
                hidden = setOf(DashboardBrickKey("gpu")),
            )
            val stored = MutableStateFlow(DashboardLayout.Default)
            every { preferencesStore.dashboardLayout } returns stored

            val turbine = useCase().testIn(this)
            runCurrent()
            stored.value = custom
            runCurrent()

            assertEquals(listOf(DashboardLayout.Default, custom), turbine.cancelAndConsumeItems())
        }
    }
}
