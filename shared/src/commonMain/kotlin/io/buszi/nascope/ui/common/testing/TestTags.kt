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

    // Setup flow
    const val WelcomeScreen: String = "welcome_screen"
    const val WelcomeConnect: String = "welcome_connect"
    const val WelcomeDemo: String = "welcome_demo"
    const val SetupBack: String = "setup_back"

    // Dashboard
    const val DashboardGrid: String = "dashboard_grid"
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
