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
