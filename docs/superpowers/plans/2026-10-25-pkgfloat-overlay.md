# PkgFloat 悬浮窗实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现一个半透明悬浮窗调试工具，实时显示前台应用的包名和当前 Activity 完整类名。

**Architecture:** 无障碍服务（`PageDetectService`）监听系统窗口切换事件，解析出前台页面信息后写入单例 Repository（StateFlow）；`FloatingWindowController` 用 WindowManager 挂 ComposeView 悬浮窗并订阅 Repository 更新卡片。主界面（`MainActivity`）提供总开关和两项权限引导。

**Tech Stack:** Kotlin 2.2.10、Jetpack Compose（BOM 2026.02.01）、AccessibilityService、WindowManager、SharedPreferences。

**Spec:** `docs/superpowers/specs/2026-10-25-pkgfloat-overlay-design.md`

## Global Constraints

- 包名 `com.fly.pkg`，applicationId `com.fly.pkg`
- minSdk 23，targetSdk 36，compileSdk 36
- 悬浮窗样式：详情卡片（样式 B）——半透明深色圆角卡片，图标 + 应用名 + 等宽字体两行（完整包名 + Activity 完整类名）
- 悬浮窗权限声明 `SYSTEM_ALERT_WINDOW`；不声明网络权限；不使用前台服务和通知
- 本应用自身包名（`com.fly.pkg`）的窗口事件必须过滤，不显示自己
- 回到桌面照常显示 Launcher 信息；息屏不做特殊处理
- 提交信息用英文
- Windows 环境，Gradle 命令用 `./gradlew ...`（Git Bash）

---

### Task 1: 依赖与资源配置

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts:42-58`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/xml/accessibility_service_config.xml`

**Interfaces:**
- Produces: 依赖别名 `libs.androidx.lifecycle.viewmodel`、`libs.androidx.savedstate`；字符串资源 `accessibility_service_description`；无障碍配置 `@xml/accessibility_service_config`（Task 5 引用）

- [ ] **Step 1: 新增依赖**

`gradle/libs.versions.toml` 的 `[versions]` 段末尾加：

```toml
lifecycleViewmodel = "2.6.1"
savedstate = "1.2.1"
```

`[libraries]` 段末尾加：

```toml
androidx-lifecycle-viewmodel = { group = "androidx.lifecycle", name = "lifecycle-viewmodel", version.ref = "lifecycleViewmodel" }
androidx-savedstate = { group = "androidx.savedstate", name = "savedstate", version.ref = "savedstate" }
```

`app/build.gradle.kts` 的 `dependencies` 块中（`implementation(libs.androidx.lifecycle.runtime.ktx)` 之后）加：

```kotlin
implementation(libs.androidx.lifecycle.viewmodel)
implementation(libs.androidx.savedstate)
```

- [ ] **Step 2: Manifest 声明悬浮窗权限**

`app/src/main/AndroidManifest.xml` 中 `<manifest>` 标签内、`<application>` 之前加：

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

- [ ] **Step 3: 字符串与无障碍配置**

`app/src/main/res/values/strings.xml` 中加：

```xml
<string name="accessibility_service_description">监听前台窗口切换，用于在悬浮窗显示当前应用的页面信息</string>
```

新建 `app/src/main/res/xml/accessibility_service_config.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:description="@string/accessibility_service_description" />
```

