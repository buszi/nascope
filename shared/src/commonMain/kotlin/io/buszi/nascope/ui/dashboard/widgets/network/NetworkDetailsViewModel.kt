package io.buszi.nascope.ui.dashboard.widgets.network

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.NetworkInterfaceUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkDetailsViewModel.ViewAction.OnToggleInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

/**
 * Drives the network details screen: aggregate throughput and a card per interface with its
 * throughput history, peaks, link utilization and link properties. Cards are expanded by default
 * and can be collapsed individually.
 */
@KoinViewModel
class NetworkDetailsViewModel(
    private val telemetryStateHolder: TelemetryStateHolder,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeTelemetry()
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            is OnToggleInterface -> toggleInterface(action.name)
        }
    }

    private fun toggleInterface(name: String) {
        mutableViewState.update { state ->
            state.copy(
                collapsedNames = if (name in state.collapsedNames) {
                    state.collapsedNames - name
                } else {
                    state.collapsedNames + name
                },
            )
        }
    }

    private fun observeTelemetry() {
        telemetryStateHolder.networkIo.onEach { data ->
            mutableViewState.update { current ->
                val interfaces = data.interfaces.map(NetworkInterfaceUi::from)
                current.copy(
                    interfaces = interfaces,
                    collapsedNames = current.collapsedNames.filterTo(mutableSetOf()) { name ->
                        interfaces.any { it.name == name }
                    },
                    widgetState = WidgetState.Ready,
                )
            }
        }.launchIn(viewModelScope)
    }

    @Stable
    data class ViewState(
        val interfaces: List<NetworkInterfaceUi> = emptyList(),
        val collapsedNames: Set<String> = emptySet(),
        val widgetState: WidgetState = WidgetState.Loading,
    ) {
        val activeInterfaces: List<NetworkInterfaceUi>
            get() = interfaces.filter { it.isUp }

        val aggregateRxBytesPerSec: Long
            get() = activeInterfaces.sumOf { it.rxBytesPerSec }

        val aggregateTxBytesPerSec: Long
            get() = activeInterfaces.sumOf { it.txBytesPerSec }

        val totalRxBytes: Long
            get() = interfaces.sumOf { it.totalRxBytes }

        val totalTxBytes: Long
            get() = interfaces.sumOf { it.totalTxBytes }

        val overallStatus: StatusLevel
            get() = NetworkInterfaceUi.overallStatus(interfaces)

        fun isExpanded(name: String): Boolean = name !in collapsedNames
    }

    sealed interface ViewAction {
        data class OnToggleInterface(val name: String) : ViewAction
    }
}
