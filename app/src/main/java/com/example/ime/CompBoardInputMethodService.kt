package com.example.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.KeyEvent

class CompBoardInputMethodService : InputMethodService() {

    private lateinit var keyboardView: KeyboardView
    private lateinit var dispatcher: KeyEventDispatcher

    override fun onCreateInputView(): View {
        keyboardView = KeyboardView(this)
        dispatcher = KeyEventDispatcher(this)
        keyboardView.setDispatcher(dispatcher)
        return keyboardView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardView.updateForEditorInfo(info)
    }

    fun dispatchKeyEventToTarget(event: KeyEvent) {
        val ic: InputConnection? = currentInputConnection
        if (ic != null) {
            ic.sendKeyEvent(event)
        } else {
            // Fallback for system UI without InputConnection
            sendDownUpKeyEvents(event.keyCode)
        }
    }
    
    fun commitTextToTarget(text: CharSequence) {
        currentInputConnection?.commitText(text, 1)
    }
}
