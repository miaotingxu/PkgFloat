# PkgFloat 悬浮窗设计文档

日期：2026-10-25
状态：已与用户逐节确认

## 1. 背景与目标

PkgFloat 是一个面向开发者/测试人员的 Android 调试工具：以**半透明悬浮窗**实时显示当前系统前台是**哪个应用的哪个页面（Activity 完整类名）**。

- 定位：开发/测试调试工具，精确到 Activity 类名（类似"当前 Activity"类应用）
- 平台：Android，minSdk 23，targetSdk 36，Kotlin + Jetpack Compose
- 包名：`com.fly.pkg`

## 2. 已确认的需求决策

| 问题 | 决策 |
|---|---|
| 用途定位 | 开发/测试调试工具，精确显示 Activity 完整类名 |
| 悬浮窗交互 | 自由拖动、单击复制类名、拖到边缘自动贴边收起成小圆点（点圆点恢复） |
| 回到桌面/息屏 | 照常显示 Launcher 的包名和类名，不做特殊处理 |
| 悬浮窗样式 | B · 详情卡片：图标 + 应用名 + 等宽字体两行（完整包名 + Activity 类名），半透明深色圆角卡片 |
| 页面检测方案 | 无障碍服务 AccessibilityService（唯一能做到 Activity 级的方案） |

## 3. 架构

| 组件 | 职责 |
|---|---|
| `MainActivity` | 唯一的常规界面（Compose 单页）：总开关、权限状态与引导跳转、服务运行状态指示 |
| `PageDetectService` | `AccessibilityService`，监听窗口切换事件，解析前台包名 + 类名。被系统绑定常驻，天然保活，**不需要前台服务和通知** |
| `FloatingWindowController` | 封装 `WindowManager`：创建/更新/移除悬浮窗；处理拖动、贴边收起、点击复制 |
| `FloatCardView` | 悬浮窗 UI（ComposeView 挂进 WindowManager），详情卡片样式 |
| `ForegroundPageRepository` | 单例 + `StateFlow<ForegroundPage>`，持有最新前台页面信息，服务与界面间共享状态 |

数据模型：`ForegroundPage(packageName: String, className: String, appName: String)`。

### 数据流

```
系统窗口切换
  → PageDetectService.onAccessibilityEvent (TYPE_WINDOW_STATE_CHANGED)
  → 解析 packageName + className，拼接完整类名，过滤本应用自身
  → ForegroundPageRepository.pageFlow (StateFlow)
  → FloatingWindowController 收集流，更新卡片内容
```

### 类名拼接规则

事件返回的 `className` 若以 `packageName` 开头则直接使用；否则按 `packageName + className` 拼接（处理 `.ui.LauncherUI` 这类缩写形式）。拼接与过滤逻辑提取为纯 Kotlin 函数，便于单元测试。

## 4. 主界面与权限引导

`MainActivity`（Compose 单页）包含：

- 顶部：总开关，控制悬浮窗显示/隐藏
- 两个权限卡片，各显示"已开启/未开启"状态 + 跳转按钮：
  - 悬浮窗权限 → `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`
  - 无障碍服务 → `Settings.ACTION_ACCESSIBILITY_SETTINGS`
- 无障碍服务运行状态指示（服务被系统回收时变红提示）
- 打开总开关时检查权限，缺失项高亮其引导按钮

## 5. 悬浮窗细节

- `WindowManager.LayoutParams`：窗口类型 API 26+ 用 `TYPE_APPLICATION_OVERLAY`，API 23-25 用 `TYPE_PHONE`（`FLAG_NOT_FOCUSABLE`、`PixelFormat.TRANSLUCENT`）；`addView` 需捕获 `BadTokenException` 防止服务进程崩溃
- 卡片内容：应用图标 + 应用名（粗体），下方等宽字体两行显示完整包名 + Activity 完整类名
- 半透明深色圆角卡片（样式 B，见 `.superpowers/brainstorm/` 中的 mockup）
- 交互：
  - 单击卡片：`ClipboardManager` 复制完整类名，Toast 提示"已复制"
  - 按住拖动：实时更新位置
  - 松手时贴近左右边缘：动画吸附收起为半透明小圆点；点击小圆点展开恢复原位
- 悬浮窗位置用 `SharedPreferences` 持久化，启动时恢复
- 应用图标通过 `PackageManager.getApplicationIcon(packageName)` 动态获取

## 6. 边界与异常处理

- 过滤本应用自身的窗口事件（不显示自己）
- 桌面照常显示 Launcher 信息；息屏不做特殊处理
- 个别应用上报的 `className` 可能是内部 View 类而非 Activity——照常显示，属正常现象
- 无障碍服务被系统回收：主界面状态指示变红，重新引导开启

## 7. 技术要点

- 悬浮窗 UI 用 Compose：`ComposeView` 挂进 `WindowManager` 时需手动挂载 `ViewTreeLifecycleOwner` / `ViewTreeViewModelStoreOwner` / `SavedStateRegistryOwner`（Service 没有这些 owner）
- 新增依赖（`gradle/libs.versions.toml`）：`androidx.lifecycle:lifecycle-viewmodel`、`androidx.savedstate:savedstate`
- 不需要网络权限、前台服务、通知
- `AndroidManifest.xml` 需声明：无障碍服务（含 `accessibilityservice` 元数据 XML 配置）、`SYSTEM_ALERT_WINDOW` 权限

## 8. 测试策略

- 单元测试：类名拼接规则、事件过滤逻辑（纯 Kotlin 函数）
- 悬浮窗 + 无障碍需真机验证，提供手动验收清单：
  - 总开关打开/关闭悬浮窗
  - 两项权限引导跳转与状态刷新
  - 切换应用/页面时卡片实时更新
  - 拖动、贴边收起、点圆点恢复、单击复制
  - 回到桌面显示 Launcher 信息
  - 杀掉无障碍服务后主界面状态变红

## 9. 明确不做（YAGNI）

- 点击悬浮窗跳转到对应应用（用户已排除）
- UsageStats 双保险降级方案（后续迭代再考虑）
- 历史记录、页面停留时长统计
- 悬浮窗样式 A/C 及其配置项
- 开机自启
