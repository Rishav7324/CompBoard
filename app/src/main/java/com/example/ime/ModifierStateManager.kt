package com.example.ime

import android.view.KeyEvent

class ModifierStateManager {
    enum class State { INACTIVE, ONE_SHOT, LATCHED, LOCKED }

    var shiftState = State.INACTIVE
    var ctrlState = State.INACTIVE
    var altState = State.INACTIVE
    var metaState = State.INACTIVE
    var fnState = State.INACTIVE
    var capsLockState = State.INACTIVE

    fun getMetaStateInt(): Int {
        var m = 0
        if (ctrlState != State.INACTIVE) m = m or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (shiftState != State.INACTIVE) m = m or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        if (altState != State.INACTIVE) m = m or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (metaState != State.INACTIVE) m = m or KeyEvent.META_META_ON
        if (capsLockState == State.LOCKED) m = m or KeyEvent.META_CAPS_LOCK_ON
        return m
    }

    fun handleModifierPress(keyCode: Int) {
        when (keyCode) {
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                shiftState = nextState(shiftState)
            }
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> {
                ctrlState = nextState(ctrlState)
            }
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> {
                altState = nextState(altState)
            }
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT -> {
                metaState = nextState(metaState)
            }
            KeyEvent.KEYCODE_FUNCTION -> {
                fnState = nextState(fnState)
            }
            KeyEvent.KEYCODE_CAPS_LOCK -> {
                capsLockState = if (capsLockState == State.INACTIVE) State.LOCKED else State.INACTIVE
            }
        }
    }

    private fun nextState(current: State): State {
        return when (current) {
            State.INACTIVE -> State.ONE_SHOT
            State.ONE_SHOT -> State.LOCKED
            State.LATCHED -> State.LOCKED
            State.LOCKED -> State.INACTIVE
        }
    }

    fun onNormalKeyPress() {
        if (shiftState == State.ONE_SHOT) shiftState = State.INACTIVE
        if (ctrlState == State.ONE_SHOT) ctrlState = State.INACTIVE
        if (altState == State.ONE_SHOT) altState = State.INACTIVE
        if (metaState == State.ONE_SHOT) metaState = State.INACTIVE
        if (fnState == State.ONE_SHOT) fnState = State.INACTIVE
    }
}
