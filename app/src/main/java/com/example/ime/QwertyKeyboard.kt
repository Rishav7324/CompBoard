package com.example.ime

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.scale
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

data class KeyboardKeyInfo(
    val label: String,
    val secondaryLabel: String = "",
    val code: Int,
    val weight: Float = 1f,
    val isModifier: Boolean = false,
    val isAccent: Boolean = false
)

enum class KeyboardProfile {
    WINDOWS, LINUX, MACOS, TERMUX, VSCODE, GAMING
}

enum class KeyboardLayoutType {
    QWERTY, DVORAK, COLEMAK
}

@Composable
fun VirtualKeyboard(
    modifier: Modifier = Modifier,
    layoutType: KeyboardLayoutType = KeyboardLayoutType.QWERTY,
    profile: KeyboardProfile = KeyboardProfile.WINDOWS,
    keyStyle: String = "MECHANICAL",
    onKeyPress: (KeyboardKeyInfo) -> Unit,
    onSwipe: (KeyboardKeyInfo, String) -> Unit = { _, _ -> }
) {
    var isFnActive by remember { mutableStateOf(false) }
    var isNavModeActive by remember { mutableStateOf(false) }

    val utilityBar = listOf(
        KeyboardKeyInfo("ESC", code = KeyEvent.KEYCODE_ESCAPE, isModifier = true),
        KeyboardKeyInfo("TAB", code = KeyEvent.KEYCODE_TAB, isModifier = true),
        KeyboardKeyInfo("CTRL", code = KeyEvent.KEYCODE_CTRL_LEFT, isModifier = true),
        KeyboardKeyInfo("ALT", code = KeyEvent.KEYCODE_ALT_LEFT, isModifier = true),
        KeyboardKeyInfo("WIN", code = KeyEvent.KEYCODE_META_LEFT, isModifier = true),
        KeyboardKeyInfo("NAV", code = KeyEvent.KEYCODE_UNKNOWN, isModifier = true),
        KeyboardKeyInfo("HOME", code = KeyEvent.KEYCODE_MOVE_HOME, isModifier = true),
        KeyboardKeyInfo("END", code = KeyEvent.KEYCODE_MOVE_END, isModifier = true),
        KeyboardKeyInfo("PG↑", code = KeyEvent.KEYCODE_PAGE_UP, isModifier = true),
        KeyboardKeyInfo("PG↓", code = KeyEvent.KEYCODE_PAGE_DOWN, isModifier = true),
        KeyboardKeyInfo("DEL", code = KeyEvent.KEYCODE_FORWARD_DEL, isAccent = true)
    )

    val programmerToolbar = listOf(
        "{", "}", "[", "]", "(", ")", "<", ">", "|", "\\", "/", "=", "+", "-", "*", "&", "%", "$", "#", "@"
    ).map {
        val code = when (it) {
            "{" -> KeyEvent.KEYCODE_LEFT_BRACKET
            "}" -> KeyEvent.KEYCODE_RIGHT_BRACKET
            "[" -> KeyEvent.KEYCODE_LEFT_BRACKET
            "]" -> KeyEvent.KEYCODE_RIGHT_BRACKET
            "(" -> KeyEvent.KEYCODE_9
            ")" -> KeyEvent.KEYCODE_0
            "<" -> KeyEvent.KEYCODE_COMMA
            ">" -> KeyEvent.KEYCODE_PERIOD
            "|" -> KeyEvent.KEYCODE_BACKSLASH
            "\\" -> KeyEvent.KEYCODE_BACKSLASH
            "/" -> KeyEvent.KEYCODE_SLASH
            "=" -> KeyEvent.KEYCODE_EQUALS
            "+" -> KeyEvent.KEYCODE_EQUALS
            "-" -> KeyEvent.KEYCODE_MINUS
            "*" -> KeyEvent.KEYCODE_8
            "&" -> KeyEvent.KEYCODE_7
            "%" -> KeyEvent.KEYCODE_5
            "$" -> KeyEvent.KEYCODE_4
            "#" -> KeyEvent.KEYCODE_3
            "@" -> KeyEvent.KEYCODE_2
            else -> KeyEvent.KEYCODE_UNKNOWN
        }
        KeyboardKeyInfo(it, code = code)
    }

    // Explicit layouts for QWERTY, Dvorak, Colemak
    val row1 = when (layoutType) {
        KeyboardLayoutType.QWERTY -> listOf(
            KeyboardKeyInfo("Q", "", KeyEvent.KEYCODE_Q),
            KeyboardKeyInfo("W", "", KeyEvent.KEYCODE_W),
            KeyboardKeyInfo("E", "", KeyEvent.KEYCODE_E),
            KeyboardKeyInfo("R", "", KeyEvent.KEYCODE_R),
            KeyboardKeyInfo("T", "", KeyEvent.KEYCODE_T),
            KeyboardKeyInfo("Y", "", KeyEvent.KEYCODE_Y),
            KeyboardKeyInfo("U", "", KeyEvent.KEYCODE_U),
            KeyboardKeyInfo("I", "", KeyEvent.KEYCODE_I),
            KeyboardKeyInfo("O", "", KeyEvent.KEYCODE_O),
            KeyboardKeyInfo("P", "", KeyEvent.KEYCODE_P)
        )
        KeyboardLayoutType.DVORAK -> listOf(
            KeyboardKeyInfo("'", "\"", KeyEvent.KEYCODE_APOSTROPHE),
            KeyboardKeyInfo(",", "<", KeyEvent.KEYCODE_COMMA),
            KeyboardKeyInfo(".", ">", KeyEvent.KEYCODE_PERIOD),
            KeyboardKeyInfo("P", "", KeyEvent.KEYCODE_P),
            KeyboardKeyInfo("Y", "", KeyEvent.KEYCODE_Y),
            KeyboardKeyInfo("F", "", KeyEvent.KEYCODE_F),
            KeyboardKeyInfo("G", "", KeyEvent.KEYCODE_G),
            KeyboardKeyInfo("C", "", KeyEvent.KEYCODE_C),
            KeyboardKeyInfo("R", "", KeyEvent.KEYCODE_R),
            KeyboardKeyInfo("L", "", KeyEvent.KEYCODE_L)
        )
        KeyboardLayoutType.COLEMAK -> listOf(
            KeyboardKeyInfo("Q", "", KeyEvent.KEYCODE_Q),
            KeyboardKeyInfo("W", "", KeyEvent.KEYCODE_W),
            KeyboardKeyInfo("F", "", KeyEvent.KEYCODE_F),
            KeyboardKeyInfo("P", "", KeyEvent.KEYCODE_P),
            KeyboardKeyInfo("G", "", KeyEvent.KEYCODE_G),
            KeyboardKeyInfo("J", "", KeyEvent.KEYCODE_J),
            KeyboardKeyInfo("L", "", KeyEvent.KEYCODE_L),
            KeyboardKeyInfo("U", "", KeyEvent.KEYCODE_U),
            KeyboardKeyInfo("Y", "", KeyEvent.KEYCODE_Y),
            KeyboardKeyInfo(";", ":", KeyEvent.KEYCODE_SEMICOLON)
        )
    }

    val row2 = when (layoutType) {
        KeyboardLayoutType.QWERTY -> listOf(
            KeyboardKeyInfo("A", "", KeyEvent.KEYCODE_A),
            KeyboardKeyInfo("S", "", KeyEvent.KEYCODE_S),
            KeyboardKeyInfo("D", "", KeyEvent.KEYCODE_D),
            KeyboardKeyInfo("F", "", KeyEvent.KEYCODE_F),
            KeyboardKeyInfo("G", "", KeyEvent.KEYCODE_G),
            KeyboardKeyInfo("H", "", KeyEvent.KEYCODE_H),
            KeyboardKeyInfo("J", "", KeyEvent.KEYCODE_J),
            KeyboardKeyInfo("K", "", KeyEvent.KEYCODE_K),
            KeyboardKeyInfo("L", "", KeyEvent.KEYCODE_L),
            KeyboardKeyInfo(";", ":", KeyEvent.KEYCODE_SEMICOLON)
        )
        KeyboardLayoutType.DVORAK -> listOf(
            KeyboardKeyInfo("A", "", KeyEvent.KEYCODE_A),
            KeyboardKeyInfo("O", "", KeyEvent.KEYCODE_O),
            KeyboardKeyInfo("E", "", KeyEvent.KEYCODE_E),
            KeyboardKeyInfo("U", "", KeyEvent.KEYCODE_U),
            KeyboardKeyInfo("I", "", KeyEvent.KEYCODE_I),
            KeyboardKeyInfo("D", "", KeyEvent.KEYCODE_D),
            KeyboardKeyInfo("H", "", KeyEvent.KEYCODE_H),
            KeyboardKeyInfo("T", "", KeyEvent.KEYCODE_T),
            KeyboardKeyInfo("N", "", KeyEvent.KEYCODE_N),
            KeyboardKeyInfo("S", "", KeyEvent.KEYCODE_S)
        )
        KeyboardLayoutType.COLEMAK -> listOf(
            KeyboardKeyInfo("A", "", KeyEvent.KEYCODE_A),
            KeyboardKeyInfo("R", "", KeyEvent.KEYCODE_R),
            KeyboardKeyInfo("S", "", KeyEvent.KEYCODE_S),
            KeyboardKeyInfo("T", "", KeyEvent.KEYCODE_T),
            KeyboardKeyInfo("D", "", KeyEvent.KEYCODE_D),
            KeyboardKeyInfo("H", "", KeyEvent.KEYCODE_H),
            KeyboardKeyInfo("N", "", KeyEvent.KEYCODE_N),
            KeyboardKeyInfo("E", "", KeyEvent.KEYCODE_E),
            KeyboardKeyInfo("I", "", KeyEvent.KEYCODE_I),
            KeyboardKeyInfo("O", "", KeyEvent.KEYCODE_O)
        )
    }

    val row3Letters = when (layoutType) {
        KeyboardLayoutType.QWERTY -> listOf(
            KeyboardKeyInfo("Z", "", KeyEvent.KEYCODE_Z),
            KeyboardKeyInfo("X", "", KeyEvent.KEYCODE_X),
            KeyboardKeyInfo("C", "", KeyEvent.KEYCODE_C),
            KeyboardKeyInfo("V", "", KeyEvent.KEYCODE_V),
            KeyboardKeyInfo("B", "", KeyEvent.KEYCODE_B),
            KeyboardKeyInfo("N", "", KeyEvent.KEYCODE_N),
            KeyboardKeyInfo("M", "", KeyEvent.KEYCODE_M)
        )
        KeyboardLayoutType.DVORAK -> listOf(
            KeyboardKeyInfo(";", ":", KeyEvent.KEYCODE_SEMICOLON),
            KeyboardKeyInfo("Q", "", KeyEvent.KEYCODE_Q),
            KeyboardKeyInfo("J", "", KeyEvent.KEYCODE_J),
            KeyboardKeyInfo("K", "", KeyEvent.KEYCODE_K),
            KeyboardKeyInfo("X", "", KeyEvent.KEYCODE_X),
            KeyboardKeyInfo("B", "", KeyEvent.KEYCODE_B),
            KeyboardKeyInfo("M", "", KeyEvent.KEYCODE_M)
        )
        KeyboardLayoutType.COLEMAK -> listOf(
            KeyboardKeyInfo("Z", "", KeyEvent.KEYCODE_Z),
            KeyboardKeyInfo("X", "", KeyEvent.KEYCODE_X),
            KeyboardKeyInfo("C", "", KeyEvent.KEYCODE_C),
            KeyboardKeyInfo("V", "", KeyEvent.KEYCODE_V),
            KeyboardKeyInfo("B", "", KeyEvent.KEYCODE_B),
            KeyboardKeyInfo("K", "", KeyEvent.KEYCODE_K),
            KeyboardKeyInfo("M", "", KeyEvent.KEYCODE_M)
        )
    }

    val row3 = listOf(KeyboardKeyInfo("SHIFT", "", KeyEvent.KEYCODE_SHIFT_LEFT, weight = 1.5f, isModifier = true)) +
            row3Letters +
            listOf(KeyboardKeyInfo("⌫", "", KeyEvent.KEYCODE_DEL, weight = 1.5f, isAccent = true))
    
    val fnRow1 = listOf(
        KeyboardKeyInfo("F1", code = KeyEvent.KEYCODE_F1),
        KeyboardKeyInfo("F2", code = KeyEvent.KEYCODE_F2),
        KeyboardKeyInfo("F3", code = KeyEvent.KEYCODE_F3),
        KeyboardKeyInfo("F4", code = KeyEvent.KEYCODE_F4),
        KeyboardKeyInfo("F5", code = KeyEvent.KEYCODE_F5),
        KeyboardKeyInfo("F6", code = KeyEvent.KEYCODE_F6),
        KeyboardKeyInfo("F7", code = KeyEvent.KEYCODE_F7),
        KeyboardKeyInfo("F8", code = KeyEvent.KEYCODE_F8),
        KeyboardKeyInfo("F9", code = KeyEvent.KEYCODE_F9),
        KeyboardKeyInfo("F10", code = KeyEvent.KEYCODE_F10)
    )
    
    val fnRow2 = listOf(
        KeyboardKeyInfo("F11", code = KeyEvent.KEYCODE_F11),
        KeyboardKeyInfo("F12", code = KeyEvent.KEYCODE_F12),
        KeyboardKeyInfo("INS", code = KeyEvent.KEYCODE_INSERT),
        KeyboardKeyInfo("PRTSC", code = KeyEvent.KEYCODE_SYSRQ),
        KeyboardKeyInfo("PAUSE", code = KeyEvent.KEYCODE_BREAK),
        KeyboardKeyInfo("MENU", code = KeyEvent.KEYCODE_MENU)
    )

    val navRow1 = listOf(
        KeyboardKeyInfo("HOME", code = KeyEvent.KEYCODE_MOVE_HOME),
        KeyboardKeyInfo("↑", code = KeyEvent.KEYCODE_DPAD_UP),
        KeyboardKeyInfo("END", code = KeyEvent.KEYCODE_MOVE_END)
    )

    val navRow2 = listOf(
        KeyboardKeyInfo("←", code = KeyEvent.KEYCODE_DPAD_LEFT),
        KeyboardKeyInfo("↓", code = KeyEvent.KEYCODE_DPAD_DOWN),
        KeyboardKeyInfo("→", code = KeyEvent.KEYCODE_DPAD_RIGHT)
    )

    val navRow3 = listOf(
        KeyboardKeyInfo("PGUP", code = KeyEvent.KEYCODE_PAGE_UP),
        KeyboardKeyInfo("PGDN", code = KeyEvent.KEYCODE_PAGE_DOWN),
        KeyboardKeyInfo("DEL", code = KeyEvent.KEYCODE_FORWARD_DEL, isAccent = true)
    )

    val bottomRow = listOf(
        KeyboardKeyInfo("FN", "", KeyEvent.KEYCODE_FUNCTION, weight = 1.2f, isModifier = true),
        KeyboardKeyInfo("CTRL", "", KeyEvent.KEYCODE_CTRL_LEFT, weight = 1.2f, isModifier = true),
        KeyboardKeyInfo("ALT", "", KeyEvent.KEYCODE_ALT_LEFT, weight = 1.2f, isModifier = true),
        KeyboardKeyInfo("SPACE", "", KeyEvent.KEYCODE_SPACE, weight = 5f),
        KeyboardKeyInfo("←", "", KeyEvent.KEYCODE_DPAD_LEFT, weight = 1f),
        KeyboardKeyInfo("↓", "", KeyEvent.KEYCODE_DPAD_DOWN, weight = 1f),
        KeyboardKeyInfo("↑", "", KeyEvent.KEYCODE_DPAD_UP, weight = 1f),
        KeyboardKeyInfo("→", "", KeyEvent.KEYCODE_DPAD_RIGHT, weight = 1f)
    )

    // Layout/Label Mapper based on active profile
    val labelMapper: (String) -> String = { label ->
        when (profile) {
            KeyboardProfile.MACOS -> {
                when (label) {
                    "WIN" -> "CMD"
                    "ALT" -> "OPT"
                    else -> label
                }
            }
            KeyboardProfile.LINUX -> {
                when (label) {
                    "WIN" -> "SUPER"
                    else -> label
                }
            }
            else -> label
        }
    }

    val mappedUtilityBar = utilityBar.map { it.copy(label = labelMapper(it.label)) }
    val mappedBottomRow = bottomRow.map { it.copy(label = labelMapper(it.label)) }

    val mainBgColor = when (profile) {
        KeyboardProfile.TERMUX -> Color(0xFF000000)
        KeyboardProfile.VSCODE -> Color(0xFF0F111A)
        KeyboardProfile.MACOS -> Color(0xFF1E1E22)
        KeyboardProfile.GAMING -> Color(0xFF06060A)
        else -> Color(0xFF0F1117)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(mainBgColor)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // VS Code Quick Shortcuts row
        if (profile == KeyboardProfile.VSCODE) {
            Row(
                modifier = Modifier.weight(0.5f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val vscodeChips = listOf("Ctrl+P", "Ctrl+Shift+P", "Ctrl+/", "Ctrl+F", "Ctrl+`", "Undo", "Redo")
                vscodeChips.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(0xFF007ACC).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF007ACC).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .pointerInput(chip) {
                                detectTapGestures(
                                    onPress = {
                                        onKeyPress(KeyboardKeyInfo(chip, code = KeyEvent.KEYCODE_UNKNOWN, isModifier = true))
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chip,
                            color = Color(0xFF4F8CFF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Utility Bar
        KeyboardRow(
            keys = mappedUtilityBar, 
            onKeyPress = { key ->
                if (key.label == "NAV") {
                    isNavModeActive = !isNavModeActive
                } else {
                    onKeyPress(key)
                }
            }, 
            onSwipe = onSwipe, 
            modifier = Modifier.weight(0.7f),
            navActiveState = isNavModeActive,
            profile = profile,
            keyStyle = keyStyle
        )
        
        // Programmer Toolbar
        Row(modifier = Modifier.weight(0.6f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (key in programmerToolbar) {
                KeyboardKey(key, onKeyPress, onSwipe, Modifier.weight(1f), fontSize = 12.sp, isProgrammerKey = true, profile = profile, keyStyle = keyStyle)
            }
        }
        
        if (isNavModeActive) {
            KeyboardRow(navRow1, onKeyPress, onSwipe, modifier = Modifier.weight(1.2f), profile = profile, keyStyle = keyStyle)
            KeyboardRow(navRow2, onKeyPress, onSwipe, modifier = Modifier.weight(1.2f), profile = profile, keyStyle = keyStyle)
            KeyboardRow(navRow3, onKeyPress, onSwipe, modifier = Modifier.weight(1.2f), profile = profile, keyStyle = keyStyle)
        } else if (isFnActive) {
            KeyboardRow(fnRow1, onKeyPress, onSwipe, modifier = Modifier.weight(1.2f), profile = profile, keyStyle = keyStyle)
            KeyboardRow(fnRow2, onKeyPress, onSwipe, modifier = Modifier.weight(1.2f), profile = profile, keyStyle = keyStyle)
            Spacer(modifier = Modifier.weight(1.2f))
        } else {
            KeyboardRow(row1, onKeyPress, onSwipe, modifier = Modifier.weight(1.2f), profile = profile, keyStyle = keyStyle)
            KeyboardRow(row2, onKeyPress, onSwipe, modifier = Modifier.weight(1.2f), profile = profile, keyStyle = keyStyle)
            KeyboardRow(row3, onKeyPress, onSwipe, modifier = Modifier.weight(1.2f), profile = profile, keyStyle = keyStyle)
        }
        
        // Custom bottom row for FN toggle
        Row(
            modifier = Modifier.weight(1.2f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (key in mappedBottomRow) {
                if (key.label == "FN") {
                    val isToggled = isFnActive
                    val cornerRadius = when (profile) {
                        KeyboardProfile.MACOS -> 10.dp
                        KeyboardProfile.TERMUX -> 2.dp
                        else -> 8.dp
                    }
                    val bgColor = if (isToggled) {
                        when (profile) {
                            KeyboardProfile.TERMUX -> Color(0xFF00FF66)
                            KeyboardProfile.VSCODE -> Color(0xFF007ACC)
                            KeyboardProfile.MACOS -> Color(0xFF0A84FF)
                            KeyboardProfile.GAMING -> Color(0xFFFF007F)
                            else -> Color(0xFF4F8CFF)
                        }
                    } else {
                        when (profile) {
                            KeyboardProfile.TERMUX -> Color(0xFF0D0D0D)
                            KeyboardProfile.VSCODE -> Color(0xFF161B22)
                            KeyboardProfile.MACOS -> Color(0xFF32363E)
                            KeyboardProfile.GAMING -> Color(0xFF121218)
                            else -> Color(0xFF1A1D26)
                        }
                    }
                    val textColor = if (isToggled) Color.White else {
                        if (profile == KeyboardProfile.TERMUX) Color(0xFF00FF66) else Color.LightGray
                    }
                    val borderColor = if (isToggled) bgColor else {
                        when (profile) {
                            KeyboardProfile.TERMUX -> Color(0xFF333333)
                            KeyboardProfile.VSCODE -> Color(0xFF30363D)
                            KeyboardProfile.MACOS -> Color(0xFF4F515A)
                            else -> Color(0xFF2A2E39)
                        }
                    }
                    var isPressed by remember { mutableStateOf(false) }
                    
                    if (keyStyle == "MECHANICAL") {
                        val skirtColor = lerp(bgColor, Color.Black, 0.45f)
                        val highlightedBorderColor = lerp(borderColor, Color.White, 0.1f)
                        val animatedSkirtThickness by animateDpAsState(
                            targetValue = if (isPressed) 1.dp else 4.dp,
                            label = "skirtThickness"
                        )
                        val animatedTopOffset by animateDpAsState(
                            targetValue = if (isPressed) 3.dp else 0.dp,
                            label = "topOffset"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(key.weight)
                                .padding(bottom = 1.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isPressed = true
                                            isFnActive = !isFnActive
                                            tryAwaitRelease()
                                            isPressed = false
                                        }
                                    )
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 2.dp)
                                    .background(skirtColor, RoundedCornerShape(cornerRadius))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .offset(y = animatedTopOffset)
                                    .padding(bottom = animatedSkirtThickness)
                                    .background(bgColor, RoundedCornerShape(cornerRadius))
                                    .border(1.dp, highlightedBorderColor, RoundedCornerShape(cornerRadius)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("FN", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        val scale by animateFloatAsState(targetValue = if (isPressed) 0.90f else 1f, label = "")
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(key.weight)
                                .scale(scale)
                                .background(bgColor, RoundedCornerShape(cornerRadius))
                                .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isPressed = true
                                            isFnActive = !isFnActive
                                            tryAwaitRelease()
                                            isPressed = false
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("FN", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    KeyboardKey(
                        key = key,
                        onKeyPress = onKeyPress,
                        onSwipe = onSwipe,
                        modifier = Modifier.weight(key.weight),
                        profile = profile,
                        keyStyle = keyStyle
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
    onSwipe: (KeyboardKeyInfo, String) -> Unit,
    modifier: Modifier = Modifier,
    navActiveState: Boolean = false,
    profile: KeyboardProfile = KeyboardProfile.WINDOWS,
    keyStyle: String = "MECHANICAL"
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (key in keys) {
            KeyboardKey(
                key = key,
                onKeyPress = onKeyPress,
                onSwipe = onSwipe,
                modifier = Modifier.weight(key.weight),
                navActiveState = navActiveState,
                profile = profile,
                keyStyle = keyStyle
            )
        }
    }
}

@Composable
fun KeyboardKey(
    key: KeyboardKeyInfo,
    onKeyPress: (KeyboardKeyInfo) -> Unit,
    onSwipe: (KeyboardKeyInfo, String) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    isProgrammerKey: Boolean = false,
    navActiveState: Boolean = false,
    profile: KeyboardProfile = KeyboardProfile.WINDOWS,
    keyStyle: String = "MECHANICAL"
) {
    var isPressed by remember { mutableStateOf(false) }
    var accumulatedDragX by remember { mutableStateOf(0f) }
    
    val isActive = isPressed || ModifierState.activeKeys.contains(key.code)
    
    val isToggled = remember(key.code, ModifierState.shiftPressed, ModifierState.ctrlPressed, ModifierState.altPressed, ModifierState.metaPressed, ModifierState.capsLockEnabled, ModifierState.ctrlLocked, ModifierState.altLocked, ModifierState.metaLocked, navActiveState) {
        when (key.code) {
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> ModifierState.shiftPressed || ModifierState.capsLockEnabled
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> ModifierState.ctrlPressed || ModifierState.ctrlLocked
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> ModifierState.altPressed || ModifierState.altLocked
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT -> ModifierState.metaPressed || ModifierState.metaLocked
            KeyEvent.KEYCODE_CAPS_LOCK -> ModifierState.capsLockEnabled
            else -> if (key.label == "NAV") navActiveState else false
        }
    }
    
    val isGamingKey = profile == KeyboardProfile.GAMING && (key.label == "W" || key.label == "A" || key.label == "S" || key.label == "D" || key.label == "↑" || key.label == "←" || key.label == "↓" || key.label == "→")
    val isOtherGamingKey = profile == KeyboardProfile.GAMING && !isGamingKey && !key.isModifier && key.label != "SPACE"

    val bgColor = when {
        isActive -> {
            when (profile) {
                KeyboardProfile.TERMUX -> Color(0xFF00FF66).copy(alpha = 0.3f)
                KeyboardProfile.VSCODE -> Color(0xFF007ACC).copy(alpha = 0.4f)
                KeyboardProfile.MACOS -> Color(0xFF0A84FF).copy(alpha = 0.4f)
                KeyboardProfile.GAMING -> Color(0xFFFF007F).copy(alpha = 0.4f)
                else -> Color(0xFF2A2E39)
            }
        }
        isToggled -> {
            when (profile) {
                KeyboardProfile.TERMUX -> Color(0xFF00FF66)
                KeyboardProfile.VSCODE -> Color(0xFF007ACC)
                KeyboardProfile.MACOS -> Color(0xFF0A84FF)
                KeyboardProfile.GAMING -> Color(0xFFFF007F)
                else -> Color(0xFF4F8CFF)
            }
        }
        isGamingKey -> Color(0xFF00FF66).copy(alpha = 0.15f)
        key.isModifier -> {
            when (profile) {
                KeyboardProfile.TERMUX -> Color(0xFF0D0D0D)
                KeyboardProfile.VSCODE -> Color(0xFF161B22)
                KeyboardProfile.MACOS -> Color(0xFF32363E)
                KeyboardProfile.GAMING -> Color(0xFF121218)
                else -> Color(0xFF2A2E39)
            }
        }
        key.isAccent -> {
            when (profile) {
                KeyboardProfile.TERMUX -> Color(0xFF151515)
                KeyboardProfile.VSCODE -> Color(0xFF1F242C)
                else -> Color(0xFF2A2E39)
            }
        }
        else -> {
            when (profile) {
                KeyboardProfile.TERMUX -> Color(0xFF000000)
                KeyboardProfile.VSCODE -> Color(0xFF1E1E24)
                KeyboardProfile.MACOS -> Color(0xFF1C1C1E)
                KeyboardProfile.GAMING -> Color(0xFF161622)
                else -> Color(0xFF1A1D26)
            }
        }
    }
    
    val textColor = when {
        isActive || isToggled -> Color.White
        isOtherGamingKey -> Color.White.copy(alpha = 0.35f)
        isGamingKey -> Color(0xFF00FF66)
        key.label == "⌫" -> Color(0xFFEF4444)
        profile == KeyboardProfile.TERMUX -> Color(0xFF00FF66)
        else -> Color.White
    }

    val borderColor = when {
        isToggled -> {
            when (profile) {
                KeyboardProfile.TERMUX -> Color(0xFF00FF66)
                KeyboardProfile.VSCODE -> Color(0xFF007ACC)
                KeyboardProfile.MACOS -> Color(0xFF0A84FF)
                KeyboardProfile.GAMING -> Color(0xFFFF007F)
                else -> Color(0xFF4F8CFF)
            }
        }
        isGamingKey -> Color(0xFF00FF66)
        profile == KeyboardProfile.TERMUX -> Color(0xFF333333)
        profile == KeyboardProfile.VSCODE -> Color(0xFF30363D)
        profile == KeyboardProfile.MACOS -> Color(0xFF3E3F46)
        profile == KeyboardProfile.GAMING -> Color(0xFF222233)
        else -> Color(0xFF2A2E39)
    }

    val cornerRadius = when {
        isProgrammerKey -> 4.dp
        profile == KeyboardProfile.MACOS -> 10.dp
        profile == KeyboardProfile.TERMUX -> 2.dp
        else -> 8.dp
    }

    if (keyStyle == "MECHANICAL") {
        val skirtColor = lerp(bgColor, Color.Black, 0.45f)
        val highlightedBorderColor = lerp(borderColor, Color.White, 0.1f)
        
        val animatedSkirtThickness by animateDpAsState(
            targetValue = if (isPressed || isActive) 1.dp else 4.dp,
            label = "skirtThickness"
        )
        val animatedTopOffset by animateDpAsState(
            targetValue = if (isPressed || isActive) 3.dp else 0.dp,
            label = "topOffset"
        )
        
        Box(
            modifier = modifier
                .fillMaxHeight()
                .padding(bottom = 1.dp)
                .pointerInput(key) {
                    if (key.label == "SPACE") {
                        detectDragGestures(
                            onDragStart = { 
                                isPressed = true
                                accumulatedDragX = 0f
                            },
                            onDragEnd = { isPressed = false },
                            onDragCancel = { isPressed = false },
                            onDrag = { change, dragAmount ->
                                accumulatedDragX += dragAmount.x
                                if (accumulatedDragX > 35f) {
                                    onSwipe(key, "RIGHT")
                                    accumulatedDragX = 0f
                                } else if (accumulatedDragX < -35f) {
                                    onSwipe(key, "LEFT")
                                    accumulatedDragX = 0f
                                }
                                change.consume()
                            }
                        )
                    } else {
                        detectDragGestures(
                            onDragStart = { isPressed = true },
                            onDragEnd = { isPressed = false },
                            onDragCancel = { isPressed = false },
                            onDrag = { change, dragAmount ->
                                if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                    if (dragAmount.x > 25f) onSwipe(key, "RIGHT")
                                    else if (dragAmount.x < -25f) onSwipe(key, "LEFT")
                                } else {
                                    if (dragAmount.y > 25f) onSwipe(key, "DOWN")
                                    else if (dragAmount.y < -25f) onSwipe(key, "UP")
                                }
                                change.consume()
                            }
                        )
                    }
                }
                .pointerInput(key) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            onKeyPress(key)
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onDoubleTap = {
                            onSwipe(key, "DOUBLE_TAP")
                        },
                        onLongPress = {
                            onSwipe(key, "LONG_PRESS")
                        }
                    )
                }
        ) {
            // 3D bottom base skirt shadow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 2.dp)
                    .background(skirtColor, RoundedCornerShape(cornerRadius))
            )
            
            // Compressing keycap surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .offset(y = animatedTopOffset)
                    .padding(bottom = animatedSkirtThickness)
                    .background(bgColor, RoundedCornerShape(cornerRadius))
                    .border(1.dp, highlightedBorderColor, RoundedCornerShape(cornerRadius)),
                contentAlignment = Alignment.Center
            ) {
                if (key.secondaryLabel.isNotEmpty()) {
                    Text(
                        text = key.secondaryLabel,
                        color = textColor.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 2.dp)
                    )
                }
                Text(
                    text = key.label,
                    color = textColor,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    } else {
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.92f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "keyScale"
        )
        
        Box(
            modifier = modifier
                .fillMaxHeight()
                .scale(scale)
                .background(bgColor, RoundedCornerShape(cornerRadius))
                .border(0.5.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
                .pointerInput(key) {
                    if (key.label == "SPACE") {
                        detectDragGestures(
                            onDragStart = { 
                                isPressed = true
                                accumulatedDragX = 0f
                            },
                            onDragEnd = { isPressed = false },
                            onDragCancel = { isPressed = false },
                            onDrag = { change, dragAmount ->
                                accumulatedDragX += dragAmount.x
                                if (accumulatedDragX > 35f) {
                                    onSwipe(key, "RIGHT")
                                    accumulatedDragX = 0f
                                } else if (accumulatedDragX < -35f) {
                                    onSwipe(key, "LEFT")
                                    accumulatedDragX = 0f
                                }
                                change.consume()
                            }
                        )
                    } else {
                        detectDragGestures(
                            onDragStart = { isPressed = true },
                            onDragEnd = { isPressed = false },
                            onDragCancel = { isPressed = false },
                            onDrag = { change, dragAmount ->
                                if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                    if (dragAmount.x > 25f) onSwipe(key, "RIGHT")
                                    else if (dragAmount.x < -25f) onSwipe(key, "LEFT")
                                } else {
                                    if (dragAmount.y > 25f) onSwipe(key, "DOWN")
                                    else if (dragAmount.y < -25f) onSwipe(key, "UP")
                                }
                                change.consume()
                            }
                        )
                    }
                }
                .pointerInput(key) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            onKeyPress(key)
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onDoubleTap = {
                            onSwipe(key, "DOUBLE_TAP")
                        },
                        onLongPress = {
                            onSwipe(key, "LONG_PRESS")
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (key.secondaryLabel.isNotEmpty()) {
                Text(
                    text = key.secondaryLabel,
                    color = textColor.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                )
            }
            Text(
                text = key.label,
                color = textColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
