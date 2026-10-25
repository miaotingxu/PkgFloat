package com.fly.pkg.floatwin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.fly.pkg.ForegroundPage
import com.fly.pkg.SettingsStore
import com.fly.pkg.WindowPositionUtils
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

    companion object {
        private const val TAG = "FloatingWindowController"
    }

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

    private fun overlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun newWindowParams(x: Int, y: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
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

    private fun addOverlayView(view: ComposeView, x: Int, y: Int) {
        try {
            windowManager.addView(view, newWindowParams(x, y))
        } catch (e: WindowManager.BadTokenException) {
            Log.w(TAG, "Failed to add overlay window", e)
            Toast.makeText(context, "悬浮窗创建失败，请检查悬浮窗权限", Toast.LENGTH_SHORT).show()
        }
    }

    private fun screenMetrics() = context.resources.displayMetrics

    private fun maxCardWidthPx(): Int = (screenMetrics().widthPixels * 0.8f).toInt()

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
                onCollapse = { collapseToNearestEdge() },
                maxWidth = (maxCardWidthPx() / screenMetrics().density).dp,
            )
        }
        cardView = view
        val m = screenMetrics()
        val minVisible = (48 * m.density).toInt()
        val (cx, cy) = WindowPositionUtils.clampPosition(
            settings.windowX, settings.windowY, m.widthPixels, m.heightPixels, minVisible, minVisible,
        )
        addOverlayView(view, cx, cy)
    }

    private fun showDot(edgeX: Int, y: Int) {
        removeView(cardView)
        cardView = null
        if (dotView != null) return
        val view = newComposeView {
            FloatDot(onClick = { showCard() })
        }
        dotView = view
        val m = screenMetrics()
        val dotSize = (36 * m.density).toInt()
        val (cx, cy) = WindowPositionUtils.clampPosition(edgeX, y, m.widthPixels, m.heightPixels, dotSize, dotSize)
        addOverlayView(view, cx, cy)
    }

    private fun moveCard(dx: Int, dy: Int) {
        val view = cardView ?: return
        val lp = view.layoutParams as WindowManager.LayoutParams
        lp.x += dx
        lp.y += dy
        windowManager.updateViewLayout(view, lp)
    }

    private fun collapseToNearestEdge() {
        val view = cardView ?: return
        val lp = view.layoutParams as WindowManager.LayoutParams
        settings.windowX = lp.x
        settings.windowY = lp.y
        val m = screenMetrics()
        val dotSize = (36 * m.density).toInt()
        val centerX = lp.x + view.width / 2
        val edgeX = if (centerX < m.widthPixels / 2) 0 else m.widthPixels - dotSize
        showDot(edgeX = edgeX, y = lp.y)
    }

    private fun onCardDragEnd() {
        val view = cardView ?: return
        val lp = view.layoutParams as WindowManager.LayoutParams
        val metrics = context.resources.displayMetrics
        val (cx, cy) = WindowPositionUtils.clampPosition(
            lp.x, lp.y, metrics.widthPixels, metrics.heightPixels, view.width, view.height,
        )
        if (cx != lp.x || cy != lp.y) {
            lp.x = cx
            lp.y = cy
            windowManager.updateViewLayout(view, lp)
        }
        settings.windowX = lp.x
        settings.windowY = lp.y
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
