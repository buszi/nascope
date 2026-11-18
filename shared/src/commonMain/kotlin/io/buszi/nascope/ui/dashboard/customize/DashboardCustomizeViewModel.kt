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
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnDragCancel
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnDragMove
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnDragStart
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnDrop
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnMoveDown
import io.buszi.nascope.ui.dashboard.customize.DashboardCustomizeViewModel.ViewAction.OnMoveUp
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
 * layout at once (there is no "Save"), so the dashboard beside the editor follows every change. A drag is the exception:
 * it moves the rows in the state only and writes once, on drop.
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

    /** The rows as last stored, which a drag does not touch: what a cancelled drag goes back to. */
    private var stored: DashboardCustomizeUi? = null

    /** Whether a row is being dragged; the shown rows then follow the drag, not the store. */
    private var isDragging = false

    init {
        combine(observeDashboardLayout(), observeAvailablePlugins()) { layout, plugins ->
            DashboardCustomizeUi.from(brickRegistry.allBricks, layout, plugins)
        }.onEach { customize ->
            stored = customize
            mutableViewState.update {
                it.copy(
                    // A change from elsewhere during a drag waits for the drop, so the dragged list stays whole.
                    customize = if (isDragging) it.customize else customize,
                    // Reset by someone else (another window): nothing is left to confirm.
                    isConfirmingReset = it.isConfirmingReset && customize.isCustomized,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onViewAction(action: ViewAction) {
        when (action) {
            is OnToggleBrick -> edit { it.withEnabled(action.id, action.enabled) }
            is OnMoveUp -> edit { it.movedBy(action.id, offset = -1) }
            is OnMoveDown -> edit { it.movedBy(action.id, offset = 1) }
            OnDragStart -> isDragging = mutableViewState.value.customize != null
            is OnDragMove -> if (isDragging) {
                mutableViewState.update { state -> state.copy(customize = state.customize?.movedBy(action.id, action.offset)) }
            }
            OnDrop -> drop()
            OnDragCancel -> if (isDragging) {
                isDragging = false
                mutableViewState.update { it.copy(customize = stored) }
            }
            OnRequestReset -> mutableViewState.update {
                it.copy(isConfirmingReset = it.customize?.isCustomized == true)
            }
            OnCancelReset -> mutableViewState.update { it.copy(isConfirmingReset = false) }
            OnConfirmReset -> confirmReset()
        }
    }

    /** Applies [change] to the rows and writes the complete result; a change that changes nothing writes nothing. */
    private fun edit(change: (DashboardCustomizeUi) -> DashboardCustomizeUi) {
        // A switch or a keyboard move during a drag would write the half-dragged order.
        if (isDragging) return
        val current = mutableViewState.value.customize ?: return
        val edited = change(current)
        if (edited == current) return
        // Shown at once, so a second edit made before the write comes back builds on this one.
        mutableViewState.update { it.copy(customize = edited) }
        write(edited.toLayout())
    }

    /** The brick [id] moved [offset] places; at an end, or for a brick without a row, nothing changes. */
    private fun DashboardCustomizeUi.movedBy(id: DashboardBrickId, offset: Int): DashboardCustomizeUi {
        val from = rows.indexOfFirst { it.id == id }
        return moved(from = from, to = from + offset)
    }

    /** Writes the dragged order once, over the latest stored rows, so a switch changed elsewhere meanwhile survives. */
    private fun drop() {
        if (!isDragging) return
        isDragging = false
        val latest = stored ?: return
        val dragged = mutableViewState.value.customize ?: return
        val dropped = latest.inOrderOf(dragged.rows.map { it.id })
        mutableViewState.update { it.copy(customize = dropped) }
        if (dropped.rows != latest.rows) write(dropped.toLayout())
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
        data class OnMoveUp(val id: DashboardBrickId) : ViewAction
        data class OnMoveDown(val id: DashboardBrickId) : ViewAction

        /** A drag began; until [OnDrop] or [OnDragCancel] the rows move in the state only. */
        data object OnDragStart : ViewAction

        /** The dragged brick [id] passed a neighbour: [offset] is `-1` (up) or `1` (down). */
        data class OnDragMove(val id: DashboardBrickId, val offset: Int) : ViewAction
        data object OnDrop : ViewAction
        data object OnDragCancel : ViewAction
        data object OnRequestReset : ViewAction
        data object OnConfirmReset : ViewAction
        data object OnCancelReset : ViewAction
    }
}
