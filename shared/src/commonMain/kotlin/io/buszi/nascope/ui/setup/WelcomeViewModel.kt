package io.buszi.nascope.ui.setup

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.connection.SetDemoModeUseCase
import io.buszi.nascope.ui.setup.WelcomeViewModel.ViewAction.OnExploreDemo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class WelcomeViewModel(
    private val setDemoMode: SetDemoModeUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    fun onViewAction(action: ViewAction) {
        when (action) {
            OnExploreDemo -> exploreDemo()
        }
    }

    /**
     * Turns demo mode on. The root gate reacts to the new session and leaves the setup flow, so there is
     * no navigation here. A second tap while the write is in flight is ignored.
     */
    private fun exploreDemo() {
        if (mutableViewState.value.isStartingDemo) return
        mutableViewState.update { it.copy(isStartingDemo = true) }
        viewModelScope.launch {
            try {
                setDemoMode(true)
            } finally {
                mutableViewState.update { it.copy(isStartingDemo = false) }
            }
        }
    }

    @Stable
    data class ViewState(
        val isStartingDemo: Boolean = false,
    )

    sealed interface ViewAction {
        data object OnExploreDemo : ViewAction
    }
}
