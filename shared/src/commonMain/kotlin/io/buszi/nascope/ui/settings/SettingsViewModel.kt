package io.buszi.nascope.ui.settings

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.settings.ObserveRefreshIntervalUseCase
import io.buszi.nascope.domain.settings.SetRefreshIntervalUseCase
import io.buszi.nascope.ui.common.model.RefreshIntervalUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SettingsViewModel(
    private val observeRefreshInterval: ObserveRefreshIntervalUseCase,
    private val setRefreshInterval: SetRefreshIntervalUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        observeRefreshInterval()
            .onEach { interval ->
                mutableViewState.update { it.copy(refreshInterval = RefreshIntervalUi.from(interval)) }
            }
            .launchIn(viewModelScope)
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            is ViewAction.OnSelectRefreshInterval ->
                viewModelScope.launch { setRefreshInterval(action.interval.toDomain()) }
        }
    }

    @Stable
    data class ViewState(
        val refreshInterval: RefreshIntervalUi = RefreshIntervalUi.S2,
    )

    sealed interface ViewAction {
        data class OnSelectRefreshInterval(val interval: RefreshIntervalUi) : ViewAction
    }
}
