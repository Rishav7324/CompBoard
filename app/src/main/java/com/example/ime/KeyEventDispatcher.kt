package com.example.ime

import android.view.KeyEvent

class KeyEventDispatcher(private val ims: CompBoardInputMethodService) {
    
    val shortcutManager = ShortcutManager(this)

    fun sendKeyDown(code: Int) {
        if (!shortcutManager.handleKeyDown(code)) {
            // Not consumed by shortcut manager
            if (code == KeyEvent.KEYCODE_DEL) {
                val ic = ims.currentInputConnection
                if (ic != null) {
                    ic.deleteSurroundingText(1, 0)
                    return
                }
            }
            if (code > 0) {
                val metaState = ModifierState.getModifierMask()
                val eventDown = KeyEvent(0, 0, KeyEvent.ACTION_DOWN, code, 0, metaState)
                ims.dispatchKeyEventToTarget(eventDown)
            }
        }
    }
    
    fun sendKeyUp(code: Int) {
        shortcutManager.handleKeyUp(code)
        if (code > 0) {
            val metaState = ModifierState.getModifierMask()
            val eventUp = KeyEvent(0, 0, KeyEvent.ACTION_UP, code, 0, metaState)
            ims.dispatchKeyEventToTarget(eventUp)
        }
    }
    
    fun sendKey(code: Int, metaState: Int = 0) {
        sendKeyDown(code)
        sendKeyUp(code)
    }
    
    fun sendText(text: String) {
        ims.commitTextToTarget(text)
    }
}
