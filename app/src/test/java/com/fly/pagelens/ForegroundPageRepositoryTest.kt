package com.fly.pagelens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun `updateFragment sets fragment when package and activity match`() {
        ForegroundPageRepository.updatePage(ForegroundPage("com.a", "com.a.MainActivity", "A"))
        ForegroundPageRepository.updateFragment("com.a", "com.a.MainActivity", "com.a.HomeFragment")
        assertEquals("com.a.HomeFragment", ForegroundPageRepository.pageFlow.value?.fragmentName)
    }

    @Test
    fun `updateFragment ignored when package mismatches`() {
        ForegroundPageRepository.updatePage(ForegroundPage("com.a", "com.a.MainActivity", "A"))
        ForegroundPageRepository.updateFragment("com.b", "com.b.MainActivity", "com.b.XFragment")
        assertNull(ForegroundPageRepository.pageFlow.value?.fragmentName)
    }

    @Test
    fun `updateFragment ignored when activity mismatches`() {
        ForegroundPageRepository.updatePage(ForegroundPage("com.a", "com.a.MainActivity", "A"))
        ForegroundPageRepository.updateFragment("com.a", "com.a.OtherActivity", "com.a.HomeFragment")
        assertNull(ForegroundPageRepository.pageFlow.value?.fragmentName)
    }

    @Test
    fun `updatePage clears previous fragment`() {
        ForegroundPageRepository.updatePage(ForegroundPage("com.a", "com.a.MainActivity", "A", "com.a.F"))
        ForegroundPageRepository.updatePage(ForegroundPage("com.b", "com.b.MainActivity", "B"))
        assertNull(ForegroundPageRepository.pageFlow.value?.fragmentName)
    }
}
