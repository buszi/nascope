package io.buszi.nascope.ui.dashboard

import app.cash.turbine.turbineScope
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.dashboard.DashboardViewModel.ViewState
import io.buszi.nascope.ui.dashboard.framework.BrickRegistry
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewBrick
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class DashboardViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val brickRegistry = mockk<BrickRegistry>()

    private val viewModel by lazy {
        DashboardViewModel(
            brickRegistry = brickRegistry,
        )
    }

    @Test
    fun dashboardViewModelExposesActiveBricksFromRegistry() = runTest {
        turbineScope {
            every { brickRegistry.getActiveBricks() } returns listOf(SystemOverviewBrick)
            val viewStateTurbine = viewModel.viewState.testIn(this)

            assertEquals(
                listOf(
                    ViewState(bricks = listOf(SystemOverviewBrick))
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                brickRegistry.getActiveBricks()
            }
        }
    }
}
