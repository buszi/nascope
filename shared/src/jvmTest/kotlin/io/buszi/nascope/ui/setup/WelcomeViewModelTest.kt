package io.buszi.nascope.ui.setup

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.connection.SetDemoModeUseCase
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.setup.WelcomeViewModel.ViewAction.OnExploreDemo
import io.buszi.nascope.ui.setup.WelcomeViewModel.ViewState
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val setDemoMode = mockk<SetDemoModeUseCase>()

    private val viewModel by lazy {
        WelcomeViewModel(
            setDemoMode = setDemoMode,
        )
    }

    @Test
    fun welcomeViewModelTurnsDemoModeOnAndBlocksActionsWhileWriting() = runTest {
        turbineScope {
            val write = CompletableDeferred<Unit>()
            coEvery { setDemoMode(true) } coAnswers { write.await() }

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnExploreDemo)
            runCurrent()
            write.complete(Unit)
            runCurrent()

            assertEquals(
                listOf(
                    initialViewState,
                    startingDemoViewState,
                    initialViewState,
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            coVerifySequence {
                setDemoMode(true)
            }
        }
    }

    @Test
    fun welcomeViewModelIgnoresSecondTapWhileWriting() = runTest {
        turbineScope {
            val write = CompletableDeferred<Unit>()
            coEvery { setDemoMode(true) } coAnswers { write.await() }

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnExploreDemo)
            runCurrent()
            viewModel.onViewAction(OnExploreDemo)
            runCurrent()
            write.complete(Unit)
            runCurrent()

            viewStateTurbine.cancelAndConsumeItems()
            coVerifySequence {
                setDemoMode(true)
            }
        }
    }

    companion object {
        private val initialViewState = ViewState(isStartingDemo = false)
        private val startingDemoViewState = ViewState(isStartingDemo = true)
    }
}
