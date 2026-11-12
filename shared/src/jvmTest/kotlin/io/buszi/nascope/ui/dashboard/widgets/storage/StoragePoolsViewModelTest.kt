package io.buszi.nascope.ui.dashboard.widgets.storage

import io.buszi.nascope.domain.model.telemetry.FileSystemUsage
import io.buszi.nascope.domain.telemetry.ObserveStorageUseCase
import io.buszi.nascope.domain.telemetry.ObserveStorageUseCase.Data
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.DiskIoUi
import io.buszi.nascope.ui.common.model.StorageMountUi
import io.buszi.nascope.ui.dashboard.widgets.storage.StoragePoolsViewModel.ViewAction.OnNextPage
import io.buszi.nascope.ui.dashboard.widgets.storage.StoragePoolsViewModel.ViewAction.OnPrevPage
import io.buszi.nascope.ui.dashboard.widgets.storage.StoragePoolsViewModel.ViewAction.OnRetry
import io.buszi.nascope.ui.dashboard.widgets.storage.StoragePoolsViewModel.ViewState
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
class StoragePoolsViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val observeStorage = mockk<ObserveStorageUseCase>()
    private val refreshTelemetry = mockk<RefreshTelemetryUseCase>()
    private val states = MutableStateFlow<TelemetryState<Data>>(TelemetryState.Loading)

    private val viewModel by lazy {
        every { observeStorage() } returns states
        StoragePoolsViewModel(observeStorage = observeStorage, refreshTelemetry = refreshTelemetry)
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
    fun storagePoolsViewModelStartsLoadingThenShowsMountsAndDiskIo() = runTest {
        assertEquals(
            listOf(ViewState(), ViewState(mounts = mounts(2), diskIo = diskIo, widgetState = WidgetState.Ready)),
            viewStates(TelemetryState.Ready(data(2), isStale = false)),
        )
    }

    @Test
    fun storagePoolsViewModelLeavesOutDiskIoWhenTheHostHasNone() = runTest {
        val last = viewStates(TelemetryState.Ready(data(1).copy(readBytesPerSec = null, writeBytesPerSec = null), false)).last()

        assertEquals(null, last.diskIo)
        assertEquals(WidgetState.Ready, last.widgetState)
    }

    @Test
    fun storagePoolsViewModelShowsAnEmptyListAsReady() = runTest {
        val last = viewStates(TelemetryState.Ready(data(0), false)).last()

        assertEquals(emptyList(), last.mounts)
        assertEquals(WidgetState.Ready, last.widgetState)
    }

    @Test
    fun storagePoolsViewModelMarksStaleKeepsTheContentAndMapsFailures() = runTest {
        val states = viewStates(
            TelemetryState.Ready(data(2), false),
            TelemetryState.Ready(data(2), true),
            TelemetryState.Failed(TelemetryFailure.PluginUnavailable),
            TelemetryState.Failed(TelemetryFailure.Connection(ConnectionFailure.Timeout)),
        )

        assertEquals(WidgetState.Stale, states[2].widgetState)
        assertEquals(mounts(2), states[2].mounts)
        assertEquals(WidgetState.Unavailable, states[3].widgetState)
        assertIs<WidgetState.Offline>(states[4].widgetState)
    }

    @Test
    fun storagePoolsViewModelDropsTheOldMountsWhenLoadingAgain() = runTest {
        assertEquals(ViewState(), viewStates(TelemetryState.Ready(data(2), false), TelemetryState.Loading).last())
    }

    @Test
    fun storagePoolsViewModelRetryRefreshesTelemetry() = runTest {
        every { refreshTelemetry() } just runs

        viewModel.onViewAction(OnRetry)

        verifySequence {
            observeStorage()
            refreshTelemetry()
        }
    }

    @Test
    fun storagePoolsViewModelPagesThroughMountsAndClampsAtTheEnds() = runTest {
        viewModel // created now, so it collects what is emitted below
        states.value = TelemetryState.Ready(data(7), false)
        runCurrent()

        viewModel.onViewAction(OnPrevPage)
        assertEquals(0, viewModel.viewState.value.currentPage)
        repeat(4) { viewModel.onViewAction(OnNextPage) }
        assertEquals(2, viewModel.viewState.value.currentPage)
        assertEquals(listOf("/m6"), viewModel.viewState.value.displayedMounts.map { it.mountPoint })
        assertEquals(3, viewModel.viewState.value.totalPages)

        states.value = TelemetryState.Ready(data(2), false)
        runCurrent()
        assertEquals(0, viewModel.viewState.value.currentPage)
    }

    @Test
    fun storagePoolsViewStateAggregatesCapacityAndStatus() {
        val viewState = ViewState(mounts = listOf(mount(0, used = 50), mount(1, used = 95)))

        assertEquals(200L, viewState.totalCapacityBytes)
        assertEquals(145L, viewState.totalUsedBytes)
        assertEquals(StatusLevel.Critical, viewState.overallStatus)
    }

    @Test
    fun storagePoolsViewStateCountsADeviceOnceWhenSeveralMountsShareIt() {
        val shared = listOf("/etc/hosts", "/etc/hostname", "/etc/resolv.conf")
            .map { StorageMountUi(it, "/dev/bcache0", "btrfs", 100, 65, 35) }
        val viewState = ViewState(mounts = shared + mount(1))

        assertEquals(200L, viewState.totalCapacityBytes)
        assertEquals(105L, viewState.totalUsedBytes)
    }

    companion object {
        private fun fileSystem(index: Int) = FileSystemUsage("/m$index", "dev$index", "ext4", 100, 40, 60, null)
        private fun mount(index: Int, used: Long = 40) = StorageMountUi("/m$index", "dev$index", "ext4", 100, used, 100 - used)
        private fun mounts(count: Int) = List(count) { mount(it) }
        private fun data(count: Int) = Data(
            fileSystems = List(count, ::fileSystem),
            disks = emptyList(),
            readBytesPerSec = 100,
            writeBytesPerSec = 50,
            readHistory = listOf(1f, 2f),
            writeHistory = listOf(3f),
        )
        private val diskIo = DiskIoUi(100, 50, listOf(1f, 2f), listOf(3f))
    }
}
