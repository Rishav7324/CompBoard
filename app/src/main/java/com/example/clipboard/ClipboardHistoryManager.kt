package com.example.clipboard

import android.content.ClipboardManager
import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.example.data.AppDatabase
import com.example.data.ClipboardEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

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
                    val isSensitiveContent = detectSensitive(text)
                    val storedText = if (isSensitiveContent) saveSensitiveText(text) else text
                    
                    val preview = if (text.length > 100) text.substring(0, 100) + "..." else text
                    // Mask preview if sensitive
                    val finalPreview = if (isSensitiveContent) "•••••••• (Encrypted)" else preview
                    
                    val entry = ClipboardEntry(
                        text = storedText,
                        preview = finalPreview,
                        contentType = if (isSensitiveContent) "secure" else detectContentType(text)
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
    
    private fun detectSensitive(text: String): Boolean {
        val lowerText = text.lowercase()
        return lowerText.contains("password") || 
               lowerText.contains("secret") || 
               lowerText.contains("api_key") || 
               lowerText.contains("bearer ")
    }
    
    private fun saveSensitiveText(text: String): String {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            val filename = "secure_clip_${UUID.randomUUID()}.enc"
            val file = File(context.filesDir, filename)
            
            val encryptedFile = EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            
            encryptedFile.openFileOutput().use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
            }
            return "encrypted://$filename"
        } catch (e: Exception) {
            e.printStackTrace()
            return text
        }
    }
    
    fun getDecryptedText(entry: ClipboardEntry): String {
        if (!entry.text.startsWith("encrypted://")) return entry.text
        
        val filename = entry.text.removePrefix("encrypted://")
        val file = File(context.filesDir, filename)
        if (!file.exists()) return entry.text
        
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
                
            val encryptedFile = EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            
            encryptedFile.openFileInput().use { input ->
                val bytes = ByteArrayOutputStream()
                var nextByte: Int
                while (input.read().also { nextByte = it } != -1) {
                    bytes.write(nextByte)
                }
                bytes.toByteArray().toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            entry.text // fallback
        }
    }
    
    fun deleteEntry(entry: ClipboardEntry) {
        scope.launch {
            if (entry.text.startsWith("encrypted://")) {
                val filename = entry.text.removePrefix("encrypted://")
                val file = File(context.filesDir, filename)
                if (file.exists()) {
                    file.delete()
                }
            }
            dao.deleteEntryById(entry.id)
        }
    }
}
