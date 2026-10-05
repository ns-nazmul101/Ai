package com.example.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

class DevilAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
        isServiceRunning = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used, event tracking can be added if needed
    }

    override fun onInterrupt() {
        isServiceRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        instance = null
    }

    companion object {
        var isServiceRunning: Boolean = false
            private set
        private var instance: WeakReference<DevilAccessibilityService>? = null

        fun takeScreenshot(): Boolean {
            val service = instance?.get() ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                service.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            } else {
                false
            }
        }

        fun goHome(): Boolean {
            val service = instance?.get() ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_HOME)
        }

        fun goBack(): Boolean {
            val service = instance?.get() ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_BACK)
        }

        fun lockScreen(): Boolean {
            val service = instance?.get() ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                false
            }
        }

        fun openNotifications(): Boolean {
            val service = instance?.get() ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        }

        fun openQuickSettings(): Boolean {
            val service = instance?.get() ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        }
    }
}
