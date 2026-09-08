package com.easypocket.mobile.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreColorsTest {
    @Test
    fun `palette has 9 light and 9 dark colors`() {
        assertEquals(9, StoreColors.light.size)
        assertEquals(9, StoreColors.dark.size)
    }

    @Test
    fun `get returns color by index`() {
        assertEquals(Color(0xFFA1A1AA), StoreColors.get(0, isDark = false))
        assertEquals(Color(0xFFCA8A04), StoreColors.get(8, isDark = true))
    }

    @Test
    fun `get clamps out of range index`() {
        assertEquals(Color(0xFFA1A1AA), StoreColors.get(-1, isDark = false))
        assertEquals(Color(0xFF6B7280), StoreColors.get(99, isDark = true))
    }
}
