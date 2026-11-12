package io.buszi.nascope.ui.settings

import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.model.settings.RefreshInterval
import io.buszi.nascope.domain.settings.ObserveRefreshIntervalUseCase
import io.buszi.nascope.domain.settings.SetRefreshIntervalUseCase
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.model.RefreshIntervalUi
import io.buszi.nascope.ui.settings.SettingsViewModel.ViewAction.OnSelectRefreshInterval
import io.buszi.nascope.ui.settings.SettingsViewModel.ViewState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val observeRefreshInterval = mockk<ObserveRefreshIntervalUseCase>()
    private val setRefreshInterval = mockk<SetRefreshIntervalUseCase>()
    private val stored = MutableStateFlow(RefreshInterval.S2)

    private val viewModel by lazy {
        SettingsViewModel(
            observeRefreshInterval = observeRefreshInterval,
            setRefreshInterval = setRefreshInterval,
        )
    }

    @Test
    fun settingsViewModelShowsTheStoredIntervalAndFollowsChanges() = runTest {
        turbineScope {
            every { observeRefreshInterval() } returns stored
            stored.value = RefreshInterval.S5

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            stored.value = RefreshInterval.S10
            runCurrent()

            assertEquals(
                listOf(
                    ViewState(refreshInterval = RefreshIntervalUi.S2),
                    ViewState(refreshInterval = RefreshIntervalUi.S5),
                    ViewState(refreshInterval = RefreshIntervalUi.S10),
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
        }
    }

    @Test
    fun settingsViewModelSavesTheSelectedInterval() = runTest {
        every { observeRefreshInterval() } returns stored
        coEvery { setRefreshInterval(any()) } just runs

        viewModel.onViewAction(OnSelectRefreshInterval(RefreshIntervalUi.S10))
        runCurrent()

        coVerify(exactly = 1) { setRefreshInterval(RefreshInterval.S10) }
    }
}
