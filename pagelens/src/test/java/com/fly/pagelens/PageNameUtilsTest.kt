package com.fly.pagelens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageNameUtilsTest {

    @Test
    fun `resolveFullClassName concatenates abbreviated class name`() {
        assertEquals(
            "com.tencent.mm.ui.LauncherUI",
            PageNameUtils.resolveFullClassName("com.tencent.mm", ".ui.LauncherUI")
        )
    }

    @Test
    fun `resolveFullClassName keeps already full class name`() {
        assertEquals(
            "com.tencent.mm.ui.LauncherUI",
            PageNameUtils.resolveFullClassName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI")
        )
    }

    @Test
    fun `shouldIgnore returns true for own package`() {
        assertTrue(PageNameUtils.shouldIgnore("com.fly.pagelens", "com.fly.pagelens"))
    }

    @Test
    fun `shouldIgnore returns false for other package`() {
        assertFalse(PageNameUtils.shouldIgnore("com.tencent.mm", "com.fly.pagelens"))
    }
}
