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
    
    fun performGlobalShortcut(action: Int) {
        performGlobalAction(action)
    }
}
