package com.fly.pagelens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FragmentReportReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REPORT_FRAGMENT) return
        val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: return
        val activityName = intent.getStringExtra(EXTRA_ACTIVITY) ?: return
        val fragmentName = intent.getStringExtra(EXTRA_FRAGMENT) ?: return
        ForegroundPageRepository.updateFragment(packageName, activityName, fragmentName)
    }

    companion object {
        const val ACTION_REPORT_FRAGMENT = "com.fly.pagelens.REPORT_FRAGMENT"
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_ACTIVITY = "activity"
        const val EXTRA_FRAGMENT = "fragment"
    }
}
