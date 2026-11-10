package io.buszi.nascope.ui.common.util

import androidx.compose.ui.unit.dp
import io.buszi.nascope.ui.common.theme.AppDimensions
import kotlin.test.Test
import kotlin.test.assertEquals

class WindowWidthClassTest {

    @Test
    fun classifiesDefaultDimensionsCorrectly() {
        val dimensions = AppDimensions(
            mobileBreakpoint = 768.dp,
            desktopBreakpoint = 1280.dp,
        )

        // Below mobile breakpoint -> Compact
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.fromWidth(360.dp, dimensions))
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.fromWidth(767.dp, dimensions))

        // Mobile to desktop breakpoint -> Medium
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.fromWidth(768.dp, dimensions))
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.fromWidth(1024.dp, dimensions))
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.fromWidth(1279.dp, dimensions))

        // At or above desktop breakpoint -> Expanded
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.fromWidth(1280.dp, dimensions))
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.fromWidth(1920.dp, dimensions))
    }

    @Test
    fun respectsCustomDimensionBreakpoints() {
        val customDimensions = AppDimensions(
            mobileBreakpoint = 600.dp,
            desktopBreakpoint = 1000.dp,
        )

        assertEquals(WindowWidthClass.Compact, WindowWidthClass.fromWidth(599.dp, customDimensions))
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.fromWidth(600.dp, customDimensions))
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.fromWidth(1000.dp, customDimensions))
    }
}
