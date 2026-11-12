package io.buszi.nascope.ui.dashboard.widgets.system

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveSystemOverviewUseCase
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.ui.common.model.SystemOverviewUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.toWidgetState
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
    private val observeSystemOverview: ObserveSystemOverviewUseCase,
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
        observeSystemOverview().onEach { state ->
            mutableViewState.update {
                it.copy(
                    overview = when (state) {
                        is TelemetryState.Ready -> SystemOverviewUi.from(state.data)
                        TelemetryState.Loading -> SystemOverviewUi()
                        is TelemetryState.Failed -> it.overview
                    },
                    widgetState = state.toWidgetState(onRetry = refreshTelemetry::invoke),
                )
            }
        }.launchIn(viewModelScope)
    }

    @Stable
    data class ViewState(
        val overview: SystemOverviewUi = SystemOverviewUi(),
        val widgetState: WidgetState = WidgetState.Loading,
    )

    sealed interface ViewAction {
        data object OnRetry : ViewAction
    }
}
