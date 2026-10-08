package com.example.oneread.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.view.Window
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized manager for persistent "Keep Screen On" application setting.
 *
 * Ensures:
 * - Setting is persisted across app restarts, activity recreation, navigation, and backgrounding.
 * - Single source of truth applied to current active Window.
 * - Automatically reapplied upon Activity resume/focus gain.
 */
object KeepScreenOnManager {
    private const val PREFS_NAME = "hr_read_settings_prefs"
    private const val KEY_KEEP_SCREEN_ON = "pref_keep_screen_on"

    private var prefs: SharedPreferences? = null
    private val _keepScreenOn = MutableStateFlow(false)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private var currentWindow: Window? = null

    fun init(context: Context) {
        if (prefs == null) {
            val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = p
            _keepScreenOn.value = p.getBoolean(KEY_KEEP_SCREEN_ON, false)
        }
        applyCurrentState()
    }

    fun setKeepScreenOn(enabled: Boolean) {
        _keepScreenOn.value = enabled
        prefs?.edit()?.putBoolean(KEY_KEEP_SCREEN_ON, enabled)?.apply()
        applyCurrentState()
    }

    fun attachWindow(window: Window) {
        currentWindow = window
        applyCurrentState()
    }

    fun detachWindow(window: Window) {
        if (currentWindow == window) {
            currentWindow = null
        }
    }

    fun reapply(activity: Activity?) {
        activity?.window?.let { window ->
            currentWindow = window
            applyCurrentState()
        }
    }

    private fun applyCurrentState() {
        val window = currentWindow ?: return
        val enabled = _keepScreenOn.value
        try {
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } catch (_: Exception) {}
    }
}
