package io.buszi.nascope.ui.dashboard.framework

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BrickRegistryTest {

    private val registry = BrickRegistry()

    @Test
    fun everyBrickIdHasARegisteredBrick() {
        DashboardBrickId.entries.forEach { id ->
            assertEquals(id, assertNotNull(registry.findBrick(id)).id)
        }
    }

    @Test
    fun activeBricksHaveUniqueIds() {
        val ids = registry.getActiveBricks().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
