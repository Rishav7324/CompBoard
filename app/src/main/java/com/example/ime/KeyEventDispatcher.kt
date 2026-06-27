package com.example.ime

import android.view.KeyEvent

class KeyEventDispatcher(private val ims: CompBoardInputMethodService) {
    
    fun sendKey(code: Int, metaState: Int = 0) {
        if (code > 0) {
            val eventDown = KeyEvent(0, 0, KeyEvent.ACTION_DOWN, code, 0, metaState)
            val eventUp = KeyEvent(0, 0, KeyEvent.ACTION_UP, code, 0, metaState)
            ims.dispatchKeyEventToTarget(eventDown)
            ims.dispatchKeyEventToTarget(eventUp)
        }
    }
    
    fun sendText(text: String) {
        ims.commitTextToTarget(text)
    }
}
