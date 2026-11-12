package io.buszi.nascope.ui.root

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.connection.ObserveSessionUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase.Session
import io.buszi.nascope.domain.foreground.SetAppForegroundUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class AppRootViewModel(
    private val observeSession: ObserveSessionUseCase,
    private val setAppForeground: SetAppForegroundUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeSession()
            .onEach { session ->
                mutableViewState.update { it.copy(destination = session.toDestination()) }
            }
            .launchIn(viewModelScope)
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            is ViewAction.OnForegroundChanged -> setAppForeground(action.isForeground)
        }
    }

    private fun Session.toDestination(): Destination = when (this) {
        Session.NotConfigured -> Destination.Setup
        Session.Demo, is Session.Connected -> Destination.Main
    }

    @Stable
    data class ViewState(
        val destination: Destination = Destination.Loading,
    )

    sealed interface ViewAction {
        /** The app became visible (`true`) or stopped being visible (`false`). */
        data class OnForegroundChanged(val isForeground: Boolean) : ViewAction
    }

    /** The top-level flow to render. */
    enum class Destination {
        /** The session is not known yet: plain canvas, no flow. */
        Loading,

        /** Nothing is configured: welcome and connection setup, without navigation chrome. */
        Setup,

        /** A server is connected or demo mode is on: the dashboard inside the navigation scaffold. */
        Main,
    }
}
