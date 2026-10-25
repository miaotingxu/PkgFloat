package com.fly.pkg.service

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.fly.pkg.ForegroundPage
import com.fly.pkg.ForegroundPageRepository
import com.fly.pkg.PageNameUtils
import com.fly.pkg.SettingsStore
import com.fly.pkg.floatwin.FloatingWindowController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object ServiceState {
    val running = MutableStateFlow(false)
}

class PageDetectService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: FloatingWindowController? = null

    override fun onServiceConnected() {
        ServiceState.running.value = true
        ForegroundPageRepository.setFloatEnabled(SettingsStore(this).floatEnabled)
        scope.launch {
            ForegroundPageRepository.floatEnabled.collectLatest { enabled ->
                if (enabled) {
                    if (controller == null) {
                        controller = FloatingWindowController(this@PageDetectService).also { it.attach() }
                    }
                } else {
                    controller?.detach()
                    controller = null
                }
            }
        }
        scope.launch {
            ForegroundPageRepository.pageFlow.collectLatest { page ->
                if (page != null) controller?.update(page)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: return
        if (PageNameUtils.shouldIgnore(pkg, packageName)) return
        val fullClassName = PageNameUtils.resolveFullClassName(pkg, cls)
        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            pkg
        }
        ForegroundPageRepository.updatePage(ForegroundPage(pkg, fullClassName, appName))
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        ServiceState.running.value = false
        controller?.detach()
        controller = null
        scope.cancel()
        super.onDestroy()
    }
}
