package io.buszi.nascope.ui.common.model

import io.buszi.nascope.domain.model.telemetry.FileSystemUsage
import io.buszi.nascope.domain.telemetry.ObserveStorageUseCase.Data
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StorageUiTest {

    private val fileSystem = FileSystemUsage("/", "sda1", "ext4", 100L, 40L, 60L, null)

    @Test
    fun mapsTheMountsAndTheDiskIo() {
        val ui = StorageUi.from(Data(listOf(fileSystem), emptyList(), 100L, 50L, listOf(1f), listOf(2f)))

        assertEquals(listOf(StorageMountUi.from(fileSystem)), ui.mounts)
        assertEquals(DiskIoUi(100L, 50L, listOf(1f), listOf(2f)), ui.diskIo)
    }

    @Test
    fun hasNoDiskIoWhenTheHostProvidesNone() {
        val ui = StorageUi.from(Data(listOf(fileSystem), emptyList(), null, null, emptyList(), emptyList()))

        assertNull(ui.diskIo)
    }
}
