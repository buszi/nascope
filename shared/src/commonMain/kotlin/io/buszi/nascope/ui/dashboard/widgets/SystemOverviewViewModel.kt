package io.buszi.nascope.ui.dashboard.widgets

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SystemOverviewViewModel(
    private val telemetryStateHolder: TelemetryStateHolder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SystemOverviewUiState())
    val uiState: StateFlow<SystemOverviewUiState> = _uiState.asStateFlow()

    init {
        observeTelemetry()
    }

    private fun observeTelemetry() {
        viewModelScope.launch {
            telemetryStateHolder.systemOverview.collect { telemetry ->
                _uiState.update { current ->
                    current.copy(
                        data = telemetry,
                        state = WidgetState.Ready,
                    )
                }
            }
        }
    }

    fun retry() {
        _uiState.update { it.copy(state = WidgetState.Loading) }
        telemetryStateHolder.refreshSystemOverview()
        _uiState.update { it.copy(state = WidgetState.Ready) }
    }

    @Immutable
    data class SystemOverviewUiState(
        val data: SystemOverviewData = SystemOverviewPreviewParameterProvider.UbuntuServer,
        val state: WidgetState = WidgetState.Ready,
    )
}
