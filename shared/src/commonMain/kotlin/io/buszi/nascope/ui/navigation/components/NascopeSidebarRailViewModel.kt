package io.buszi.nascope.ui.navigation.components

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase
import io.buszi.nascope.ui.common.model.SessionStatusUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.KoinViewModel

/** Drives the health dot of the sidebar rail from the live connection status. */
@KoinViewModel
class NascopeSidebarRailViewModel(
    observeSession: ObserveSessionUseCase,
    observeConnectionStatus: ObserveConnectionStatusUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        combine(observeSession(), observeConnectionStatus(), SessionStatusUi::from)
            .onEach { status -> mutableViewState.value = ViewState(sessionStatus = status) }
            .launchIn(viewModelScope)
    }

    @Stable
    data class ViewState(
        val sessionStatus: SessionStatusUi = SessionStatusUi.None,
    )
}
