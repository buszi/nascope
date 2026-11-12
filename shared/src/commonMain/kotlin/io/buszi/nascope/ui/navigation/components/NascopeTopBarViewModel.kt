package io.buszi.nascope.ui.navigation.components

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase.Session
import io.buszi.nascope.ui.common.model.ConnectionStatusUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class NascopeTopBarViewModel(
    private val observeSession: ObserveSessionUseCase,
    private val observeConnectionStatus: ObserveConnectionStatusUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeSessionAndStatus()
    }

    private fun observeSessionAndStatus() {
        combine(observeSession(), observeConnectionStatus()) { session, status ->
            when (session) {
                Session.NotConfigured -> ViewState(mode = Mode.None)
                Session.Demo -> ViewState(mode = Mode.Demo)
                is Session.Connected -> ViewState(
                    mode = Mode.Server,
                    nodeName = session.profile.alias,
                    connectionStatus = ConnectionStatusUi.from(status),
                )
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
    )

    /** What the top bar says about the data source. */
    enum class Mode {
        /** No data source yet: nothing to show. */
        None,

        /** Demo mode: a `DEMO` chip instead of a connection status. */
        Demo,

        /** A real server: its name, live status and a `SAMPLE DATA` tag while the bricks are still hardcoded. */
        Server,
    }
}
