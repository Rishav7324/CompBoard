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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    override fun onCreateInputView(): View {
        val root = FrameLayout(this)
        
        keyboardView = KeyboardView(this)
        dispatcher = KeyEventDispatcher(this)
        keyboardView.setDispatcher(dispatcher)
        
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val dao = AppDatabase.getDatabase(this@CompBoardInputMethodService).clipboardHistoryDao()
                val entries by dao.getHistory().collectAsState(initial = emptyList())
                
                MyApplicationTheme {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        AnimatedVisibility(
                            visible = showClipboardPanel,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                            ClipboardPanel(
                                entries = entries,
                                onItemClick = { 
                                    commitTextToTarget(it.text)
                                    showClipboardPanel = false
                                },
                                onPinClick = { 
                                    scope.launch(Dispatchers.IO) {
                                        dao.insertEntry(it.copy(isPinned = !it.isPinned))
                                    }
                                },
                                onDeleteClick = {
                                    scope.launch(Dispatchers.IO) {
                                        dao.deleteEntryById(it.id)
                                    }
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
        
        root.addView(keyboardView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(composeView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        
        root.setViewTreeLifecycleOwner(lifecycleOwner)
        root.setViewTreeViewModelStoreOwner(lifecycleOwner)
        root.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        
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
