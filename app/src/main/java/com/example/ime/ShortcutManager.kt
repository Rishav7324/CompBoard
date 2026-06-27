package com.example.ime

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.data.ShortcutEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ModifierState {
    var ctrlPressed by androidx.compose.runtime.mutableStateOf(false)
    var shiftPressed by androidx.compose.runtime.mutableStateOf(false)
    var altPressed by androidx.compose.runtime.mutableStateOf(false)
    var metaPressed by androidx.compose.runtime.mutableStateOf(false)
    var capsLockEnabled by androidx.compose.runtime.mutableStateOf(false)
    var numLockEnabled by androidx.compose.runtime.mutableStateOf(false)

    val activeKeys = androidx.compose.runtime.mutableStateListOf<Int>()
    val debugLogs = androidx.compose.runtime.mutableStateListOf<String>()
    val keyPressFrequencies = androidx.compose.runtime.mutableStateMapOf<String, Int>()
    var firstKeyPressTime by androidx.compose.runtime.mutableStateOf(0L)
    val keystrokeTimestamps = androidx.compose.runtime.mutableStateListOf<Long>()

    fun recordKeyPress(keyName: String) {
        val now = System.currentTimeMillis()
        if (firstKeyPressTime == 0L) {
            firstKeyPressTime = now
        }
        val currentCount = keyPressFrequencies[keyName] ?: 0
        keyPressFrequencies[keyName] = currentCount + 1
        
        keystrokeTimestamps.add(now)
        val thirtySecondsAgo = now - 30_000
        keystrokeTimestamps.removeAll { it < thirtySecondsAgo }
    }

    fun log(message: String) {
        recordKeyPress(message)
        debugLogs.add(message)
        if (debugLogs.size > 10) {
            debugLogs.removeAt(0)
        }
    }

    fun getModifierMask(): Int {

        var mask = 0
        if (ctrlPressed) mask = mask or KeyEvent.META_CTRL_ON
        if (shiftPressed) mask = mask or KeyEvent.META_SHIFT_ON
        if (altPressed) mask = mask or KeyEvent.META_ALT_ON
        if (metaPressed) mask = mask or KeyEvent.META_META_ON
        return mask
    }
}

class ShortcutManager(private val dispatcher: KeyEventDispatcher) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    // Active shortcuts loaded from DB
    private val activeShortcuts = mutableListOf<ShortcutEntity>()
    
    // Sequence tracking
    private var pendingSequenceKey: Int? = null
    private var sequenceTimeoutJob: Job? = null

    fun loadShortcuts(shortcuts: List<ShortcutEntity>) {
        activeShortcuts.clear()
        activeShortcuts.addAll(shortcuts)
    }

    /**
     * Handles key down events for modifiers and shortcut matching.
     * Returns true if the key event was consumed by a shortcut.
     */
    fun handleKeyDown(keyCode: Int): Boolean {
        updateModifierState(keyCode, true)

        if (keyCode == android.view.KeyEvent.KEYCODE_CAPS_LOCK) {
            ModifierState.capsLockEnabled = !ModifierState.capsLockEnabled
            return true
        }

        if (isModifier(keyCode)) return false // Don't trigger shortcuts just on modifier press

        val currentModifiers = ModifierState.getModifierMask()

        // 1. Check for Sequence Continuation
        if (pendingSequenceKey != null) {
            val sequenceMatch = activeShortcuts.find {
                it.secondaryKeyCode == keyCode && it.primaryKeyCode == pendingSequenceKey && (it.modifiers == currentModifiers || it.modifiers == 0) // simplify modifiers check
            }
            clearSequence()
            if (sequenceMatch != null) {
                executeShortcut(sequenceMatch)
                return true
            }
        }

        // 2. Check for matching shortcuts
        val matches = activeShortcuts.filter {
            it.primaryKeyCode == keyCode && it.modifiers == currentModifiers
        }

        if (matches.isNotEmpty()) {
            // Priority: Macro -> Sequence Start -> Single
            val sequenceStart = matches.find { it.secondaryKeyCode != null }
            if (sequenceStart != null) {
                startSequence(keyCode)
                return true
            }
            
            val exactMatch = matches.find { it.secondaryKeyCode == null }
            if (exactMatch != null) {
                executeShortcut(exactMatch)
                return true
            }
        }

        return false
    }

    fun handleKeyUp(keyCode: Int) {
        updateModifierState(keyCode, false)
    }

    private fun startSequence(keyCode: Int) {
        pendingSequenceKey = keyCode
        sequenceTimeoutJob?.cancel()
        sequenceTimeoutJob = scope.launch {
            delay(1500) // Sequence timeout
            clearSequence()
        }
    }

    private fun clearSequence() {
        pendingSequenceKey = null
        sequenceTimeoutJob?.cancel()
    }

    private fun executeShortcut(shortcut: ShortcutEntity) {
        when (shortcut.actionType) {
            "text" -> {
                dispatcher.sendText(shortcut.actionPayload)
            }
            "keyboard" -> {
                if (shortcut.actionPayload == "caps_lock") {
                    ModifierState.capsLockEnabled = !ModifierState.capsLockEnabled
                }
            }
            "macro" -> {
                // Execute steps
                scope.launch {
                    val steps = shortcut.actionPayload.split(",")
                    for (step in steps) {
                        if (step == "DELAY") {
                            delay(100)
                        } else {
                            dispatcher.sendText(step)
                        }
                    }
                }
            }
            // Add other actions like cursor movement, etc.
            "cursor" -> {
                when (shortcut.actionPayload) {
                    "left" -> dispatcher.sendKey(KeyEvent.KEYCODE_DPAD_LEFT)
                    "right" -> dispatcher.sendKey(KeyEvent.KEYCODE_DPAD_RIGHT)
                    "up" -> dispatcher.sendKey(KeyEvent.KEYCODE_DPAD_UP)
                    "down" -> dispatcher.sendKey(KeyEvent.KEYCODE_DPAD_DOWN)
                }
            }
        }
    }

    private fun updateModifierState(keyCode: Int, isDown: Boolean) {
        when (keyCode) {
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> ModifierState.ctrlPressed = isDown
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> ModifierState.shiftPressed = isDown
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> ModifierState.altPressed = isDown
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT -> ModifierState.metaPressed = isDown
        }
    }

    private fun isModifier(keyCode: Int): Boolean {
        return keyCode in listOf(
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT
        )
    }
}
