package com.example.ime

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class KeyboardKeyInfo(
    val label: String,
    val secondaryLabel: String = "",
    val code: Int,
    val weight: Float = 1f,
    val isModifier: Boolean = false,
    val isAccent: Boolean = false
)

@Composable
fun QwertyKeyboard(
    modifier: Modifier = Modifier,
    onKeyPress: (KeyboardKeyInfo) -> Unit
) {
    val fRow = listOf(
        KeyboardKeyInfo("Esc", code = KeyEvent.KEYCODE_ESCAPE, isModifier = true),
        KeyboardKeyInfo("F1", code = KeyEvent.KEYCODE_F1),
        KeyboardKeyInfo("F2", code = KeyEvent.KEYCODE_F2),
        KeyboardKeyInfo("F3", code = KeyEvent.KEYCODE_F3),
        KeyboardKeyInfo("F4", code = KeyEvent.KEYCODE_F4),
        KeyboardKeyInfo("F5", code = KeyEvent.KEYCODE_F5),
        KeyboardKeyInfo("F6", code = KeyEvent.KEYCODE_F6),
        KeyboardKeyInfo("F7", code = KeyEvent.KEYCODE_F7),
        KeyboardKeyInfo("F8", code = KeyEvent.KEYCODE_F8),
        KeyboardKeyInfo("F9", code = KeyEvent.KEYCODE_F9),
        KeyboardKeyInfo("F10", code = KeyEvent.KEYCODE_F10),
        KeyboardKeyInfo("F11", code = KeyEvent.KEYCODE_F11),
        KeyboardKeyInfo("F12", code = KeyEvent.KEYCODE_F12),
        KeyboardKeyInfo("Del", code = KeyEvent.KEYCODE_FORWARD_DEL, isAccent = true)
    )

    val numRow = listOf(
        KeyboardKeyInfo("`", "~", KeyEvent.KEYCODE_GRAVE),
        KeyboardKeyInfo("1", "!", KeyEvent.KEYCODE_1),
        KeyboardKeyInfo("2", "@", KeyEvent.KEYCODE_2),
        KeyboardKeyInfo("3", "#", KeyEvent.KEYCODE_3),
        KeyboardKeyInfo("4", "$", KeyEvent.KEYCODE_4),
        KeyboardKeyInfo("5", "%", KeyEvent.KEYCODE_5),
        KeyboardKeyInfo("6", "^", KeyEvent.KEYCODE_6),
        KeyboardKeyInfo("7", "&", KeyEvent.KEYCODE_7),
        KeyboardKeyInfo("8", "*", KeyEvent.KEYCODE_8),
        KeyboardKeyInfo("9", "(", KeyEvent.KEYCODE_9),
        KeyboardKeyInfo("0", ")", KeyEvent.KEYCODE_0),
        KeyboardKeyInfo("-", "_", KeyEvent.KEYCODE_MINUS),
        KeyboardKeyInfo("=", "+", KeyEvent.KEYCODE_EQUALS),
        KeyboardKeyInfo("⌫", "", KeyEvent.KEYCODE_DEL, weight = 2f, isAccent = true)
    )

    val qwertyRow = listOf(
        KeyboardKeyInfo("Tab", "", KeyEvent.KEYCODE_TAB, weight = 1.5f, isModifier = true),
        KeyboardKeyInfo("Q", "", KeyEvent.KEYCODE_Q),
        KeyboardKeyInfo("W", "", KeyEvent.KEYCODE_W),
        KeyboardKeyInfo("E", "", KeyEvent.KEYCODE_E),
        KeyboardKeyInfo("R", "", KeyEvent.KEYCODE_R),
        KeyboardKeyInfo("T", "", KeyEvent.KEYCODE_T),
        KeyboardKeyInfo("Y", "", KeyEvent.KEYCODE_Y),
        KeyboardKeyInfo("U", "", KeyEvent.KEYCODE_U),
        KeyboardKeyInfo("I", "", KeyEvent.KEYCODE_I),
        KeyboardKeyInfo("O", "", KeyEvent.KEYCODE_O),
        KeyboardKeyInfo("P", "", KeyEvent.KEYCODE_P),
        KeyboardKeyInfo("[", "{", KeyEvent.KEYCODE_LEFT_BRACKET),
        KeyboardKeyInfo("]", "}", KeyEvent.KEYCODE_RIGHT_BRACKET),
        KeyboardKeyInfo("\\", "|", KeyEvent.KEYCODE_BACKSLASH, weight = 1.5f)
    )

    val homeRow = listOf(
        KeyboardKeyInfo("Caps", "", KeyEvent.KEYCODE_CAPS_LOCK, weight = 1.8f, isModifier = true),
        KeyboardKeyInfo("A", "", KeyEvent.KEYCODE_A),
        KeyboardKeyInfo("S", "", KeyEvent.KEYCODE_S),
        KeyboardKeyInfo("D", "", KeyEvent.KEYCODE_D),
        KeyboardKeyInfo("F", "", KeyEvent.KEYCODE_F),
        KeyboardKeyInfo("G", "", KeyEvent.KEYCODE_G),
        KeyboardKeyInfo("H", "", KeyEvent.KEYCODE_H),
        KeyboardKeyInfo("J", "", KeyEvent.KEYCODE_J),
        KeyboardKeyInfo("K", "", KeyEvent.KEYCODE_K),
        KeyboardKeyInfo("L", "", KeyEvent.KEYCODE_L),
        KeyboardKeyInfo(";", ":", KeyEvent.KEYCODE_SEMICOLON),
        KeyboardKeyInfo("'", "\"", KeyEvent.KEYCODE_APOSTROPHE),
        KeyboardKeyInfo("Enter", "", KeyEvent.KEYCODE_ENTER, weight = 2.2f, isAccent = true)
    )

    val shiftRow = listOf(
        KeyboardKeyInfo("Shift", "", KeyEvent.KEYCODE_SHIFT_LEFT, weight = 2.3f, isModifier = true),
        KeyboardKeyInfo("Z", "", KeyEvent.KEYCODE_Z),
        KeyboardKeyInfo("X", "", KeyEvent.KEYCODE_X),
        KeyboardKeyInfo("C", "", KeyEvent.KEYCODE_C),
        KeyboardKeyInfo("V", "", KeyEvent.KEYCODE_V),
        KeyboardKeyInfo("B", "", KeyEvent.KEYCODE_B),
        KeyboardKeyInfo("N", "", KeyEvent.KEYCODE_N),
        KeyboardKeyInfo("M", "", KeyEvent.KEYCODE_M),
        KeyboardKeyInfo(",", "<", KeyEvent.KEYCODE_COMMA),
        KeyboardKeyInfo(".", ">", KeyEvent.KEYCODE_PERIOD),
        KeyboardKeyInfo("/", "?", KeyEvent.KEYCODE_SLASH),
        KeyboardKeyInfo("Shift", "", KeyEvent.KEYCODE_SHIFT_RIGHT, weight = 2.7f, isModifier = true)
    )

    val bottomRow = listOf(
        KeyboardKeyInfo("Ctrl", "", KeyEvent.KEYCODE_CTRL_LEFT, weight = 1.5f, isModifier = true),
        KeyboardKeyInfo("Win", "", KeyEvent.KEYCODE_META_LEFT, weight = 1.2f, isModifier = true),
        KeyboardKeyInfo("Alt", "", KeyEvent.KEYCODE_ALT_LEFT, weight = 1.2f, isModifier = true),
        KeyboardKeyInfo("SPACE", "", KeyEvent.KEYCODE_SPACE, weight = 6.1f),
        KeyboardKeyInfo("AltGr", "", KeyEvent.KEYCODE_ALT_RIGHT, weight = 1.2f, isModifier = true),
        KeyboardKeyInfo("Fn", "", KeyEvent.KEYCODE_UNKNOWN, weight = 1.2f, isModifier = true),
        KeyboardKeyInfo("Ctrl", "", KeyEvent.KEYCODE_CTRL_RIGHT, weight = 1.2f, isModifier = true)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        KeyboardRow(fRow, onKeyPress, modifier = Modifier.weight(0.8f))
        KeyboardRow(numRow, onKeyPress, modifier = Modifier.weight(1f))
        KeyboardRow(qwertyRow, onKeyPress, modifier = Modifier.weight(1f))
        KeyboardRow(homeRow, onKeyPress, modifier = Modifier.weight(1f))
        KeyboardRow(shiftRow, onKeyPress, modifier = Modifier.weight(1f))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KeyboardRow(bottomRow, onKeyPress, modifier = Modifier.weight(0.7f))
            
            // Arrows
            Column(
                modifier = Modifier.weight(0.3f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Spacer(modifier = Modifier.weight(1f))
                    KeyboardKey(
                        key = KeyboardKeyInfo("▲", "", KeyEvent.KEYCODE_DPAD_UP),
                        onKeyPress = onKeyPress,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    KeyboardKey(
                        key = KeyboardKeyInfo("◀", "", KeyEvent.KEYCODE_DPAD_LEFT),
                        onKeyPress = onKeyPress,
                        modifier = Modifier.weight(1f)
                    )
                    KeyboardKey(
                        key = KeyboardKeyInfo("▼", "", KeyEvent.KEYCODE_DPAD_DOWN),
                        onKeyPress = onKeyPress,
                        modifier = Modifier.weight(1f)
                    )
                    KeyboardKey(
                        key = KeyboardKeyInfo("▶", "", KeyEvent.KEYCODE_DPAD_RIGHT),
                        onKeyPress = onKeyPress,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun KeyboardRow(
    keys: List<KeyboardKeyInfo>,
    onKeyPress: (KeyboardKeyInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (key in keys) {
            KeyboardKey(
                key = key,
                onKeyPress = onKeyPress,
                modifier = Modifier.weight(key.weight)
            )
        }
    }
}

@Composable
fun KeyboardKey(
    key: KeyboardKeyInfo,
    onKeyPress: (KeyboardKeyInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val isActive = isPressed || ModifierState.activeKeys.contains(key.code)
    
    val isToggled = remember(key.code, ModifierState.shiftPressed, ModifierState.ctrlPressed, ModifierState.altPressed, ModifierState.metaPressed, ModifierState.capsLockEnabled) {
        when (key.code) {
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> ModifierState.shiftPressed
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> ModifierState.ctrlPressed
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> ModifierState.altPressed
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT -> ModifierState.metaPressed
            KeyEvent.KEYCODE_CAPS_LOCK -> ModifierState.capsLockEnabled
            else -> false
        }
    }
    
    val bgColor = when {
        isActive -> Color(0xFF5C3DDD)
        isToggled -> Color(0xFF5C3DDD) // Highlight active modifiers
        key.isModifier -> Color(0xFF262626)
        key.isAccent -> Color(0xFF333333)
        else -> Color(0xFF1A1A1A)
    }
    
    val textColor = when {
        isActive -> Color.White
        key.isModifier -> Color.White
        key.label == "⌫" -> Color(0xFFEF4444)
        else -> Color.LightGray
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(bgColor, RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onKeyPress(key)
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (key.secondaryLabel.isNotEmpty()) {
            Text(
                text = key.secondaryLabel,
                color = textColor.copy(alpha = 0.7f),
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 4.dp, top = 2.dp)
            )
        }
        Text(
            text = key.label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
