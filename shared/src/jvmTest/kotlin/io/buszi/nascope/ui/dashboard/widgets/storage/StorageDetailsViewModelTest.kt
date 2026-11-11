package io.buszi.nascope.ui.dashboard.widgets.storage

import app.cash.turbine.turbineScope
import io.buszi.nascope.data.telemetry.StorageMountData
import io.buszi.nascope.data.telemetry.StoragePoolsData
import io.buszi.nascope.data.telemetry.TelemetryStateHolder
import io.buszi.nascope.test.TestCoroutineRule
import io.buszi.nascope.test.cancelAndConsumeItems
import io.buszi.nascope.ui.common.components.badges.StatusLevel
import io.buszi.nascope.ui.common.model.StorageMountUi
import io.buszi.nascope.ui.dashboard.widgets.WidgetState
import io.buszi.nascope.ui.dashboard.widgets.storage.StorageDetailsViewModel.SortOrder
import io.buszi.nascope.ui.dashboard.widgets.storage.StorageDetailsViewModel.ViewAction.OnSelectSort
import io.buszi.nascope.ui.dashboard.widgets.storage.StorageDetailsViewModel.ViewAction.OnToggleMount
import io.buszi.nascope.ui.dashboard.widgets.storage.StorageDetailsViewModel.ViewState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class StorageDetailsViewModelTest {

    @get:Rule
    val coroutineRule = TestCoroutineRule()

    private val telemetryStateHolder = mockk<TelemetryStateHolder>()

    private val viewModel by lazy {
        StorageDetailsViewModel(
            telemetryStateHolder = telemetryStateHolder,
        )
    }

    @Test
    fun storageDetailsViewModelEmitsReadyStateWithTelemetryData() = runTest {
        turbineScope {
            every { telemetryStateHolder.storagePools } returns MutableStateFlow(storageData)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(
                listOf(
                    initialViewState,
                    readyViewState,
                ),
                viewStateTurbine.cancelAndConsumeItems(),
            )
            verifySequence {
                telemetryStateHolder.storagePools
            }
        }
    }

    @Test
    fun storageDetailsViewModelSortsMounts() = runTest {
        turbineScope {
            every { telemetryStateHolder.storagePools } returns MutableStateFlow(storageData)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            assertEquals(listOf("/zpool", "/data", "/"), viewModel.viewState.value.sortedMounts.map { it.mountPoint })

            viewModel.onViewAction(OnSelectSort(SortOrder.Name))
            assertEquals(listOf("/", "/data", "/zpool"), viewModel.viewState.value.sortedMounts.map { it.mountPoint })

            viewModel.onViewAction(OnSelectSort(SortOrder.Size))
            assertEquals(listOf("/zpool", "/data", "/"), viewModel.viewState.value.sortedMounts.map { it.mountPoint })

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun storageDetailsViewModelTogglesExpandedMount() = runTest {
        turbineScope {
            every { telemetryStateHolder.storagePools } returns MutableStateFlow(storageData)

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()

            viewModel.onViewAction(OnToggleMount("/data"))
            assertEquals("/data", viewModel.viewState.value.expandedMountPoint)

            viewModel.onViewAction(OnToggleMount("/zpool"))
            assertEquals("/zpool", viewModel.viewState.value.expandedMountPoint)

            viewModel.onViewAction(OnToggleMount("/zpool"))
            assertNull(viewModel.viewState.value.expandedMountPoint)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun storageDetailsViewModelCollapsesMountThatDisappears() = runTest {
        turbineScope {
            val storageFlow = MutableStateFlow(storageData)
            every { telemetryStateHolder.storagePools } returns storageFlow

            val viewStateTurbine = viewModel.viewState.testIn(this)
            runCurrent()
            viewModel.onViewAction(OnToggleMount("/data"))

            storageFlow.value = storageData.copy(mounts = storageData.mounts.filter { it.mountPoint != "/data" })
            runCurrent()

            assertNull(viewModel.viewState.value.expandedMountPoint)

            viewStateTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun storageDetailsViewStateComputesAggregates() {
        val state = readyViewState

        assertEquals(3_000_000_000L, state.totalCapacityBytes)
        assertEquals(2_300_000_000L, state.totalUsedBytes)
        assertEquals(700_000_000L, state.totalFreeBytes)
        assertEquals(StatusLevel.Critical, state.overallStatus)
    }

    companion object {
        private val rootMount = StorageMountData("/", "/dev/nvme0n1", "ext4", 500_000_000L, 100_000_000L, 400_000_000L, true)
        private val dataMount = StorageMountData("/data", "/dev/sda1", "btrfs", 1_000_000_000L, 700_000_000L, 300_000_000L, true)
        private val zpoolMount = StorageMountData("/zpool", "tank", "zfs", 1_500_000_000L, 1_500_000_000L, 0L, true)

        private val storageData = StoragePoolsData(
            mounts = listOf(rootMount, dataMount, zpoolMount),
            readRateBytesPerSec = 10_000_000L,
            writeRateBytesPerSec = 20_000_000L,
            readHistory = listOf(1f, 2f),
            writeHistory = listOf(3f, 4f),
        )

        private val initialViewState = ViewState()
        private val readyViewState = ViewState(
            mounts = storageData.mounts.map(StorageMountUi::from),
            readRateBytesPerSec = 10_000_000L,
            writeRateBytesPerSec = 20_000_000L,
            readHistory = listOf(1f, 2f),
            writeHistory = listOf(3f, 4f),
            widgetState = WidgetState.Ready,
        )
    }
}
