package com.fly.pagelens

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.fly.pagelens.service.PageDetectService

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
