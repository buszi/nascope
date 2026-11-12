package io.buszi.nascope.ui.dashboard.widgets.storage

import io.buszi.nascope.domain.model.telemetry.DiskIoRate
import io.buszi.nascope.domain.model.telemetry.FileSystemUsage
import io.buszi.nascope.domain.telemetry.ObserveStorageUseCase
import io.buszi.nascope.domain.telemetry.ObserveStorageUseCase.Data
import io.buszi.nascope.ui.common.model.DiskIoUi
import io.buszi.nascope.ui.common.model.StorageMountUi
import io.buszi.nascope.ui.dashboard.widgets.storage.StorageDetailsViewModel.SortOrder
import io.buszi.nascope.ui.dashboard.widgets.storage.StorageDetailsViewModel.ViewAction.OnDisksPage
import io.buszi.nascope.ui.dashboard.widgets.storage.StorageDetailsViewModel.ViewAction.OnSelectSort
import io.buszi.nascope.ui.dashboard.widgets.storage.StorageDetailsViewModel.ViewAction.OnToggleMount
import io.buszi.nascope.ui.dashboard.widgets.storage.StorageDetailsViewModel.ViewState
import app.cash.turbine.turbineScope
import io.buszi.nascope.domain.model.connection.ConnectionFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryFailure
import io.buszi.nascope.domain.model.telemetry.TelemetryState
import io.buszi.nascope.domain.telemetry.RefreshTelemetryUseCase
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class StorageDetailsViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val observeStorage = mockk<ObserveStorageUseCase>()
    private val refreshTelemetry = mockk<RefreshTelemetryUseCase>()
    private val states = MutableStateFlow<TelemetryState<Data>>(TelemetryState.Loading)

    private val viewModel by lazy {
        every { observeStorage() } returns states
        StorageDetailsViewModel(observeStorage = observeStorage, refreshTelemetry = refreshTelemetry)
    }

    private suspend fun TestScope.viewStates(vararg steps: TelemetryState<Data>): List<ViewState> {
        var result: List<ViewState> = emptyList()
        turbineScope {
            val turbine = viewModel.viewState.testIn(this)
            runCurrent()
            steps.forEach {
                states.value = it
                runCurrent()
            }
            result = turbine.cancelAndConsumeItems()
        }
        return result
    }

    @Test
    fun storageDetailsViewModelStartsLoadingThenShowsMountsAndDiskIoWithHistory() = runTest {
        assertEquals(
            listOf(
                ViewState(),
                ViewState(mounts = listOf(small, big), diskIo = DiskIoUi(100, 50, listOf(1f, 2f), listOf(3f)), widgetState = WidgetState.Ready),
            ),
            viewStates(TelemetryState.Ready(data(), isStale = false)),
        )
    }

    @Test
    fun storageDetailsViewModelLeavesOutMissingDiskIoAndMapsFailureStates() = runTest {
        val states = viewStates(
            TelemetryState.Ready(data().copy(readBytesPerSec = null, writeBytesPerSec = null), false),
            TelemetryState.Ready(data(), true),
            TelemetryState.Failed(TelemetryFailure.PluginUnavailable),
            TelemetryState.Failed(TelemetryFailure.InvalidResponse("x")),
        )

        assertEquals(null, states[1].diskIo)
        assertEquals(WidgetState.Stale, states[2].widgetState)
        assertEquals(WidgetState.Unavailable, states[3].widgetState)
        assertIs<WidgetState.Error>(states[4].widgetState)
    }

    @Test
    fun storageDetailsViewModelSortsMounts() = runTest {
        viewModel // created now, so it collects what is emitted below
        states.value = TelemetryState.Ready(data(), false)
        runCurrent()

        assertEquals(listOf("/small", "/big"), viewModel.viewState.value.sortedMounts.map { it.mountPoint })
        viewModel.onViewAction(OnSelectSort(SortOrder.Name))
        assertEquals(listOf("/big", "/small"), viewModel.viewState.value.sortedMounts.map { it.mountPoint })
        viewModel.onViewAction(OnSelectSort(SortOrder.Size))
        assertEquals(listOf("/big", "/small"), viewModel.viewState.value.sortedMounts.map { it.mountPoint })
        viewModel.onViewAction(OnSelectSort(SortOrder.Usage))
        assertEquals(listOf("/small", "/big"), viewModel.viewState.value.sortedMounts.map { it.mountPoint })
    }

    private fun disk(name: String, read: Long) =
        ObserveStorageUseCase.Disk(DiskIoRate(name, read, 0), listOf(read.toFloat()), emptyList())

    @Test
    fun storageDetailsViewModelListsDisksBusiestFirstAndPagesThem() = runTest {
        viewModel
        val disks = (1..12).map { disk("d$it", it * 10L) }
        states.value = TelemetryState.Ready(data().copy(disks = disks), false)
        runCurrent()

        val first = viewModel.viewState.value
        assertEquals(12, first.disks.size)
        assertEquals(listOf("d12", "d11", "d10", "d9", "d8"), first.pagedDisks.map { it.name })
        assertEquals(3, first.disksPageCount)
        viewModel.onViewAction(OnDisksPage(1))
        assertEquals(listOf("d7", "d6", "d5", "d4", "d3"), viewModel.viewState.value.pagedDisks.map { it.name })
        viewModel.onViewAction(OnDisksPage(1))
        viewModel.onViewAction(OnDisksPage(1)) // clamped at the last page
        assertEquals(listOf("d2", "d1"), viewModel.viewState.value.pagedDisks.map { it.name })
        viewModel.onViewAction(OnDisksPage(-5))
        assertEquals(0, viewModel.viewState.value.disksPage)
    }

    @Test
    fun storageDetailsViewModelStepsBackWhenTheDisksShrinkAndHasNoDisksSectionWithoutDisks() = runTest {
        viewModel
        states.value = TelemetryState.Ready(data().copy(disks = (1..12).map { disk("d$it", it.toLong()) }), false)
        runCurrent()
        viewModel.onViewAction(OnDisksPage(1))
        viewModel.onViewAction(OnDisksPage(1))
        assertEquals(2, viewModel.viewState.value.disksPage)

        states.value = TelemetryState.Ready(data().copy(disks = listOf(disk("only", 1))), false)
        runCurrent()
        assertEquals(0, viewModel.viewState.value.disksPage)
        states.value = TelemetryState.Ready(data(), false)
        runCurrent()
        assertEquals(emptyList(), viewModel.viewState.value.disks)
    }

    @Test
    fun storageDetailsViewModelTogglesTheExpandedMountAndCollapsesOneThatDisappears() = runTest {
        viewModel // created now, so it collects what is emitted below
        states.value = TelemetryState.Ready(data(), false)
        runCurrent()

        viewModel.onViewAction(OnToggleMount("/small"))
        assertEquals("/small", viewModel.viewState.value.expandedMountPoint)
        viewModel.onViewAction(OnToggleMount("/small"))
        assertEquals(null, viewModel.viewState.value.expandedMountPoint)

        viewModel.onViewAction(OnToggleMount("/small"))
        states.value = TelemetryState.Ready(data().copy(fileSystems = listOf(bigFs)), false)
        runCurrent()
        assertEquals(null, viewModel.viewState.value.expandedMountPoint)
    }

    @Test
    fun storageDetailsViewStateComputesAggregates() {
        val viewState = ViewState(mounts = listOf(small, big))

        assertEquals(1_100L, viewState.totalCapacityBytes)
        assertEquals(590L, viewState.totalUsedBytes)
        assertEquals(510L, viewState.totalFreeBytes)
        assertEquals(590f / 1_100f, viewState.totalUsageRatio)
    }

    companion object {
        private val smallFs = FileSystemUsage("/small", null, null, 100, 90, 10, null)
        private val bigFs = FileSystemUsage("/big", "dev", "btrfs", 1_000, 500, 500, "rw")
        private val small = StorageMountUi("/small", "", "", 100, 90, 10)
        private val big = StorageMountUi("/big", "dev", "btrfs", 1_000, 500, 500, "rw")
        private fun data() = Data(listOf(smallFs, bigFs), emptyList(), 100, 50, listOf(1f, 2f), listOf(3f))
    }
}
