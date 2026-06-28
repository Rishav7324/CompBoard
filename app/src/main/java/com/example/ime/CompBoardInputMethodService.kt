package com.example.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.KeyEvent
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.clipboard.ClipboardHistoryManager
import com.example.clipboard.ClipboardPanel
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CompBoardInputMethodService : InputMethodService() {

    private lateinit var keyboardView: KeyboardView
    private lateinit var dispatcher: KeyEventDispatcher
    private lateinit var clipboardManager: ClipboardHistoryManager
    private lateinit var lifecycleOwner: IMELifecycleOwner
    private var showClipboardPanel by mutableStateOf(false)
    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentRemaps: Map<String, String> = emptyMap()

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner = IMELifecycleOwner()
        lifecycleOwner.onCreate()
        clipboardManager = ClipboardHistoryManager(this)
        clipboardManager.startMonitoring()
        
        scope.launch {
            val settingsManager = com.example.settings.SettingsManager(this@CompBoardInputMethodService)
            settingsManager.remapsFlow.collect { remaps ->
                currentRemaps = remaps
            }
        }
    }

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return true
    }

    override fun onCreateInputView(): View {
        val root = FrameLayout(this)
        
        window.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(lifecycleOwner)
            decorView.setViewTreeViewModelStoreOwner(lifecycleOwner)
            decorView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        }
        
        keyboardView = KeyboardView(this)
        dispatcher = KeyEventDispatcher(this)
        keyboardView.setDispatcher(dispatcher)

        // Load shortcuts
        scope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@CompBoardInputMethodService)
            // Listen to default profile shortcuts (for demo, just profileId 1 or all)
            db.shortcutDao().getAllProfiles().collect { profiles ->
                if (profiles.isNotEmpty()) {
                    val defaultProfile = profiles.firstOrNull { it.isDefault } ?: profiles.first()
                    db.shortcutDao().getShortcutsForProfile(defaultProfile.id).collect { shortcuts ->
                        withContext(Dispatchers.Main) {
                            dispatcher.shortcutManager.loadShortcuts(shortcuts)
                        }
                    }
                }
            }
        }
        
        val composeView = ComposeView(this).apply {
            isFocusable = false
            isFocusableInTouchMode = false
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val dao = AppDatabase.getDatabase(this@CompBoardInputMethodService).clipboardHistoryDao()
                val entries by dao.getHistory().collectAsState(initial = emptyList())
                
                val displayMetrics = resources.displayMetrics
                val keyboardHeightDp = with(androidx.compose.ui.platform.LocalDensity.current) {
                    (displayMetrics.widthPixels * 0.70f).toDp()
                }
                
                val settingsManager = remember { com.example.settings.SettingsManager(this@CompBoardInputMethodService) }
                val layoutFlowStr by settingsManager.layoutFlow.collectAsState(initial = "QWERTY")
                var currentLayoutType by remember {
                    mutableStateOf(KeyboardLayoutType.valueOf(layoutFlowStr))
                }
                
                LaunchedEffect(layoutFlowStr) {
                    try {
                        currentLayoutType = KeyboardLayoutType.valueOf(layoutFlowStr)
                    } catch (e: Exception) {
                        currentLayoutType = KeyboardLayoutType.QWERTY
                    }
                }

                val profileFlowStr by settingsManager.profileFlow.collectAsState(initial = "WINDOWS")
                var currentProfile by remember {
                    mutableStateOf(KeyboardProfile.valueOf(profileFlowStr))
                }
                
                LaunchedEffect(profileFlowStr) {
                    try {
                        currentProfile = KeyboardProfile.valueOf(profileFlowStr)
                    } catch (e: Exception) {
                        currentProfile = KeyboardProfile.WINDOWS
                    }
                }
                
                MyApplicationTheme {
                    Box(modifier = Modifier.fillMaxWidth().height(keyboardHeightDp)) {
                        VirtualKeyboard(
                            modifier = Modifier.fillMaxSize(),
                            layoutType = currentLayoutType,
                            profile = currentProfile,
                            onKeyPress = { keyInfo ->
                                val prefs = getSharedPreferences("haptics_prefs", android.content.Context.MODE_PRIVATE)
                                if (prefs.getBoolean("haptics_enabled", true)) {
                                    val durationMs = prefs.getFloat("haptics_duration", 20f).toLong()
                                    val intensity = prefs.getFloat("haptics_intensity", 0.5f)
                                    val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                                    if (vibrator.hasVibrator()) {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            val amplitude = (intensity * 255).toInt().coerceIn(1, 255)
                                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs, amplitude))
                                        } else {
                                            @Suppress("DEPRECATION")
                                            vibrator.vibrate(durationMs)
                                        }
                                    }
                                }

                                when {
                                    keyInfo.label == "Ctrl+P" -> {
                                        dispatcher.sendKeyDown(KeyEvent.KEYCODE_CTRL_LEFT)
                                        dispatcher.sendKeyDown(KeyEvent.KEYCODE_P)
                                        dispatcher.sendKeyUp(KeyEvent.KEYCODE_P)
                                        dispatcher.sendKeyUp(KeyEvent.KEYCODE_CTRL_LEFT)
                                        ModifierState.log("Ctrl+P")
                                    }
                                    keyInfo.label == "Ctrl+Shift+P" -> {
                                        dispatcher.sendKeyDown(KeyEvent.KEYCODE_CTRL_LEFT)
                                        dispatcher.sendKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT)
                                        dispatcher.sendKeyDown(KeyEvent.KEYCODE_P)
                                        dispatcher.sendKeyUp(KeyEvent.KEYCODE_P)
                                        dispatcher.sendKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT)
                                        dispatcher.sendKeyUp(KeyEvent.KEYCODE_CTRL_LEFT)
                                        ModifierState.log("Ctrl+Shift+P")
                                    }
                                    keyInfo.label == "Ctrl+/" -> {
                                        dispatcher.sendKeyDown(KeyEvent.KEYCODE_CTRL_LEFT)
                                        dispatcher.sendKeyDown(KeyEvent.KEYCODE_SLASH)
                                        dispatcher.sendKeyUp(KeyEvent.KEYCODE_SLASH)
                                        dispatcher.sendKeyUp(KeyEvent.KEYCODE_CTRL_LEFT)
                                        ModifierState.log("Ctrl+/")
                                    }
                                    keyInfo.label == "Ctrl+F" -> {
                                        dispatcher.sendKeyDown(KeyEvent.KEYCODE_CTRL_LEFT)
                                        dispatcher.sendKeyDown(KeyEvent.KEYCODE_F)
                                        dispatcher.sendKeyUp(KeyEvent.KEYCODE_F)
                                        dispatcher.sendKeyUp(KeyEvent.KEYCODE_CTRL_LEFT)
                                        ModifierState.log("Ctrl+F")
                                    }
                                    keyInfo.label == "Ctrl+`" -> {
                                        dispatcher.sendKeyDown(KeyEvent.KEYCODE_CTRL_LEFT)
                                        dispatcher.sendKeyDown(KeyEvent.KEYCODE_GRAVE)
                                        dispatcher.sendKeyUp(KeyEvent.KEYCODE_GRAVE)
                                        dispatcher.sendKeyUp(KeyEvent.KEYCODE_CTRL_LEFT)
                                        ModifierState.log("Ctrl+`")
                                    }
                                    keyInfo.label == "Undo" -> {
                                        dispatcher.sendKeyDown(KeyEvent.KEYCODE_CTRL_LEFT)
                                        dispatcher.sendKeyDown(KeyEvent.KEYCODE_Z)
                                        dispatcher.sendKeyUp(KeyEvent.KEYCODE_Z)
                                        dispatcher.sendKeyUp(KeyEvent.KEYCODE_CTRL_LEFT)
                                        ModifierState.log("Undo")
                                    }
                                    keyInfo.label == "Redo" -> {
                                        dispatcher.sendKeyDown(KeyEvent.KEYCODE_CTRL_LEFT)
                                        dispatcher.sendKeyDown(KeyEvent.KEYCODE_Y)
                                        dispatcher.sendKeyUp(KeyEvent.KEYCODE_Y)
                                        dispatcher.sendKeyUp(KeyEvent.KEYCODE_CTRL_LEFT)
                                        ModifierState.log("Redo")
                                    }
                                    keyInfo.code == KeyEvent.KEYCODE_SPACE || keyInfo.label == "SPACE" -> {
                                        val noModifiers = !ModifierState.ctrlPressed && !ModifierState.ctrlLocked && !ModifierState.altPressed && !ModifierState.altLocked && !ModifierState.metaPressed && !ModifierState.metaLocked
                                        if (noModifiers) {
                                            dispatcher.sendText(" ")
                                        } else {
                                            dispatcher.sendKeyDown(KeyEvent.KEYCODE_SPACE)
                                            dispatcher.sendKeyUp(KeyEvent.KEYCODE_SPACE)
                                            if (ModifierState.ctrlPressed && !ModifierState.ctrlLocked) { ModifierState.ctrlPressed = false; dispatcher.sendKeyUp(KeyEvent.KEYCODE_CTRL_LEFT) }
                                            if (ModifierState.altPressed && !ModifierState.altLocked) { ModifierState.altPressed = false; dispatcher.sendKeyUp(KeyEvent.KEYCODE_ALT_LEFT) }
                                            if (ModifierState.metaPressed && !ModifierState.metaLocked) { ModifierState.metaPressed = false; dispatcher.sendKeyUp(KeyEvent.KEYCODE_META_LEFT) }
                                        }
                                        ModifierState.log("SPACE")
                                    }
                                    keyInfo.code == KeyEvent.KEYCODE_DEL || keyInfo.code == KeyEvent.KEYCODE_ENTER || 
                                    keyInfo.code == KeyEvent.KEYCODE_DPAD_LEFT || keyInfo.code == KeyEvent.KEYCODE_DPAD_RIGHT || 
                                    keyInfo.code == KeyEvent.KEYCODE_DPAD_UP || keyInfo.code == KeyEvent.KEYCODE_DPAD_DOWN ||
                                    keyInfo.code == KeyEvent.KEYCODE_ESCAPE || keyInfo.code == KeyEvent.KEYCODE_TAB -> {
                                        dispatcher.sendKeyDown(keyInfo.code)
                                        dispatcher.sendKeyUp(keyInfo.code)
                                        ModifierState.log(keyInfo.label)
                                    }
                                    keyInfo.isModifier -> {
                                        when (keyInfo.code) {
                                            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> {
                                                ModifierState.ctrlPressed = !ModifierState.ctrlPressed
                                                if (ModifierState.ctrlPressed) dispatcher.sendKeyDown(keyInfo.code) else dispatcher.sendKeyUp(keyInfo.code)
                                                ModifierState.log(keyInfo.label)
                                            }
                                            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                                                ModifierState.shiftPressed = !ModifierState.shiftPressed
                                                if (ModifierState.shiftPressed) dispatcher.sendKeyDown(keyInfo.code) else dispatcher.sendKeyUp(keyInfo.code)
                                                ModifierState.log(keyInfo.label)
                                            }
                                            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> {
                                                ModifierState.altPressed = !ModifierState.altPressed
                                                if (ModifierState.altPressed) dispatcher.sendKeyDown(keyInfo.code) else dispatcher.sendKeyUp(keyInfo.code)
                                                ModifierState.log(keyInfo.label)
                                            }
                                            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT -> {
                                                ModifierState.metaPressed = !ModifierState.metaPressed
                                                if (ModifierState.metaPressed) dispatcher.sendKeyDown(keyInfo.code) else dispatcher.sendKeyUp(keyInfo.code)
                                                ModifierState.log(keyInfo.label)
                                            }
                                            KeyEvent.KEYCODE_CAPS_LOCK -> {
                                                ModifierState.capsLockEnabled = !ModifierState.capsLockEnabled
                                                dispatcher.sendKeyDown(keyInfo.code)
                                                dispatcher.sendKeyUp(keyInfo.code)
                                                ModifierState.log(keyInfo.label)
                                            }
                                            KeyEvent.KEYCODE_FUNCTION -> {
                                                ModifierState.log(keyInfo.label)
                                            }
                                        }
                                    }
                                    else -> {
                                        val isCtrl = ModifierState.ctrlPressed || ModifierState.ctrlLocked
                                        val isAlt = ModifierState.altPressed || ModifierState.altLocked
                                        
                                        if (keyInfo.code == KeyEvent.KEYCODE_M && isCtrl && isAlt) {
                                            // Macro: Paste, New Line, Paste Again
                                            ModifierState.ctrlPressed = true // ensure it's on
                                            dispatcher.sendKeyDown(KeyEvent.KEYCODE_V)
                                            dispatcher.sendKeyUp(KeyEvent.KEYCODE_V)
                                            ModifierState.ctrlPressed = false
                                            
                                            dispatcher.sendKeyDown(KeyEvent.KEYCODE_ENTER)
                                            dispatcher.sendKeyUp(KeyEvent.KEYCODE_ENTER)
                                            
                                            ModifierState.ctrlPressed = true
                                            dispatcher.sendKeyDown(KeyEvent.KEYCODE_V)
                                            dispatcher.sendKeyUp(KeyEvent.KEYCODE_V)
                                            
                                            // Restore modifiers
                                            ModifierState.ctrlPressed = isCtrl
                                            
                                            if (!ModifierState.ctrlLocked) ModifierState.ctrlPressed = false
                                            if (!ModifierState.altLocked) ModifierState.altPressed = false
                                            return@VirtualKeyboard
                                        }

                                        dispatcher.sendKeyDown(keyInfo.code)
                                        dispatcher.sendKeyUp(keyInfo.code)
                                        
                                        val noModifiers = !ModifierState.ctrlPressed && !ModifierState.ctrlLocked && !ModifierState.altPressed && !ModifierState.altLocked && !ModifierState.metaPressed && !ModifierState.metaLocked
                                        if (keyInfo.code == KeyEvent.KEYCODE_UNKNOWN || (noModifiers && keyInfo.label.length == 1)) {
                                            val isShifted = ModifierState.shiftPressed || ModifierState.capsLockEnabled
                                            val textToCommit = if (keyInfo.label.length == 1) {
                                                val char = keyInfo.label[0]
                                                if (char in 'A'..'Z') {
                                                    if (isShifted) char.uppercaseChar().toString() else char.lowercaseChar().toString()
                                                } else {
                                                    if (ModifierState.shiftPressed) {
                                                        val shiftMap = mapOf(
                                                            '`' to '~', '1' to '!', '2' to '@', '3' to '#', '4' to '$', '5' to '%',
                                                            '6' to '^', '7' to '&', '8' to '*', '9' to '(', '0' to ')',
                                                            '-' to '_', '=' to '+', '[' to '{', ']' to '}', '\\' to '|',
                                                            ';' to ':', '\'' to '"', ',' to '<', '.' to '>', '/' to '?'
                                                        )
                                                        shiftMap[char]?.toString() ?: char.toString()
                                                    } else {
                                                        char.toString()
                                                    }
                                                }
                                            } else {
                                                keyInfo.label
                                            }
                                            dispatcher.sendText(textToCommit)
                                            ModifierState.log(textToCommit)
                                            
                                            if (ModifierState.shiftPressed && !ModifierState.capsLockEnabled) {
                                                ModifierState.shiftPressed = false
                                                dispatcher.sendKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT)
                                            }
                                            if (ModifierState.ctrlPressed && !ModifierState.ctrlLocked) {
                                                ModifierState.ctrlPressed = false
                                                dispatcher.sendKeyUp(KeyEvent.KEYCODE_CTRL_LEFT)
                                            }
                                            if (ModifierState.altPressed && !ModifierState.altLocked) {
                                                ModifierState.altPressed = false
                                                dispatcher.sendKeyUp(KeyEvent.KEYCODE_ALT_LEFT)
                                            }
                                            if (ModifierState.metaPressed && !ModifierState.metaLocked) {
                                                ModifierState.metaPressed = false
                                                dispatcher.sendKeyUp(KeyEvent.KEYCODE_META_LEFT)
                                            }
                                        }
                                    }
                                }
                            },
                            onSwipe = { keyInfo, action ->
                                when (keyInfo.label) {
                                    "SPACE" -> {
                                        if (action == "LEFT") {
                                            dispatcher.sendKeyDown(KeyEvent.KEYCODE_DPAD_LEFT)
                                            dispatcher.sendKeyUp(KeyEvent.KEYCODE_DPAD_LEFT)
                                        } else if (action == "RIGHT") {
                                            dispatcher.sendKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT)
                                            dispatcher.sendKeyUp(KeyEvent.KEYCODE_DPAD_RIGHT)
                                        }
                                    }
                                    "⌫" -> {
                                        if (action == "LEFT") {
                                            // Delete word roughly (Ctrl+Backspace)
                                            dispatcher.sendKeyDown(KeyEvent.KEYCODE_CTRL_LEFT)
                                            dispatcher.sendKeyDown(KeyEvent.KEYCODE_DEL)
                                            dispatcher.sendKeyUp(KeyEvent.KEYCODE_DEL)
                                            dispatcher.sendKeyUp(KeyEvent.KEYCODE_CTRL_LEFT)
                                        } else if (action == "DOWN") {
                                            // Delete line roughly (Shift+Home then Del)
                                            dispatcher.sendKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT)
                                            dispatcher.sendKeyDown(KeyEvent.KEYCODE_MOVE_HOME)
                                            dispatcher.sendKeyUp(KeyEvent.KEYCODE_MOVE_HOME)
                                            dispatcher.sendKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT)
                                            dispatcher.sendKeyDown(KeyEvent.KEYCODE_DEL)
                                            dispatcher.sendKeyUp(KeyEvent.KEYCODE_DEL)
                                        }
                                    }
                                    "SHIFT" -> {
                                        if (action == "UP") {
                                            ModifierState.capsLockEnabled = !ModifierState.capsLockEnabled
                                        } else if (action == "DOUBLE_TAP") {
                                            ModifierState.capsLockEnabled = !ModifierState.capsLockEnabled
                                        }
                                    }
                                    "CTRL" -> {
                                        if (action == "DOUBLE_TAP") ModifierState.ctrlLocked = !ModifierState.ctrlLocked
                                    }
                                    "ALT" -> {
                                        if (action == "DOUBLE_TAP") ModifierState.altLocked = !ModifierState.altLocked
                                    }
                                    "WIN" -> {
                                        if (action == "DOUBLE_TAP") ModifierState.metaLocked = !ModifierState.metaLocked
                                    }
                                    "{" -> {
                                        if (action == "LONG_PRESS") {
                                            dispatcher.sendText("{\n\t\n}")
                                            dispatcher.sendKeyDown(KeyEvent.KEYCODE_DPAD_UP)
                                            dispatcher.sendKeyUp(KeyEvent.KEYCODE_DPAD_UP)
                                        }
                                    }
                                    "[" -> {
                                        if (action == "LONG_PRESS") {
                                            dispatcher.sendText("[\n\t\n]")
                                            dispatcher.sendKeyDown(KeyEvent.KEYCODE_DPAD_UP)
                                            dispatcher.sendKeyUp(KeyEvent.KEYCODE_DPAD_UP)
                                        }
                                    }
                                    "(" -> {
                                        if (action == "LONG_PRESS") {
                                            dispatcher.sendText("(\n\t\n)")
                                            dispatcher.sendKeyDown(KeyEvent.KEYCODE_DPAD_UP)
                                            dispatcher.sendKeyUp(KeyEvent.KEYCODE_DPAD_UP)
                                        }
                                    }
                                    "<" -> {
                                        if (action == "LONG_PRESS") {
                                            dispatcher.sendText("<\n\t\n>")
                                            dispatcher.sendKeyDown(KeyEvent.KEYCODE_DPAD_UP)
                                            dispatcher.sendKeyUp(KeyEvent.KEYCODE_DPAD_UP)
                                        }
                                    }
                                }
                            }
                        )
                        
                        // Top Status Bar (Toolbar) overlay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp) // matches ~0.1f of 280dp
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                androidx.compose.material3.Text(
                                    text = "📋 ${entries.size}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.clickable { toggleClipboardPanel() }.padding(4.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                        )
                                        .clickable {
                                            val nextLayout = when(currentLayoutType) {
                                                KeyboardLayoutType.QWERTY -> KeyboardLayoutType.DVORAK
                                                KeyboardLayoutType.DVORAK -> KeyboardLayoutType.COLEMAK
                                                KeyboardLayoutType.COLEMAK -> KeyboardLayoutType.QWERTY
                                                else -> KeyboardLayoutType.QWERTY
                                            }
                                            currentLayoutType = nextLayout
                                            scope.launch {
                                                settingsManager.setLayout(nextLayout.name)
                                            }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    androidx.compose.material3.Text(
                                        text = currentLayoutType.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)
                                        )
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                        )
                                        .clickable {
                                            val nextProfile = when(currentProfile) {
                                                KeyboardProfile.WINDOWS -> KeyboardProfile.LINUX
                                                KeyboardProfile.LINUX -> KeyboardProfile.MACOS
                                                KeyboardProfile.MACOS -> KeyboardProfile.TERMUX
                                                KeyboardProfile.TERMUX -> KeyboardProfile.VSCODE
                                                KeyboardProfile.VSCODE -> KeyboardProfile.GAMING
                                                KeyboardProfile.GAMING -> KeyboardProfile.WINDOWS
                                            }
                                            currentProfile = nextProfile
                                            scope.launch {
                                                settingsManager.setProfile(nextProfile.name)
                                            }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "👤 ${currentProfile.name}",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)
                                        )
                                    )
                                }
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val chips = listOf("CTRL", "ALT", "SHIFT", "WIN", "CAPS")
                                chips.forEach { chip ->
                                    val isActive = when (chip) {
                                        "CTRL" -> ModifierState.ctrlPressed || ModifierState.ctrlLocked
                                        "ALT" -> ModifierState.altPressed || ModifierState.altLocked
                                        "SHIFT" -> ModifierState.shiftPressed
                                        "CAPS" -> ModifierState.capsLockEnabled
                                        "WIN" -> ModifierState.metaPressed || ModifierState.metaLocked
                                        else -> false
                                    }
                                    val bgColor = if (isActive) Color(0xFF4F8CFF) else Color(0xFF1A1D26)
                                    val textColor = if (isActive) Color.White else Color(0xFFAAAAAA)
                                    val borderColor = if (isActive) Color(0xFF4F8CFF) else Color(0xFF2A2E39)
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = bgColor,
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = borderColor,
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        androidx.compose.material3.Text(
                                            text = chip,
                                            color = textColor,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                            
                            // WPM Gauge Overlay
                            androidx.compose.runtime.LaunchedEffect(Unit) {
                                while (true) {
                                    kotlinx.coroutines.delay(1000)
                                    val now = System.currentTimeMillis()
                                    val thirtySecondsAgo = now - 30_000
                                    ModifierState.keystrokeTimestamps.removeAll { it < thirtySecondsAgo }
                                }
                            }
                            
                            val wpm = (ModifierState.keystrokeTimestamps.size / 5.0f) * 2f
                            
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(top = 32.dp, start = 8.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val gaugeColor = when {
                                        wpm > 80 -> Color.Green
                                        wpm > 40 -> Color.Yellow
                                        else -> Color.LightGray
                                    }
                                    androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
                                        drawCircle(color = gaugeColor)
                                    }
                                    androidx.compose.material3.Text(
                                        text = "${wpm.toInt()} WPM",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }


                        
                        AnimatedVisibility(
                                visible = showClipboardPanel,
                                enter = slideInVertically(initialOffsetY = { it }),
                                exit = slideOutVertically(targetOffsetY = { it })
                            ) {
                                ClipboardPanel(
                                    entries = entries,
                                    onItemClick = { 
                                        commitTextToTarget(clipboardManager.getDecryptedText(it))
                                        showClipboardPanel = false
                                    },
                                    onPinClick = { 
                                        scope.launch(Dispatchers.IO) {
                                            dao.insertEntry(it.copy(isPinned = !it.isPinned))
                                        }
                                    },
                                    onDeleteClick = {
                                        clipboardManager.deleteEntry(it)
                                    },
                                    onClearAll = {
                                        scope.launch(Dispatchers.IO) {
                                            dao.clearUnpinned()
                                        }
                                    },
                                    onClose = { showClipboardPanel = false }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        root.addView(composeView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { gravity = android.view.Gravity.BOTTOM })
        
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardView.updateForEditorInfo(info)
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()
    }
    
    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        showClipboardPanel = false
        lifecycleOwner.onPause()
        lifecycleOwner.onStop()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && !ModifierState.activeKeys.contains(keyCode)) {
            ModifierState.activeKeys.add(keyCode)
            ModifierState.log("Physical: $keyCode")
        }
        
        val remap = currentRemaps[keyCode.toString()]
        if (remap != null) {
            when {
                remap.startsWith("CHAR:") -> {
                    val char = remap.substringAfter("CHAR:")
                    commitTextToTarget(char)
                    return true
                }
                remap.startsWith("SHORTCUT:") -> {
                    val shortcut = remap.substringAfter("SHORTCUT:")
                    if (shortcut == "COPY") {
                        currentInputConnection?.performContextMenuAction(android.R.id.copy)
                    } else if (shortcut == "PASTE") {
                        currentInputConnection?.performContextMenuAction(android.R.id.paste)
                    } else if (shortcut == "CUT") {
                        currentInputConnection?.performContextMenuAction(android.R.id.cut)
                    } else if (shortcut == "HOME") {
                        sendDownUpKeyEvents(KeyEvent.KEYCODE_HOME)
                    }
                    return true
                }
                remap.startsWith("KEY:") -> {
                    val newKeyCode = remap.substringAfter("KEY:").toIntOrNull()
                    if (newKeyCode != null && event != null) {
                        val newEvent = KeyEvent(event.downTime, event.eventTime, event.action, newKeyCode, event.repeatCount, event.metaState, event.deviceId, event.scanCode, event.flags, event.source)
                        return super.onKeyDown(newKeyCode, newEvent)
                    }
                }
            }
        }
        
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null) {
            ModifierState.activeKeys.remove(keyCode)
        }
        
        val remap = currentRemaps[keyCode.toString()]
        if (remap != null) {
            when {
                remap.startsWith("CHAR:") -> return true
                remap.startsWith("SHORTCUT:") -> return true
                remap.startsWith("KEY:") -> {
                    val newKeyCode = remap.substringAfter("KEY:").toIntOrNull()
                    if (newKeyCode != null && event != null) {
                        val newEvent = KeyEvent(event.downTime, event.eventTime, event.action, newKeyCode, event.repeatCount, event.metaState, event.deviceId, event.scanCode, event.flags, event.source)
                        return super.onKeyUp(newKeyCode, newEvent)
                    }
                }
            }
        }
        
        return super.onKeyUp(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardManager.stopMonitoring()
        lifecycleOwner.onDestroy()
    }

    fun dispatchKeyEventToTarget(event: KeyEvent) {
        val ic: InputConnection? = currentInputConnection
        if (ic != null) {
            ic.sendKeyEvent(event)
        } else {
            sendDownUpKeyEvents(event.keyCode)
        }
    }
    
    fun commitTextToTarget(text: CharSequence) {
        currentInputConnection?.commitText(text, 1)
    }
    
    fun toggleClipboardPanel() {
        showClipboardPanel = !showClipboardPanel
    }
}
