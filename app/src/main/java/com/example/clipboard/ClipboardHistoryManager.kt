package com.example.clipboard

import android.content.ClipboardManager
import android.content.Context
import com.example.data.AppDatabase
import com.example.data.ClipboardEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClipboardHistoryManager(private val context: Context) {
    
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val dao = AppDatabase.getDatabase(context).clipboardHistoryDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        if (clipboardManager.hasPrimaryClip()) {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString() ?: return@OnPrimaryClipChangedListener
                
                scope.launch {
                    val preview = if (text.length > 100) text.substring(0, 100) + "..." else text
                    val entry = ClipboardEntry(
                        text = text,
                        preview = preview,
                        contentType = detectContentType(text)
                    )
                    dao.insertEntry(entry)
                }
            }
        }
    }
    
    fun startMonitoring() {
        clipboardManager.addPrimaryClipChangedListener(listener)
    }
    
    fun stopMonitoring() {
        clipboardManager.removePrimaryClipChangedListener(listener)
    }
    
    private fun detectContentType(text: String): String {
        return when {
            text.startsWith("http://") || text.startsWith("https://") -> "url"
            text.contains("@") && text.contains(".") -> "email"
            text.contains("{") && text.contains("}") -> "code"
            else -> "text"
        }
    }
}
