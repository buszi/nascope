package io.buszi.nascope.ui.dashboard.widgets.memory

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.memory.MemorySwapViewModel.ViewAction.OnRetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class MemorySwapViewModel(
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
        telemetryStateHolder.memorySwap.onEach { memoryData ->
            mutableViewState.update {
                it.copy(
                    totalMemBytes = memoryData.totalMemBytes,
                    usedMemBytes = memoryData.usedMemBytes,
                    cachedMemBytes = memoryData.cachedMemBytes,
                    buffersMemBytes = memoryData.buffersMemBytes,
                    freeMemBytes = memoryData.freeMemBytes,
                    totalSwapBytes = memoryData.totalSwapBytes,
                    usedSwapBytes = memoryData.usedSwapBytes,
                    freeSwapBytes = memoryData.freeSwapBytes,
                    widgetState = WidgetState.Ready,
                )
            }
        }.launchIn(viewModelScope)
    }

    @Stable
    data class ViewState(
        val totalMemBytes: Long = 0L,
        val usedMemBytes: Long = 0L,
        val cachedMemBytes: Long = 0L,
        val buffersMemBytes: Long = 0L,
        val freeMemBytes: Long = 0L,
        val totalSwapBytes: Long = 0L,
        val usedSwapBytes: Long = 0L,
        val freeSwapBytes: Long = 0L,
        val widgetState: WidgetState = WidgetState.Loading,
    )

    sealed interface ViewAction {
        data object OnRetry : ViewAction
    }
}
