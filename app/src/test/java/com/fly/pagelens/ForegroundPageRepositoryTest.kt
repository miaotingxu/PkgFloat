package com.fly.pagelens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundPageRepositoryTest {

    @Test
    fun `updatePage emits new page`() {
        val page = ForegroundPage("com.tencent.mm", "com.tencent.mm.ui.LauncherUI", "微信")
        ForegroundPageRepository.updatePage(page)
        assertEquals(page, ForegroundPageRepository.pageFlow.value)
    }

    @Test
    fun `setFloatEnabled updates flow`() {
        ForegroundPageRepository.setFloatEnabled(true)
        assertTrue(ForegroundPageRepository.floatEnabled.value)
        ForegroundPageRepository.setFloatEnabled(false)
        assertFalse(ForegroundPageRepository.floatEnabled.value)
    }
}
