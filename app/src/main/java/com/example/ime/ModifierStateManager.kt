package com.example.ime

import android.view.KeyEvent

enum class ModifierKey {
    CTRL, ALT, SHIFT, META, FN, CAPS_LOCK
}

class ModifierStateManager {
    enum class State { INACTIVE, ONE_SHOT, LATCHED, LOCKED }

    private val states = mutableMapOf(
        ModifierKey.CTRL to State.INACTIVE,
        ModifierKey.ALT to State.INACTIVE,
        ModifierKey.SHIFT to State.INACTIVE,
        ModifierKey.META to State.INACTIVE,
        ModifierKey.FN to State.INACTIVE,
        ModifierKey.CAPS_LOCK to State.INACTIVE
    )

    fun getMetaStateInt(): Int {
        var m = 0
        if (isActive(ModifierKey.CTRL)) m = m or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (isActive(ModifierKey.SHIFT)) m = m or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        if (isActive(ModifierKey.ALT)) m = m or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (isActive(ModifierKey.META)) m = m or KeyEvent.META_META_ON
        if (states[ModifierKey.CAPS_LOCK] == State.LOCKED) m = m or KeyEvent.META_CAPS_LOCK_ON
        return m
    }

    fun isActive(modifier: ModifierKey): Boolean {
        return states[modifier] != State.INACTIVE
    }

    fun getState(modifier: ModifierKey): State {
        return states[modifier] ?: State.INACTIVE
    }

    fun handleModifierPress(keyCode: Int) {
        val modifier = when (keyCode) {
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> ModifierKey.SHIFT
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> ModifierKey.CTRL
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> ModifierKey.ALT
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT -> ModifierKey.META
            KeyEvent.KEYCODE_FUNCTION -> ModifierKey.FN
            KeyEvent.KEYCODE_CAPS_LOCK -> ModifierKey.CAPS_LOCK
            else -> null
        } ?: return

        if (modifier == ModifierKey.CAPS_LOCK) {
            states[modifier] = if (states[modifier] == State.INACTIVE) State.LOCKED else State.INACTIVE
            return
        }

        states[modifier] = nextState(states[modifier]!!)
    }

    private fun nextState(current: State): State {
        return when (current) {
            State.INACTIVE -> State.ONE_SHOT
            State.ONE_SHOT -> State.LATCHED
            State.LATCHED -> State.LOCKED
            State.LOCKED -> State.INACTIVE
        }
    }

    fun onNormalKeyPress() {
        for (modifier in ModifierKey.values()) {
            if (modifier == ModifierKey.CAPS_LOCK) continue
            val state = states[modifier]
            if (state == State.ONE_SHOT || state == State.LATCHED) {
                states[modifier] = State.INACTIVE
            }
        }
    }

    fun reset() {
        states.keys.forEach { states[it] = State.INACTIVE }
    }

    fun getActiveModifiers(): List<ModifierKey> {
        return states.filter { it.value != State.INACTIVE }.keys.toList()
    }
}
