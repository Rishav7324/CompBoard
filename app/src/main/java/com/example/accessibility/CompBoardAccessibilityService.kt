package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class CompBoardAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track app focus changes to auto-switch layouts
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            Log.d("CompBoardAccessibility", "Focused package: $packageName")
        }
    }

    override fun onInterrupt() {
        // Needed for accessibility service contract
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        // Setup configuration if needed dynamically
    }
    
    fun performGlobalShortcut(actionCode: String) {
        when (actionCode) {
            "RECENTS" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            "HOME" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "BACK" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "NOTIFICATIONS" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            "QUICK_SETTINGS" -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            "LOCK_SCREEN" -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
            "TAKE_SCREENSHOT" -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            }
            "TOGGLE_SPLIT_SCREEN" -> performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
        }
    }
}
