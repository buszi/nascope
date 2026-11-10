package io.buszi.nascope.ui.dashboard.widgets.system

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.system.SystemOverviewViewModel.ViewAction.OnRetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SystemOverviewViewModel(
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
        }
    }

    private fun retry() {
        mutableViewState.update { it.copy(widgetState = WidgetState.Loading) }
        telemetryStateHolder.refresh()
    }

    private fun observeTelemetry() {
        telemetryStateHolder.hostOverview.onEach { overview ->
            mutableViewState.update {
                it.copy(
                    hostname = overview.hostname,
                    osName = overview.osName,
                    kernelRelease = overview.kernelRelease,
                    uptime = overview.uptime,
                    load1m = overview.load1m,
                    load5m = overview.load5m,
                    load15m = overview.load15m,
                    cpuCores = overview.cpuCores,
                    platform = HostPlatform.fromIdentifier(overview.osName),
                    widgetState = WidgetState.Ready,
                )
            }
        }.launchIn(viewModelScope)
    }

    @Stable
    data class ViewState(
        val hostname: String = "",
        val osName: String = "",
        val kernelRelease: String = "",
        val uptime: String = "",
        val load1m: Float = 0f,
        val load5m: Float = 0f,
        val load15m: Float = 0f,
        val cpuCores: Int = 0,
        val platform: HostPlatform = HostPlatform.Generic,
        val widgetState: WidgetState = WidgetState.Loading,
    )

    sealed interface ViewAction {
        data object OnRetry : ViewAction
    }
}
