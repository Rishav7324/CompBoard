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

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner = IMELifecycleOwner()
        lifecycleOwner.onCreate()
        clipboardManager = ClipboardHistoryManager(this)
        clipboardManager.startMonitoring()
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
                
                var currentLayoutType by remember {
                    val prefs = getSharedPreferences("layout_prefs", android.content.Context.MODE_PRIVATE)
                    val savedLayout = prefs.getString("current_layout", "QWERTY") ?: "QWERTY"
                    mutableStateOf(KeyboardLayoutType.valueOf(savedLayout))
                }
                
                MyApplicationTheme {
                    Box(modifier = Modifier.fillMaxWidth().height(keyboardHeightDp)) {
                        VirtualKeyboard(
                            modifier = Modifier.fillMaxSize(),
                            layoutType = currentLayoutType,
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
                                    keyInfo.label == "SPACE" || keyInfo.label == "EN" -> {
                                        dispatcher.sendText(" ")
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
                                                if (ModifierState.ctrlPressed) dispatcher.sendKeyUp(keyInfo.code)
                                                else dispatcher.sendKeyDown(keyInfo.code)
                                                ModifierState.log(keyInfo.label)
                                            }
                                            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                                                if (ModifierState.shiftPressed) dispatcher.sendKeyUp(keyInfo.code)
                                                else dispatcher.sendKeyDown(keyInfo.code)
                                                ModifierState.log(keyInfo.label)
                                            }
                                            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> {
                                                if (ModifierState.altPressed) dispatcher.sendKeyUp(keyInfo.code)
                                                else dispatcher.sendKeyDown(keyInfo.code)
                                                ModifierState.log(keyInfo.label)
                                            }
                                            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT -> {
                                                if (ModifierState.metaPressed) dispatcher.sendKeyUp(keyInfo.code)
                                                else dispatcher.sendKeyDown(keyInfo.code)
                                                ModifierState.log(keyInfo.label)
                                            }
                                            KeyEvent.KEYCODE_CAPS_LOCK -> {
                                                dispatcher.sendKeyDown(keyInfo.code)
                                                dispatcher.sendKeyUp(keyInfo.code)
                                                ModifierState.log(keyInfo.label)
                                            }
                                        }
                                    }
                                    else -> {
                                        dispatcher.sendKeyDown(keyInfo.code)
                                        dispatcher.sendKeyUp(keyInfo.code)
                                        
                                        val noModifiers = ModifierState.ctrlPressed == false && ModifierState.altPressed == false && ModifierState.metaPressed == false
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
                                                // Trigger recomposition if needed
                                            }
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
                                            getSharedPreferences("layout_prefs", android.content.Context.MODE_PRIVATE)
                                                .edit()
                                                .putString("current_layout", nextLayout.name)
                                                .apply()
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
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val chips = listOf("CTRL", "ALT", "SHIFT", "CAPS", "FN")
                                chips.forEach { chip ->
                                    // Static mock state for now
                                    val isActive = chip == "CTRL" || chip == "SHIFT"
                                    val bgColor = if (isActive) Color(0xFF5B21B6) else Color(0xFF111111)
                                    val textColor = if (isActive) Color.White else Color(0xFFAAAAAA)
                                    val borderColor = if (isActive) Color(0xFF5B21B6) else Color(0xFF333333)
                                    
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

                            // Debug Log Overlay
                        if (ModifierState.debugLogs.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 32.dp, end = 8.dp)
                                    .background(Color.Black.copy(alpha = 0.7f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(0.3f),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.Text("Debug Log", color = Color.Gray, fontSize = 10.sp)
                                        androidx.compose.material3.IconButton(
                                            onClick = {
                                                val textContent = ModifierState.debugLogs.joinToString("\n")
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                                    val resolver = contentResolver
                                                    val contentValues = android.content.ContentValues().apply {
                                                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "keyboard_log_${System.currentTimeMillis()}.txt")
                                                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                                                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                                                    }
                                                    val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                                                    if (uri != null) {
                                                        resolver.openOutputStream(uri)?.use { outputStream ->
                                                            outputStream.write(textContent.toByteArray())
                                                        }
                                                        android.widget.Toast.makeText(this@CompBoardInputMethodService, "Log downloaded to Downloads folder", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    val file = java.io.File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "keyboard_log_${System.currentTimeMillis()}.txt")
                                                    file.writeText(textContent)
                                                    android.widget.Toast.makeText(this@CompBoardInputMethodService, "Log downloaded to App Downloads folder", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.padding(2.dp)
                                        ) {
                                            androidx.compose.material3.Text("DL", fontSize=10.sp, color=Color.Gray)
                                        }
                                    }
                                    ModifierState.debugLogs.forEach { logMsg ->
                                        androidx.compose.material3.Text(
                                            text = logMsg,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                }
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
        
        val prefs = getSharedPreferences("remaps_prefs", android.content.Context.MODE_PRIVATE)
        val remap = prefs.getString(keyCode.toString(), null)
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
        
        val prefs = getSharedPreferences("remaps_prefs", android.content.Context.MODE_PRIVATE)
        val remap = prefs.getString(keyCode.toString(), null)
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
