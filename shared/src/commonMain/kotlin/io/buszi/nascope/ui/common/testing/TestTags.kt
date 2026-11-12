package io.buszi.nascope.ui.common.testing

import io.buszi.nascope.ui.dashboard.framework.DashboardBrickId

/**
 * Test tags identifying UI elements for automated UI checks.
 *
 * On Android the tags are exposed as accessibility resource ids (`testTagsAsResourceId`, enabled at the
 * activity root), so Appium / UiAutomator can locate elements with `By.id`. All tags are `snake_case`;
 * keyed tags append the snake-cased key (an enum name, an id, a mount point, ...).
 */
object TestTags {

    // Navigation chrome
    fun navigation(destinationName: String): String = "nav_${destinationName.snake()}"
    const val TopBarBack: String = "top_bar_back"
    const val TopBarNode: String = "top_bar_node"
    const val TopBarStatus: String = "top_bar_status"
    const val TopBarDemo: String = "top_bar_demo"
    const val NavRailStatus: String = "nav_rail_status"
    fun topBarNodeOption(id: String): String = "top_bar_node_option_${id.slug()}"
    const val TopBarManageNodes: String = "top_bar_manage_nodes"

    // Setup flow
    const val WelcomeScreen: String = "welcome_screen"
    const val WelcomeConnect: String = "welcome_connect"
    const val WelcomeDemo: String = "welcome_demo"

    // Nodes
    const val NodesScreen: String = "nodes_screen"
    const val NodesAdd: String = "nodes_add"
    fun nodesNode(id: String): String = "nodes_node_${id.slug()}"
    fun nodesUse(id: String): String = "nodes_use_${id.slug()}"
    fun nodesEdit(id: String): String = "nodes_edit_${id.slug()}"
    fun nodesDelete(id: String): String = "nodes_delete_${id.slug()}"
    const val NodesDeleteConfirm: String = "nodes_delete_confirm"
    const val NodesDeleteCancel: String = "nodes_delete_cancel"
    const val NodesDemoNotice: String = "nodes_demo_notice"
    const val NodesDemoConnect: String = "nodes_demo_connect"
    const val NodesDemoExit: String = "nodes_demo_exit"
    const val NodesUseDemo: String = "nodes_use_demo"

    // Connection editor
    const val ConnectionScreen: String = "connection_screen"
    const val ConnectionAlias: String = "connection_alias"
    fun connectionProtocol(protocolName: String): String = "connection_protocol_${protocolName.snake()}"
    const val ConnectionHost: String = "connection_host"
    const val ConnectionPort: String = "connection_port"
    const val ConnectionAuthToggle: String = "connection_auth_toggle"
    const val ConnectionUsername: String = "connection_username"
    const val ConnectionPassword: String = "connection_password"
    const val ConnectionSubmit: String = "connection_submit"
    const val ConnectionBack: String = "connection_back"
    fun connectionCheck(stepName: String): String = "connection_check_${stepName.snake()}"

    // Settings
    const val SettingsScreen: String = "settings_screen"
    fun settingsRefreshInterval(intervalName: String): String = "settings_refresh_interval_${intervalName.snake()}"

    // Dashboard
    const val DashboardGrid: String = "dashboard_grid"
    const val DashboardStatus: String = "dashboard_status"
    const val DashboardRefresh: String = "dashboard_refresh"
    fun brick(id: DashboardBrickId): String = "brick_${id.name.snake()}"

    // Shared details frame
    fun details(id: DashboardBrickId): String = "details_${id.name.snake()}"
    const val DetailsTitle: String = "details_title"
    const val DetailsClose: String = "details_close"
    const val DetailsScroll: String = "details_scroll"
    const val DetailsPagerPrevious: String = "details_pager_prev"
    const val DetailsPagerNext: String = "details_pager_next"
    const val DetailsPagerIndicator: String = "details_pager_indicator"

    // Storage details
    fun storageSort(orderName: String): String = "storage_sort_${orderName.snake()}"
    fun storageMount(mountPoint: String): String = "storage_mount_${mountPoint.slug()}"

    // CPU details
    fun cpuSort(orderName: String): String = "cpu_sort_${orderName.snake()}"
    fun cpuCore(index: Int): String = "cpu_core_$index"

    // Containers details
    fun containersFilter(stateName: String?): String = "containers_filter_${stateName?.snake() ?: "all"}"
    fun container(id: String): String = "container_${id.slug()}"

    // Process details
    const val ProcessSearch: String = "process_search"
    const val ProcessSearchClear: String = "process_search_clear"
    fun processFilter(stateName: String?): String = "process_filter_${stateName?.snake() ?: "all"}"
    fun processSort(fieldName: String): String = "process_sort_${fieldName.snake()}"
    fun processRow(pid: Int): String = "process_row_$pid"
    fun processPin(pid: Int): String = "process_pin_$pid"
    fun processPinned(pid: Int): String = "process_pinned_$pid"
    fun processUnpin(pid: Int): String = "process_unpin_$pid"

    // Network details
    fun networkInterface(name: String): String = "network_interface_${name.slug()}"

    private val CamelBoundary = Regex("([a-z0-9])([A-Z])")
    private val NonAlphanumeric = Regex("[^a-z0-9]+")

    /** `CpuMatrix` -> `cpu_matrix`. */
    private fun String.snake(): String = replace(CamelBoundary, "$1_$2").lowercase()

    /** Lowercase alphanumeric slug with single underscores; `"/volume1"` -> `volume1`, `"/"` -> `root`. */
    private fun String.slug(): String =
        lowercase().replace(NonAlphanumeric, "_").trim('_').ifEmpty { "root" }
}
