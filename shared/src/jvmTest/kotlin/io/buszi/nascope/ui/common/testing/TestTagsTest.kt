package io.buszi.nascope.ui.common.testing

import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestTagsTest {

    @Test
    fun brickAndDetailsTagsAreSnakeCasedBrickIds() {
        assertEquals("brick_cpu_matrix", TestTags.brick(DashboardBrickId.CpuMatrix))
        assertEquals("details_storage_pools", TestTags.details(DashboardBrickId.StoragePools))
        assertEquals("brick_network_io", TestTags.brick(DashboardBrickId.NetworkIo))
    }

    @Test
    fun everyBrickHasUniqueBrickAndDetailsTags() {
        val brickTags = DashboardBrickId.entries.map(TestTags::brick)
        val detailsTags = DashboardBrickId.entries.map(TestTags::details)

        assertEquals(brickTags.size, brickTags.toSet().size)
        assertEquals(detailsTags.size, detailsTags.toSet().size)
        assertTrue((brickTags + detailsTags).all { tag -> tag.all { it.isLowerCase() || it.isDigit() || it == '_' } })
    }

    @Test
    fun keyedTagsUseSnakeCaseOrSlugOfTheKey() {
        assertEquals("storage_sort_usage", TestTags.storageSort("Usage"))
        assertEquals("cpu_sort_frequency", TestTags.cpuSort("Frequency"))
        assertEquals("process_sort_memory", TestTags.processSort("Memory"))
        assertEquals("containers_filter_restarting", TestTags.containersFilter("Restarting"))
        assertEquals("process_filter_disk_sleep", TestTags.processFilter("DiskSleep"))
        assertEquals("nav_dashboard", TestTags.navigation("Dashboard"))
    }

    @Test
    fun setupFlowTagsAreSnakeCase() {
        assertEquals("welcome_connect", TestTags.WelcomeConnect)
        assertEquals("welcome_demo", TestTags.WelcomeDemo)
        assertEquals("welcome_screen", TestTags.WelcomeScreen)
    }

    @Test
    fun topBarTagsAreSnakeCase() {
        assertEquals("top_bar_node", TestTags.TopBarNode)
        assertEquals("top_bar_status", TestTags.TopBarStatus)
        assertEquals("top_bar_demo", TestTags.TopBarDemo)
        assertEquals("top_bar_sample_data", TestTags.TopBarSampleData)
    }

    @Test
    fun connectionEditorTagsAreSnakeCase() {
        assertEquals("connection_host", TestTags.ConnectionHost)
        assertEquals("connection_submit", TestTags.ConnectionSubmit)
        assertEquals("connection_protocol_https", TestTags.connectionProtocol("Https"))
        assertEquals("connection_check_reach_host", TestTags.connectionCheck("ReachHost"))
        assertEquals("connection_check_detect_glances", TestTags.connectionCheck("DetectGlances"))
    }

    @Test
    fun filterTagsWithoutStateMeanAll() {
        assertEquals("containers_filter_all", TestTags.containersFilter(null))
        assertEquals("process_filter_all", TestTags.processFilter(null))
    }

    @Test
    fun slugsAreLowercaseAlphanumericWithUnderscores() {
        assertEquals("storage_mount_root", TestTags.storageMount("/"))
        assertEquals("storage_mount_volume1", TestTags.storageMount("/volume1"))
        assertEquals("storage_mount_zpool_media", TestTags.storageMount("/zpool/media"))
        assertEquals("container_7f3b8a1c", TestTags.container("7f3b8a1c"))
        assertEquals("network_interface_eth0", TestTags.networkInterface("eth0"))
        assertEquals("process_row_1042", TestTags.processRow(1042))
        assertEquals("cpu_core_11", TestTags.cpuCore(11))
    }
}
