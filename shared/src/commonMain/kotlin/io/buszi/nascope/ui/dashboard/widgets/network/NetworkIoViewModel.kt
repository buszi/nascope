package io.buszi.nascope.ui.dashboard.widgets.network

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.data.telemetry.NetworkIoData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.NetworkInterfaceUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkIoViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.network.NetworkIoViewModel.ViewAction.OnSelectInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class NetworkIoViewModel(
    private val telemetryStateHolder: TelemetryStateHolder,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeTelemetry()
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            OnRetry -> retry()
            is OnSelectInterface -> selectInterface(action.name)
        }
    }

    private fun observeTelemetry() {
        telemetryStateHolder.networkIo
            .onEach { data ->
                mutableViewState.update { current ->
                    deriveViewState(data = data, currentSelectedName = current.selectedInterface?.name)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun deriveViewState(data: NetworkIoData, currentSelectedName: String?): ViewState {
        val interfaces = data.interfaces.map(NetworkInterfaceUi::from)
        if (interfaces.isEmpty()) {
            return ViewState(
                overallStatus = StatusLevel.Critical,
                interfaces = emptyList(),
                selectedInterface = null,
                aggregateRxBytesPerSec = 0L,
                aggregateTxBytesPerSec = 0L,
                activeInterfacesCount = 0,
                widgetState = WidgetState.Ready,
            )
        }

        val activeCount = interfaces.count { it.isUp }
        val overallStatus = when {
            activeCount == 0 -> StatusLevel.Critical
            activeCount < interfaces.size -> StatusLevel.Operational // Primary interfaces online, backup/wifi down is normal
            else -> StatusLevel.Operational
        }

        val targetName = currentSelectedName ?: data.selectedInterfaceName
        val selected = interfaces.firstOrNull { it.name == targetName }
            ?: interfaces.firstOrNull { it.isUp }
            ?: interfaces.firstOrNull()

        val aggregateRx = interfaces.filter { it.isUp }.sumOf { it.rxBytesPerSec }
        val aggregateTx = interfaces.filter { it.isUp }.sumOf { it.txBytesPerSec }

        return ViewState(
            overallStatus = overallStatus,
            interfaces = interfaces,
            selectedInterface = selected,
            aggregateRxBytesPerSec = aggregateRx,
            aggregateTxBytesPerSec = aggregateTx,
            activeInterfacesCount = activeCount,
            widgetState = WidgetState.Ready,
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

    private fun retry() {
        mutableViewState.update { it.copy(widgetState = WidgetState.Loading) }
        telemetryStateHolder.refresh()
    }

    @Stable
    data class ViewState(
        val overallStatus: StatusLevel = StatusLevel.Operational,
        val interfaces: List<NetworkInterfaceUi> = emptyList(),
        val selectedInterface: NetworkInterfaceUi? = null,
        val aggregateRxBytesPerSec: Long = 0L,
        val aggregateTxBytesPerSec: Long = 0L,
        val activeInterfacesCount: Int = 0,
        val widgetState: WidgetState = WidgetState.Ready,
    )

    sealed interface ViewAction {
        data object OnRetry : ViewAction
        data class OnSelectInterface(val name: String) : ViewAction
    }
}
