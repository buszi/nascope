package io.buszi.nascope.ui.dashboard.framework

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BrickHeightTest {

    private val unit = 250.dp
    private val gutter = 12.dp

    @Test
    fun oneUnitEqualsUnitHeight() {
        assertEquals(250.dp, BrickHeight.Units(1).resolve(unit, gutter))
    }

    @Test
    fun twoUnitsIncludeOneGutter() {
        assertEquals(512.dp, BrickHeight.Units(2).resolve(unit, gutter))
    }

    @Test
    fun threeUnitsIncludeTwoGutters() {
        assertEquals(774.dp, BrickHeight.Units(3).resolve(unit, gutter))
    }

    @Test
    fun zeroUnitsAreRejected() {
        assertFailsWith<IllegalArgumentException> { BrickHeight.Units(0) }
    }

    @Test
    fun allRegisteredBricksDeclareAValidHeight() {
        BrickRegistry().getActiveBricks().forEach { brick ->
            val resolved = brick.height.resolve(unit, gutter)
            assert(resolved >= unit) { "${brick.id} resolves to $resolved" }
        }
    }
}
