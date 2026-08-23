package com.katoaapps.openminilaunch.platform

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent

/**
 * Performs only the system lock-screen action requested by the user's
 * double-tap. This service does not subscribe to events or inspect windows.
 */
class LockScreenAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        connectedService = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (connectedService === this) connectedService = null
        super.onDestroy()
    }

    companion object {
        @Volatile private var connectedService: LockScreenAccessibilityService? = null

        fun lockScreen(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                connectedService?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) == true
    }
}
