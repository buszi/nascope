package io.buszi.nascope.ui.dashboard.widgets.network

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveNetworkUseCase
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.NetworkInterfaceUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkIoViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkIoViewModel.ViewAction.OnSelectInterface
import io.buszi.nascope.ui.dashboard.widgets.toWidgetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class NetworkIoViewModel(
    private val observeNetwork: ObserveNetworkUseCase,
    private val refreshTelemetry: RefreshTelemetryUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeTelemetry()
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            OnRetry -> refreshTelemetry()
            is OnSelectInterface -> selectInterface(action.name)
        }
    }

    private fun observeTelemetry() {
        observeNetwork()
            .onEach { state ->
                val widgetState = state.toWidgetState(onRetry = refreshTelemetry::invoke)
                mutableViewState.update { current ->
                    when (state) {
                        is TelemetryState.Ready -> deriveViewState(
                            interfaces = NetworkInterfaceUi.fromAll(state.data),
                            selectedName = current.selectedInterface?.name,
                            widgetState = widgetState,
                        )
                        TelemetryState.Loading -> ViewState()
                        is TelemetryState.Failed -> current.copy(widgetState = widgetState)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /** The selection is kept across updates while the interface still exists. */
    private fun deriveViewState(
        interfaces: List<NetworkInterfaceUi>,
        selectedName: String?,
        widgetState: WidgetState,
    ): ViewState {
        if (interfaces.isEmpty()) {
            return ViewState(overallStatus = StatusLevel.Critical, widgetState = widgetState)
        }
        val active = interfaces.filter { it.isUp }
        val selected = interfaces.firstOrNull { it.name == selectedName }
            ?: active.firstOrNull()
            ?: interfaces.first()
        return ViewState(
            overallStatus = NetworkInterfaceUi.overallStatus(interfaces),
            interfaces = interfaces,
            selectedInterface = selected,
            aggregateRxBytesPerSec = active.sumOf { it.rxBytesPerSec },
            aggregateTxBytesPerSec = active.sumOf { it.txBytesPerSec },
            activeInterfacesCount = active.size,
            widgetState = widgetState,
        )
    }

    private fun selectInterface(name: String) {
        mutableViewState.update { current ->
            val match = current.interfaces.firstOrNull { it.name == name }
            if (match != null) {
                current.copy(selectedInterface = match)
            } else {
                current
            }
        }
    }

    @Stable
    data class ViewState(
        val overallStatus: StatusLevel = StatusLevel.Operational,
        val interfaces: List<NetworkInterfaceUi> = emptyList(),
        val selectedInterface: NetworkInterfaceUi? = null,
        val aggregateRxBytesPerSec: Long = 0L,
        val aggregateTxBytesPerSec: Long = 0L,
        val activeInterfacesCount: Int = 0,
        val widgetState: WidgetState = WidgetState.Loading,
    )

    sealed interface ViewAction {
        data object OnRetry : ViewAction
        data class OnSelectInterface(val name: String) : ViewAction
    }
}
