package io.buszi.nascope.ui.dashboard.widgets.storage

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.ObserveStorageUseCase
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.DiskIoUi
import io.buszi.nascope.ui.common.model.StorageMountUi
import io.buszi.nascope.ui.common.model.StorageUi
import io.buszi.nascope.ui.common.model.overallStatus
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.toWidgetState
import io.buszi.nascope.ui.dashboard.widgets.storage.StoragePoolsViewModel.ViewAction.OnNextPage
import io.buszi.nascope.ui.dashboard.widgets.storage.StoragePoolsViewModel.ViewAction.OnPrevPage
import io.buszi.nascope.ui.dashboard.widgets.storage.StoragePoolsViewModel.ViewAction.OnRetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class StoragePoolsViewModel(
    private val observeStorage: ObserveStorageUseCase,
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
            OnNextPage -> nextPage()
            OnPrevPage -> prevPage()
        }
    }

    private fun nextPage() {
        mutableViewState.update { state ->
            val maxPage = (state.totalPages - 1).coerceAtLeast(0)
            if (state.currentPage < maxPage) {
                state.copy(currentPage = state.currentPage + 1)
            } else {
                state
            }
        }
    }

    private fun prevPage() {
        mutableViewState.update { state ->
            if (state.currentPage > 0) {
                state.copy(currentPage = state.currentPage - 1)
            } else {
                state
            }
        }
    }

    private fun observeTelemetry() {
        observeStorage().onEach { state ->
            mutableViewState.update { current ->
                val storage = when (state) {
                    is TelemetryState.Ready -> StorageUi.from(state.data)
                    TelemetryState.Loading -> StorageUi()
                    is TelemetryState.Failed -> StorageUi(current.mounts, current.diskIo)
                }
                val maxPage = if (storage.mounts.isEmpty()) 0 else (storage.mounts.size - 1) / current.pageSize
                current.copy(
                    mounts = storage.mounts,
                    diskIo = storage.diskIo,
                    currentPage = current.currentPage.coerceIn(0, maxPage),
                    widgetState = state.toWidgetState(onRetry = refreshTelemetry::invoke),
                )
            }
        }.launchIn(viewModelScope)
    }

    @Stable
    data class ViewState(
        val mounts: List<StorageMountUi> = emptyList(),
        val diskIo: DiskIoUi? = null,
        val currentPage: Int = 0,
        val pageSize: Int = 3,
        val widgetState: WidgetState = WidgetState.Loading,
    ) {
        val totalPages: Int
            get() = if (mounts.isEmpty()) 1 else ((mounts.size + pageSize - 1) / pageSize)

        val displayedMounts: List<StorageMountUi>
            get() {
                if (mounts.isEmpty()) return emptyList()
                val fromIndex = currentPage * pageSize
                val toIndex = minOf(fromIndex + pageSize, mounts.size)
                return if (fromIndex < mounts.size) mounts.subList(fromIndex, toIndex) else emptyList()
            }

        val overallStatus: StatusLevel
            get() = mounts.overallStatus

        val totalCapacityBytes: Long
            get() = mounts.sumOf { it.totalBytes }

        val totalUsedBytes: Long
            get() = mounts.sumOf { it.usedBytes }
    }

    sealed interface ViewAction {
        data object OnRetry : ViewAction
        data object OnNextPage : ViewAction
        data object OnPrevPage : ViewAction
    }
}
