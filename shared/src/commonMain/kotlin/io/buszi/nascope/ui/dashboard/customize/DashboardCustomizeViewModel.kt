package io.buszi.nascope.ui.dashboard.customize

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.dashboard.ObserveDashboardLayoutUseCase
import io.buszi.nascope.domain.dashboard.SetDashboardLayoutUseCase
import io.buszi.nascope.domain.model.dashboard.DashboardLayout
import io.buszi.nascope.domain.telemetry.ObserveAvailablePluginsUseCase
import io.buszi.nascope.ui.common.model.DashboardCustomizeUi
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnCancelReset
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnConfirmReset
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnRequestReset
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnToggleBrick
import io.buszi.nascope.ui.dashboard.framework.BrickRegistry
import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * The dashboard editor. Its rows follow the stored layout and the server's plugins; every edit writes the complete
 * layout at once (there is no "Save"), so the dashboard beside the editor follows every change.
 */
@KoinViewModel
class DashboardCustomizeViewModel(
    private val brickRegistry: BrickRegistry,
    private val observeDashboardLayout: ObserveDashboardLayoutUseCase,
    private val setDashboardLayout: SetDashboardLayoutUseCase,
    private val observeAvailablePlugins: ObserveAvailablePluginsUseCase,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = mutableViewState.asStateFlow()

    init {
        combine(observeDashboardLayout(), observeAvailablePlugins()) { layout, plugins ->
            DashboardCustomizeUi.from(brickRegistry.allBricks, layout, plugins)
        }.onEach { customize ->
            mutableViewState.update {
                it.copy(
                    customize = customize,
                    // Reset by someone else (another window): nothing is left to confirm.
                    isConfirmingReset = it.isConfirmingReset && customize.isCustomized,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            is OnToggleBrick -> toggleBrick(action.id, action.enabled)
            OnRequestReset -> mutableViewState.update {
                it.copy(isConfirmingReset = it.customize?.isCustomized == true)
            }
            OnCancelReset -> mutableViewState.update { it.copy(isConfirmingReset = false) }
            OnConfirmReset -> confirmReset()
        }
    }

    private fun toggleBrick(id: DashboardBrickId, enabled: Boolean) {
        val current = mutableViewState.value.customize ?: return
        val edited = current.withEnabled(id, enabled)
        if (edited == current) return
        // Shown at once, so a second switch flipped before the write comes back builds on this one.
        mutableViewState.update { it.copy(customize = edited) }
        write(edited.toLayout())
    }

    private fun confirmReset() {
        if (!mutableViewState.value.isConfirmingReset) return
        mutableViewState.update { it.copy(isConfirmingReset = false) }
        write(DashboardLayout.Default)
    }

    private fun write(layout: DashboardLayout) {
        viewModelScope.launch { setDashboardLayout(layout) }
    }

    @Stable
    data class ViewState(
        /** The editor's rows; `null` until the stored layout has been read. */
        val customize: DashboardCustomizeUi? = null,
        /** Whether "Reset to default" waits for its confirmation. */
        val isConfirmingReset: Boolean = false,
    )

    sealed interface ViewAction {
        data class OnToggleBrick(val id: DashboardBrickId, val enabled: Boolean) : ViewAction
        data object OnRequestReset : ViewAction
        data object OnConfirmReset : ViewAction
        data object OnCancelReset : ViewAction
    }
}
