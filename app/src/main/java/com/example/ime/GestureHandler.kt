package com.example.ime

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.KeyEvent

class GestureHandler(private val dispatcher: KeyEventDispatcher, private val imeService: CompBoardInputMethodService) : GestureDetector.SimpleOnGestureListener() {

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (e1 == null) return false
        val dx = e2.x - e1.x
        val dy = e2.y - e1.y

        // Check if swipe started on spacebar (approximate y range for bottom row)
        // For actual implementation, KeyboardView should pass the touched Key
        
        if (Math.abs(dx) > 40 && Math.abs(dy) < 40) {
            if (dx < 0) dispatcher.sendKey(KeyEvent.KEYCODE_DPAD_LEFT)
            else dispatcher.sendKey(KeyEvent.KEYCODE_DPAD_RIGHT)
            return true
        } else if (Math.abs(dy) > 40 && Math.abs(dx) < 40) {
            if (dy < 0) {
                imeService.toggleClipboardPanel()
            } else {
                imeService.requestHideSelf(0)
            }
            return true
        }
        return false
    }

    fun handleSwipeLeft() {
        dispatcher.sendKey(KeyEvent.KEYCODE_DPAD_LEFT)
    }

    fun handleSwipeRight() {
        dispatcher.sendKey(KeyEvent.KEYCODE_DPAD_RIGHT)
    }

    fun handleSwipeUp() {
        imeService.toggleClipboardPanel()
    }

    fun handleSwipeDown() {
        imeService.requestHideSelf(0)
    }
}
