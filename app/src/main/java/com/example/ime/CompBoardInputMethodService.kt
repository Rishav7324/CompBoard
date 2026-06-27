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
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
                
                MyApplicationTheme {
                    Box(modifier = Modifier.fillMaxWidth().height(keyboardHeightDp)) {
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { keyboardView },
                            modifier = Modifier.fillMaxSize()
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
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "QWERTY",
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
