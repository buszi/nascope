package io.buszi.nascope.ui.navigation.components

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase
import io.buszi.nascope.domain.connection.ObserveServerProfilesUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase.Session
import io.buszi.nascope.domain.connection.SetActiveServerUseCase
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.ui.common.model.ConnectionStatusUi
import io.buszi.nascope.ui.common.model.NodeUi
import io.buszi.nascope.ui.navigation.components.NascopeTopBarViewModel.ViewAction.OnSelectNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class NascopeTopBarViewModel(
    private val observeSession: ObserveSessionUseCase,
    private val observeConnectionStatus: ObserveConnectionStatusUseCase,
    private val observeServerProfiles: ObserveServerProfilesUseCase,
    private val setActiveServer: SetActiveServerUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeSessionAndStatus()
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            is OnSelectNode -> viewModelScope.launch { setActiveServer(ServerProfileId(action.id)) }
        }
    }

    private fun observeSessionAndStatus() {
        combine(observeSession(), observeConnectionStatus(), observeServerProfiles()) { session, status, profiles ->
            when (session) {
                Session.NotConfigured -> ViewState(mode = Mode.None)
                Session.Demo -> ViewState(mode = Mode.Demo)
                is Session.Connected -> {
                    val connectionStatus = ConnectionStatusUi.from(status)
                    ViewState(
                        mode = Mode.Server,
                        nodeName = session.profile.alias,
                        connectionStatus = connectionStatus,
                        nodes = NodeUi.from(profiles, session.profile.id, connectionStatus),
                    )
                }
            }
        }.onEach { viewState ->
            mutableViewState.value = viewState
        }.launchIn(viewModelScope)
    }

    @Stable
    data class ViewState(
        val mode: Mode = Mode.None,
        val nodeName: String = "",
        val connectionStatus: ConnectionStatusUi = ConnectionStatusUi.Connecting,
        /** The stored servers the quick switcher offers; the active one is marked. */
        val nodes: List<NodeUi> = emptyList(),
    )

    /** What the top bar says about the data source. */
    enum class Mode {
        /** No data source yet: nothing to show. */
        None,

        /** Demo mode: a `DEMO` chip instead of a connection status. */
        Demo,

        /** A real server: its name and live connection status. */
        Server,
    }

    sealed interface ViewAction {
        data class OnSelectNode(val id: String) : ViewAction
    }
}