- [ ] **Step 4: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml app/src/main/res/xml/accessibility_service_config.xml
git commit -m "Add dependencies, overlay permission and accessibility config"
```

---

### Task 2: PageNameUtils 纯函数（TDD）

**Files:**
- Create: `app/src/main/java/com/fly/pkg/PageNameUtils.kt`
- Test: `app/src/test/java/com/fly/pkg/PageNameUtilsTest.kt`

**Interfaces:**
- Produces:
  - `PageNameUtils.resolveFullClassName(packageName: String, className: String): String` — className 以 packageName 开头则原样返回，否则返回 `packageName + className`
  - `PageNameUtils.shouldIgnore(packageName: String, selfPackageName: String): Boolean` — 两者相等返回 true
  - Task 5 的 `PageDetectService` 消费这两个函数

- [ ] **Step 1: Write the failing test**

新建 `app/src/test/java/com/fly/pkg/PageNameUtilsTest.kt`：

```kotlin
package com.fly.pkg

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
        assertTrue(PageNameUtils.shouldIgnore("com.fly.pkg", "com.fly.pkg"))
    }

    @Test
    fun `shouldIgnore returns false for other package`() {
        assertFalse(PageNameUtils.shouldIgnore("com.tencent.mm", "com.fly.pkg"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.fly.pkg.PageNameUtilsTest"`
Expected: FAIL，编译错误 `Unresolved reference: PageNameUtils`

- [ ] **Step 3: Write minimal implementation**

新建 `app/src/main/java/com/fly/pkg/PageNameUtils.kt`：

```kotlin
package com.fly.pkg

object PageNameUtils {

    fun resolveFullClassName(packageName: String, className: String): String =
        if (className.startsWith(packageName)) className else packageName + className

    fun shouldIgnore(packageName: String, selfPackageName: String): Boolean =
        packageName == selfPackageName
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.fly.pkg.PageNameUtilsTest"`
Expected: PASS（4 个测试全绿）

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fly/pkg/PageNameUtils.kt app/src/test/java/com/fly/pkg/PageNameUtilsTest.kt
git commit -m "Add PageNameUtils for class name resolution and self filtering"
```

---

### Task 3: 数据层 — ForegroundPage、Repository、SettingsStore

**Files:**
- Create: `app/src/main/java/com/fly/pkg/ForegroundPage.kt`
- Create: `app/src/main/java/com/fly/pkg/ForegroundPageRepository.kt`
- Create: `app/src/main/java/com/fly/pkg/SettingsStore.kt`
- Test: `app/src/test/java/com/fly/pkg/ForegroundPageRepositoryTest.kt`

**Interfaces:**
- Produces:
  - `data class ForegroundPage(val packageName: String, val fullClassName: String, val appName: String)`
  - `ForegroundPageRepository.pageFlow: StateFlow<ForegroundPage?>`
  - `ForegroundPageRepository.floatEnabled: StateFlow<Boolean>`
  - `ForegroundPageRepository.updatePage(page: ForegroundPage)`
  - `ForegroundPageRepository.setFloatEnabled(enabled: Boolean)`
  - `SettingsStore(context: Context)`，属性 `floatEnabled: Boolean`、`windowX: Int`、`windowY: Int`（均可读写）；常量 `SettingsStore.DEFAULT_WINDOW_X = 24`、`DEFAULT_WINDOW_Y = 200`
  - Task 4、5、6 均消费以上接口

- [ ] **Step 1: Write the failing test**

新建 `app/src/test/java/com/fly/pkg/ForegroundPageRepositoryTest.kt`：

```kotlin
package com.fly.pkg

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
```

注意：Repository 是单例，测试会改动共享状态；每个测试都先写入再断言，不依赖初始值，因此互不影响。

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.fly.pkg.ForegroundPageRepositoryTest"`
Expected: FAIL，编译错误 `Unresolved reference: ForegroundPageRepository`

- [ ] **Step 3: Write minimal implementation**

新建 `app/src/main/java/com/fly/pkg/ForegroundPage.kt`：

```kotlin
package com.fly.pkg

data class ForegroundPage(
    val packageName: String,
    val fullClassName: String,
    val appName: String,
)
```

新建 `app/src/main/java/com/fly/pkg/ForegroundPageRepository.kt`：

```kotlin
package com.fly.pkg

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
```

新建 `app/src/main/java/com/fly/pkg/SettingsStore.kt`：

```kotlin
package com.fly.pkg

import android.content.Context

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var floatEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLOAT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_FLOAT_ENABLED, value).apply()

    var windowX: Int
        get() = prefs.getInt(KEY_WINDOW_X, DEFAULT_WINDOW_X)
        set(value) = prefs.edit().putInt(KEY_WINDOW_X, value).apply()

    var windowY: Int
        get() = prefs.getInt(KEY_WINDOW_Y, DEFAULT_WINDOW_Y)
        set(value) = prefs.edit().putInt(KEY_WINDOW_Y, value).apply()

    companion object {
        private const val PREFS_NAME = "pkgfloat"
        private const val KEY_FLOAT_ENABLED = "float_enabled"
        private const val KEY_WINDOW_X = "window_x"
        private const val KEY_WINDOW_Y = "window_y"
        const val DEFAULT_WINDOW_X = 24
        const val DEFAULT_WINDOW_Y = 200
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.fly.pkg.ForegroundPageRepositoryTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fly/pkg/ForegroundPage.kt app/src/main/java/com/fly/pkg/ForegroundPageRepository.kt app/src/main/java/com/fly/pkg/SettingsStore.kt app/src/test/java/com/fly/pkg/ForegroundPageRepositoryTest.kt
git commit -m "Add foreground page model, repository and settings store"
```

---

### Task 4: 悬浮窗 — LifecycleOwner、卡片 UI、WindowController

**Files:**
- Create: `app/src/main/java/com/fly/pkg/floatwin/FloatWindowLifecycleOwner.kt`
- Create: `app/src/main/java/com/fly/pkg/floatwin/FloatCardContent.kt`
- Create: `app/src/main/java/com/fly/pkg/floatwin/FloatingWindowController.kt`

**Interfaces:**
- Consumes: `ForegroundPage`（Task 3）、`SettingsStore`（Task 3）
- Produces:
  - `FloatingWindowController(context: Context)`，方法 `attach()`、`detach()`、`update(page: ForegroundPage)` — Task 5 消费
  - `FloatWindowLifecycleOwner`，方法 `onWindowAttach()`、`onWindowDetach()` — 仅供 Controller 内部使用

- [ ] **Step 1: 实现 FloatWindowLifecycleOwner**

ComposeView 挂在 WindowManager 里时没有 Activity 提供的 owner，需手动挂载。新建 `app/src/main/java/com/fly/pkg/floatwin/FloatWindowLifecycleOwner.kt`：

```kotlin
package com.fly.pkg.floatwin

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

class FloatWindowLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun onWindowAttach() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onWindowDetach() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
```

- [ ] **Step 2: 实现卡片与圆点 UI**

新建 `app/src/main/java/com/fly/pkg/floatwin/FloatCardContent.kt`：

```kotlin
package com.fly.pkg.floatwin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fly.pkg.ForegroundPage
import kotlin.math.roundToInt

private val CardBackground = Color(0x8C141414)
private val CardBorder = Color(0x26FFFFFF)
private val MonoText = Color(0xFF9ECBFF)

@Composable
fun FloatCard(
    page: ForegroundPage?,
    icon: ImageBitmap?,
    onCopy: () -> Unit,
    onDragBy: (Int, Int) -> Unit,
    onDragEnd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onCopy() })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragBy(dragAmount.x.roundToInt(), dragAmount.y.roundToInt())
                    },
                    onDragEnd = { onDragEnd() },
                )
            }
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = page?.appName ?: "等待页面切换…",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (page != null) {
            Spacer(Modifier.height(6.dp))
            Text(page.packageName, color = MonoText, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Text(page.fullClassName, color = MonoText, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
    }
}

@Composable
fun FloatDot(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(CardBackground)
            .border(1.dp, CardBorder, CircleShape)
            .clickable(onClick = onClick),
    )
}
```

- [ ] **Step 3: 实现 FloatingWindowController**

新建 `app/src/main/java/com/fly/pkg/floatwin/FloatingWindowController.kt`：

```kotlin
package com.fly.pkg.floatwin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.fly.pkg.ForegroundPage
import com.fly.pkg.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow

class FloatingWindowController(private val context: Context) {

    private data class CardContent(val page: ForegroundPage?, val icon: ImageBitmap?)

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val settings = SettingsStore(context)
    private val lifecycleOwner = FloatWindowLifecycleOwner()
    private val contentFlow = MutableStateFlow(CardContent(null, null))
    private val iconCache = mutableMapOf<String, ImageBitmap?>()

    private var cardView: ComposeView? = null
    private var dotView: ComposeView? = null
    private var currentPage: ForegroundPage? = null

    fun attach() {
        if (!Settings.canDrawOverlays(context)) return
        lifecycleOwner.onWindowAttach()
        showCard()
    }

    fun detach() {
        removeView(cardView)
        cardView = null
        removeView(dotView)
        dotView = null
        lifecycleOwner.onWindowDetach()
    }

    fun update(page: ForegroundPage) {
        currentPage = page
        contentFlow.value = CardContent(page, loadIcon(page.packageName))
    }

    private fun newWindowParams(x: Int, y: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }

    private fun newComposeView(content: @Composable () -> Unit): ComposeView =
        ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent { content() }
        }

    private fun removeView(view: ComposeView?) {
        if (view != null && view.isAttachedToWindow) windowManager.removeView(view)
    }

    private fun showCard() {
        removeView(dotView)
        dotView = null
        if (cardView != null) return
        val view = newComposeView {
            val content by contentFlow.collectAsState()
            FloatCard(
                page = content.page,
                icon = content.icon,
                onCopy = { copyCurrentClassName() },
                onDragBy = { dx, dy -> moveCard(dx, dy) },
                onDragEnd = { onCardDragEnd() },
            )
        }
        cardView = view
        windowManager.addView(view, newWindowParams(settings.windowX, settings.windowY))
    }

    private fun showDot(edgeX: Int, y: Int) {
        removeView(cardView)
        cardView = null
        if (dotView != null) return
        val view = newComposeView {
            FloatDot(onClick = { showCard() })
        }
        dotView = view
        windowManager.addView(view, newWindowParams(edgeX, y))
    }

    private fun moveCard(dx: Int, dy: Int) {
        val view = cardView ?: return
        val lp = view.layoutParams as WindowManager.LayoutParams
        lp.x += dx
        lp.y += dy
        windowManager.updateViewLayout(view, lp)
    }

    private fun onCardDragEnd() {
        val view = cardView ?: return
        val lp = view.layoutParams as WindowManager.LayoutParams
        settings.windowX = lp.x
        settings.windowY = lp.y
        val metrics = context.resources.displayMetrics
        val threshold = (48 * metrics.density).toInt()
        when {
            lp.x <= threshold -> showDot(edgeX = 0, y = lp.y)
            lp.x + view.width >= metrics.widthPixels - threshold ->
                showDot(edgeX = metrics.widthPixels - (36 * metrics.density).toInt(), y = lp.y)
        }
    }

    private fun copyCurrentClassName() {
        val page = currentPage ?: return
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("activity", page.fullClassName))
        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
    }

    private fun loadIcon(packageName: String): ImageBitmap? =
        iconCache.getOrPut(packageName) {
            try {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap()
                    .asImageBitmap()
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
}
```

- [ ] **Step 4: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fly/pkg/floatwin/
git commit -m "Add floating window controller with draggable collapsible card"
```

---

### Task 5: PageDetectService 无障碍服务

**Files:**
- Create: `app/src/main/java/com/fly/pkg/service/PageDetectService.kt`
- Modify: `app/src/main/AndroidManifest.xml`（`<application>` 内加 service 声明）

**Interfaces:**
- Consumes: `PageNameUtils.*`（Task 2）、`ForegroundPage` / `ForegroundPageRepository` / `SettingsStore`（Task 3）、`FloatingWindowController(context).attach() / detach() / update(page)`（Task 4）、`@xml/accessibility_service_config`（Task 1）
- Produces:
  - `class PageDetectService : AccessibilityService`
  - `ServiceState.running: MutableStateFlow<Boolean>`（Task 6 消费）

- [ ] **Step 1: 实现服务**

新建 `app/src/main/java/com/fly/pkg/service/PageDetectService.kt`：

```kotlin
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
```

- [ ] **Step 2: Manifest 声明服务**

`app/src/main/AndroidManifest.xml` 的 `<application>` 内、`<activity>` 之后加：

```xml
<service
    android:name=".service.PageDetectService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

- [ ] **Step 3: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/fly/pkg/service/PageDetectService.kt app/src/main/AndroidManifest.xml
git commit -m "Add accessibility service for foreground page detection"
```

---

### Task 6: 主界面 — 总开关、权限卡片、服务状态

**Files:**
- Create: `app/src/main/java/com/fly/pkg/PermissionChecks.kt`
- Create: `app/src/main/java/com/fly/pkg/ui/MainScreen.kt`
- Modify: `app/src/main/java/com/fly/pkg/MainActivity.kt`（整体替换）

**Interfaces:**
- Consumes: `ForegroundPageRepository.floatEnabled` / `setFloatEnabled`（Task 3）、`SettingsStore`（Task 3）、`ServiceState.running`（Task 5）
- Produces: `PermissionChecks.isAccessibilityServiceEnabled(context: Context): Boolean`

- [ ] **Step 1: 实现 PermissionChecks**

新建 `app/src/main/java/com/fly/pkg/PermissionChecks.kt`：

```kotlin
package com.fly.pkg

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.fly.pkg.service.PageDetectService

object PermissionChecks {

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, PageDetectService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
```

- [ ] **Step 2: 实现 MainScreen**

新建 `app/src/main/java/com/fly/pkg/ui/MainScreen.kt`：

```kotlin
package com.fly.pkg.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(
    floatEnabled: Boolean,
    overlayGranted: Boolean,
    a11yEnabled: Boolean,
    serviceRunning: Boolean,
    onToggleFloat: (Boolean) -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenA11ySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("悬浮窗总开关", style = MaterialTheme.typography.titleMedium)
            Switch(checked = floatEnabled, onCheckedChange = onToggleFloat)
        }

        Spacer(Modifier.height(24.dp))

        PermissionCard(
            title = "悬浮窗权限",
            granted = overlayGranted,
            highlight = floatEnabled && !overlayGranted,
            onOpenSettings = onOpenOverlaySettings,
        )

        Spacer(Modifier.height(12.dp))

        PermissionCard(
            title = "无障碍服务",
            granted = a11yEnabled,
            highlight = floatEnabled && !a11yEnabled,
            onOpenSettings = onOpenA11ySettings,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = if (serviceRunning) "检测服务运行中" else "检测服务未运行（请在无障碍设置中开启）",
            color = if (serviceRunning) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    granted: Boolean,
    highlight: Boolean,
    onOpenSettings: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (granted) "已开启" else "未开启",
                    color = when {
                        granted -> MaterialTheme.colorScheme.primary
                        highlight -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (!granted) {
                Button(onClick = onOpenSettings) { Text("去开启") }
            }
        }
    }
}
```

- [ ] **Step 3: 重写 MainActivity**

整体替换 `app/src/main/java/com/fly/pkg/MainActivity.kt`：

```kotlin
package com.fly.pkg

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.fly.pkg.service.ServiceState
import com.fly.pkg.ui.MainScreen
import com.fly.pkg.ui.theme.PkgFloatTheme

class MainActivity : ComponentActivity() {

    private val overlayGranted = mutableStateOf(false)
    private val a11yEnabled = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ForegroundPageRepository.setFloatEnabled(SettingsStore(this).floatEnabled)
        setContent {
            PkgFloatTheme {
                val floatEnabled by ForegroundPageRepository.floatEnabled.collectAsState()
                val serviceRunning by ServiceState.running.collectAsState()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        floatEnabled = floatEnabled,
                        overlayGranted = overlayGranted.value,
                        a11yEnabled = a11yEnabled.value,
                        serviceRunning = serviceRunning,
                        onToggleFloat = { enabled ->
                            SettingsStore(this).floatEnabled = enabled
                            ForegroundPageRepository.setFloatEnabled(enabled)
                        },
                        onOpenOverlaySettings = {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName"),
                                )
                            )
                        },
                        onOpenA11ySettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayGranted.value = Settings.canDrawOverlays(this)
        a11yEnabled.value = PermissionChecks.isAccessibilityServiceEnabled(this)
    }
}
```

- [ ] **Step 4: 验证编译与单元测试**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL，所有单元测试通过

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fly/pkg/PermissionChecks.kt app/src/main/java/com/fly/pkg/ui/MainScreen.kt app/src/main/java/com/fly/pkg/MainActivity.kt
git commit -m "Add main screen with master toggle and permission guidance"
```

---

### Task 7: 构建验证与真机手动验收

**Files:**
- 无新文件（本任务为验收）

**Interfaces:**
- Consumes: 全部前序任务

- [ ] **Step 1: 全量构建与测试**

Run: `./gradlew clean assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL，全部测试通过

- [ ] **Step 2: 安装到真机/模拟器**

Run: `./gradlew installDebug`
Expected: 安装成功

- [ ] **Step 3: 手动验收清单（对照 spec 第 8 节）**

逐项在真机上验证：

1. 打开应用，打开"悬浮窗总开关"
2. 缺权限时对应卡片高亮，点击"去开启"跳转到对应系统设置页
3. 授予悬浮窗权限、在无障碍设置中开启 PkgFloat 服务后，回到应用，两项状态显示"已开启"，底部显示"检测服务运行中"
4. 悬浮窗卡片出现；切换到其他应用（如微信、设置），卡片实时更新为对应应用名 + 包名 + Activity 完整类名
5. 回到桌面，卡片显示 Launcher 的包名和类名
6. 按住卡片拖动，位置跟随；松手位置被记住（杀掉应用重开后位置保持）
7. 拖到屏幕左/右边缘松手，卡片吸附收起为半透明小圆点；点击小圆点恢复卡片
8. 单击卡片，Toast 提示"已复制"，粘贴验证为完整类名
9. 关闭总开关，悬浮窗消失；重新打开恢复
10. 在系统无障碍设置中强行关闭服务，回到应用，底部状态变红提示未运行

- [ ] **Step 4: 推送最终代码**

```bash
git push
```
