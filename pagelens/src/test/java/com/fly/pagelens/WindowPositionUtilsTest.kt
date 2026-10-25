package com.fly.pagelens

import org.junit.Assert.assertEquals
import org.junit.Test

class WindowPositionUtilsTest {

    @Test
    fun `position inside screen stays unchanged`() {
        assertEquals(
            100 to 200,
            WindowPositionUtils.clampPosition(100, 200, 1080, 1920, 300, 100)
        )
    }

    @Test
    fun `negative position clamps to zero`() {
        assertEquals(
            0 to 0,
            WindowPositionUtils.clampPosition(-50, -10, 1080, 1920, 300, 100)
        )
    }

    @Test
    fun `position beyond screen clamps to max`() {
        assertEquals(
            780 to 1820,
            WindowPositionUtils.clampPosition(2000, 5000, 1080, 1920, 300, 100)
        )
    }

    @Test
    fun `view larger than screen clamps to zero`() {
        assertEquals(
            0 to 0,
            WindowPositionUtils.clampPosition(500, 500, 1080, 1920, 2000, 3000)
        )
    }
}
