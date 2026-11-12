package io.buszi.nascope.ui.dashboard.widgets.memory

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveMemoryUseCase
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.ui.common.model.MemorySwapUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.toWidgetState
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
    private val observeMemory: ObserveMemoryUseCase,
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
        }
    }

    private fun observeTelemetry() {
        observeMemory().onEach { state ->
            mutableViewState.update {
                it.copy(
                    memory = when (state) {
                        is TelemetryState.Ready -> MemorySwapUi.from(state.data)
                        TelemetryState.Loading -> MemorySwapUi()
                        is TelemetryState.Failed -> it.memory
                    },
                    widgetState = state.toWidgetState(onRetry = refreshTelemetry::invoke),
                )
            }
        }.launchIn(viewModelScope)
    }

    @Stable
    data class ViewState(
        val memory: MemorySwapUi = MemorySwapUi(),
        val widgetState: WidgetState = WidgetState.Loading,
    )

    sealed interface ViewAction {
        data object OnRetry : ViewAction
    }
}
