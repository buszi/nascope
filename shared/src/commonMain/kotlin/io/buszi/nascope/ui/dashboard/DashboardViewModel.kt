package io.buszi.nascope.ui.dashboard

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase.Session
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.ui.common.model.SessionStatusUi
import io.buszi.nascope.ui.dashboard.DashboardViewModel.ViewAction.OnRefresh
import io.buszi.nascope.ui.dashboard.framework.BrickRegistry
import io.buszi.nascope.ui.dashboard.framework.DashboardBrick
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

/**
 * The dashboard container knows the bricks, the active server and its connection status. It does not own any
 * telemetry: every brick maps its own state.
 */
@KoinViewModel
class DashboardViewModel(
    brickRegistry: BrickRegistry,
    private val observeSession: ObserveSessionUseCase,
    private val observeConnectionStatus: ObserveConnectionStatusUseCase,
    private val refreshTelemetry: RefreshTelemetryUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(
        ViewState(bricks = brickRegistry.getActiveBricks()),
    )
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        combine(observeSession(), observeConnectionStatus()) { session, status ->
            val alias = (session as? Session.Connected)?.profile?.alias.orEmpty()
            alias to SessionStatusUi.from(session, status)
        }.onEach { (alias, status) ->
            mutableViewState.update { it.copy(nodeName = alias, sessionStatus = status) }
        }.launchIn(viewModelScope)
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            OnRefresh -> refreshTelemetry()
        }
    }

    @Stable
    data class ViewState(
        val bricks: List<DashboardBrick> = emptyList(),
        /** The active server's alias; empty in demo mode and without a server. */
        val nodeName: String = "",
        val sessionStatus: SessionStatusUi = SessionStatusUi.None,
    )

    sealed interface ViewAction {
        data object OnRefresh : ViewAction
    }
}
