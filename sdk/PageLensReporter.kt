package pagelens.reporter

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

/**
 * PageLens Fragment 上报 SDK（单文件版，拷入目标工程即可用）
 *
 * 用法：
 * 1. 把本文件拷入你的应用工程（工程需依赖 androidx.fragment:fragment）
 * 2. 在 Application.onCreate 中调用 PageLensReporter.install(this)
 * 3. 保持 PageLens 悬浮窗开启，Fragment 切换时卡片第三行会显示其完整类名
 *
 * 注意：上报通过普通广播实现，任何应用都可伪造，仅用于调试场景。
 */
object PageLensReporter {

    private const val ACTION = "com.fly.pagelens.REPORT_FRAGMENT"
    private const val TARGET_PACKAGE = "com.fly.pagelens"

    fun install(application: Application) {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    if (activity !is FragmentActivity) return
                    activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
                        object : FragmentManager.FragmentLifecycleCallbacks() {
                            override fun onFragmentResumed(fm: FragmentManager, fragment: Fragment) {
                                report(activity, fragment)
                            }
                        },
                        true,
                    )
                }

                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }

    private fun report(activity: Activity, fragment: Fragment) {
        val intent = Intent(ACTION).apply {
            setPackage(TARGET_PACKAGE)
            putExtra("package", activity.packageName)
            putExtra("activity", activity.javaClass.name)
            putExtra("fragment", fragment.javaClass.name)
        }
        activity.sendBroadcast(intent)
    }
}
