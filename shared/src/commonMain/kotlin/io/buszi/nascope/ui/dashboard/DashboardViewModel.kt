package io.buszi.nascope.ui.dashboard

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.connection.ObserveConnectionStatusUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase
import io.buszi.nascope.domain.connection.ObserveSessionUseCase.Session
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveAlertsUseCase
import io.buszi.nascope.domain.telemetry.ObserveAvailablePluginsUseCase
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.ui.common.model.AlertBadgeUi
import io.buszi.nascope.ui.common.model.AlertsUi
import io.buszi.nascope.ui.common.model.SessionStatusUi
import io.buszi.nascope.ui.dashboard.DashboardViewModel.ViewAction.OnRefresh
import io.buszi.nascope.ui.dashboard.framework.BrickRegistry
import io.buszi.nascope.ui.dashboard.framework.DashboardBrick
import io.buszi.nascope.ui.dashboard.framework.visibleWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

/**
 * The dashboard container knows the bricks the server can serve, the active server and its connection status. It does not own any
 * telemetry: every brick maps its own state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class DashboardViewModel(
    private val brickRegistry: BrickRegistry,
    private val observeSession: ObserveSessionUseCase,
    private val observeConnectionStatus: ObserveConnectionStatusUseCase,
    private val observeAvailablePlugins: ObserveAvailablePluginsUseCase,
    private val observeAlerts: ObserveAlertsUseCase,
    private val refreshTelemetry: RefreshTelemetryUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(
        ViewState(bricks = brickRegistry.getActiveBricks()),
    )
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        combine(observeSession(), observeConnectionStatus()) { session, status ->
            val profile = (session as? Session.Connected)?.profile
            Triple(profile?.id?.value, profile?.alias.orEmpty(), SessionStatusUi.from(session, status))
        }.onEach { (profileId, alias, status) ->
            mutableViewState.update { it.copy(activeProfileId = profileId, nodeName = alias, sessionStatus = status) }
        }.launchIn(viewModelScope)

        // Unknown plugins (loading, failed, server switched) show every brick; see `visibleWith`.
        observeAvailablePlugins().onEach { plugins ->
            mutableViewState.update { it.copy(bricks = brickRegistry.getActiveBricks().visibleWith(plugins)) }
        }.launchIn(viewModelScope)

        // The badge needs the alert plugin, so it is only polled once the server is known to run it.
        observeAvailablePlugins()
            .flatMapLatest { plugins -> if (plugins?.contains(AlertPlugin) == true) observeAlerts() else flowOf(null) }
            .onEach { state ->
                val badge = (state as? TelemetryState.Ready)?.let { AlertBadgeUi.from(AlertsUi.from(it.data)) }
                mutableViewState.update { it.copy(alertBadge = badge) }
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
        /** The active server's profile id, for opening its editor; `null` in demo mode and without a server. */
        val activeProfileId: String? = null,
        /** The active server's alias; empty in demo mode and without a server. */
        val nodeName: String = "",
        val sessionStatus: SessionStatusUi = SessionStatusUi.None,
        /** Active alerts; `null` when there are none and on hosts without the alert plugin. */
        val alertBadge: AlertBadgeUi? = null,
    )

    sealed interface ViewAction {
        data object OnRefresh : ViewAction
    }
}

private const val AlertPlugin = "alert"
