package com.fly.pagelens

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ForegroundPageRepository {

    private val _pageFlow = MutableStateFlow<ForegroundPage?>(null)
    val pageFlow: StateFlow<ForegroundPage?> = _pageFlow.asStateFlow()

    private val _floatEnabled = MutableStateFlow(false)
    val floatEnabled: StateFlow<Boolean> = _floatEnabled.asStateFlow()

    fun updatePage(page: ForegroundPage) {
        _pageFlow.value = page
    }

    fun setFloatEnabled(enabled: Boolean) {
        _floatEnabled.value = enabled
    }
}
