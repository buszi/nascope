package io.buszi.nascope.ui.nodes

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.connection.DeleteServerProfileUseCase
import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase
import io.buszi.nascope.domain.connection.ObserveServerProfilesUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase.Session
import io.buszi.nascope.domain.connection.SetActiveServerUseCase
import io.buszi.nascope.domain.connection.SetDemoModeUseCase
import io.buszi.nascope.domain.model.connection.ServerProfileId
import io.buszi.nascope.ui.common.model.ConnectionStatusUi
import io.buszi.nascope.ui.common.model.NodeUi
import io.buszi.nascope.ui.nodes.NodesViewModel.ViewAction.OnCancelDelete
import io.buszi.nascope.ui.nodes.NodesViewModel.ViewAction.OnConfirmDelete
import io.buszi.nascope.ui.nodes.NodesViewModel.ViewAction.OnExitDemo
import io.buszi.nascope.ui.nodes.NodesViewModel.ViewAction.OnRequestDelete
import io.buszi.nascope.ui.nodes.NodesViewModel.ViewAction.OnSetActive
import io.buszi.nascope.ui.nodes.NodesViewModel.ViewAction.OnUseDemo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class NodesViewModel(
    private val observeServerProfiles: ObserveServerProfilesUseCase,
    private val observeSession: ObserveSessionUseCase,
    private val observeConnectionStatus: ObserveConnectionStatusUseCase,
    private val setActiveServer: SetActiveServerUseCase,
    private val deleteServerProfile: DeleteServerProfileUseCase,
    private val setDemoMode: SetDemoModeUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeNodes()
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            is OnSetActive -> viewModelScope.launch { setActiveServer(ServerProfileId(action.id)) }
            is OnRequestDelete -> mutableViewState.update { it.copy(pendingDeleteId = action.id) }
            OnCancelDelete -> mutableViewState.update { it.copy(pendingDeleteId = null) }
            OnConfirmDelete -> confirmDelete()
            OnUseDemo -> viewModelScope.launch { setDemoMode(true) }
            OnExitDemo -> viewModelScope.launch { setDemoMode(false) }
        }
    }

    private fun confirmDelete() {
        val id = mutableViewState.value.pendingDeleteId ?: return
        mutableViewState.update { it.copy(pendingDeleteId = null) }
        viewModelScope.launch { deleteServerProfile(ServerProfileId(id)) }
    }

    private fun observeNodes() {
        combine(observeServerProfiles(), observeSession(), observeConnectionStatus()) { profiles, session, status ->
            val activeId = (session as? Session.Connected)?.profile?.id
            val nodes = NodeUi.from(profiles, activeId, ConnectionStatusUi.from(status))
            nodes to (session == Session.Demo)
        }.onEach { (nodes, isDemo) ->
            mutableViewState.update { state ->
                state.copy(
                    nodes = nodes,
                    isDemo = isDemo,
                    // A pending confirmation for a node that no longer exists is dropped.
                    pendingDeleteId = state.pendingDeleteId?.takeIf { id -> nodes.any { it.id == id } },
                    isLoading = false,
                )
            }
        }.launchIn(viewModelScope)
    }

    @Stable
    data class ViewState(
        val nodes: List<NodeUi> = emptyList(),
        val isDemo: Boolean = false,
        /** The node whose deletion awaits confirmation. */
        val pendingDeleteId: String? = null,
        val isLoading: Boolean = true,
    )

    sealed interface ViewAction {
        data class OnSetActive(val id: String) : ViewAction
        data class OnRequestDelete(val id: String) : ViewAction
        data object OnConfirmDelete : ViewAction
        data object OnCancelDelete : ViewAction
        data object OnUseDemo : ViewAction
        data object OnExitDemo : ViewAction
    }
}
