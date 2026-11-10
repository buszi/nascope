package io.buszi.nascope.ui.dashboard.widgets.storage

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.buszi.nascope.data.telemetry.StorageMountData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
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
            OnNextPage -> nextPage()
            OnPrevPage -> prevPage()
        }
    }

    private fun retry() {
        mutableViewState.update { it.copy(widgetState = WidgetState.Loading) }
        telemetryStateHolder.refresh()
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
        telemetryStateHolder.storagePools.onEach { storageData ->
            mutableViewState.update { current ->
                val maxPage = if (storageData.mounts.isEmpty()) 0 else (storageData.mounts.size - 1) / current.pageSize
                current.copy(
                    mounts = storageData.mounts,
                    readRateBytesPerSec = storageData.readRateBytesPerSec,
                    writeRateBytesPerSec = storageData.writeRateBytesPerSec,
                    currentPage = current.currentPage.coerceIn(0, maxPage),
                    widgetState = WidgetState.Ready,
                )
            }
        }.launchIn(viewModelScope)
    }

    @Stable
    data class ViewState(
        val mounts: List<StorageMountData> = emptyList(),
        val readRateBytesPerSec: Long = 0L,
        val writeRateBytesPerSec: Long = 0L,
        val currentPage: Int = 0,
        val pageSize: Int = 3,
        val widgetState: WidgetState = WidgetState.Loading,
    ) {
        val totalPages: Int
            get() = if (mounts.isEmpty()) 1 else ((mounts.size + pageSize - 1) / pageSize)

        val displayedMounts: List<StorageMountData>
            get() {
                if (mounts.isEmpty()) return emptyList()
                val fromIndex = currentPage * pageSize
                val toIndex = minOf(fromIndex + pageSize, mounts.size)
                return if (fromIndex < mounts.size) mounts.subList(fromIndex, toIndex) else emptyList()
            }

        val overallStatus: StatusLevel
            get() {
                if (mounts.isEmpty()) return StatusLevel.Operational
                val hasCritical = mounts.any { mount ->
                    !mount.isHealthy || (mount.totalBytes > 0 && (mount.usedBytes.toFloat() / mount.totalBytes) >= 0.90f)
                }
                if (hasCritical) return StatusLevel.Critical

                val hasDegraded = mounts.any { mount ->
                    mount.totalBytes > 0 && (mount.usedBytes.toFloat() / mount.totalBytes) >= 0.80f
                }
                if (hasDegraded) return StatusLevel.Degraded

                return StatusLevel.Operational
            }

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
