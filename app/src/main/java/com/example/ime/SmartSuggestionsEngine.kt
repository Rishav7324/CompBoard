package com.example.ime

import android.text.InputType
import android.view.inputmethod.EditorInfo

class SmartSuggestionsEngine {
    
    enum class ContextMode {
        TEXT, URL, EMAIL, CODE, NUMBER
    }
    
    fun detectContext(info: EditorInfo?): ContextMode {
        if (info == null) return ContextMode.TEXT
        
        val inputType = info.inputType
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val clazz = inputType and InputType.TYPE_MASK_CLASS
        
        if (clazz == InputType.TYPE_CLASS_NUMBER || clazz == InputType.TYPE_CLASS_PHONE || clazz == InputType.TYPE_CLASS_DATETIME) {
            return ContextMode.NUMBER
        }
        
        if (variation == InputType.TYPE_TEXT_VARIATION_URI) {
            return ContextMode.URL
        }
        
        if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS || variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) {
            return ContextMode.EMAIL
        }
        
        val pkg = info.packageName ?: ""
        if (pkg.contains("termux") || pkg.contains("vscode") || pkg.contains("jetbrains") || pkg.contains("code")) {
            return ContextMode.CODE
        }
        
        return ContextMode.TEXT
    }
    
    fun getSuggestionsForContext(mode: ContextMode): List<String> {
        return when (mode) {
            ContextMode.URL -> listOf(".com", ".in", ".org", ".io", "/", "?", "&", "#", "=", "://")
            ContextMode.EMAIL -> listOf("@", ".com", ".co.in", "gmail.com", "yahoo.com", ".net")
            ContextMode.CODE -> listOf("{}", "()", "[];", "=>", "!=", "===", "!==", "//", "/*", "*/", "::", "->")
            ContextMode.NUMBER -> listOf(".", ",", "+", "-", "*", "/", "=", "%", "(", ")")
            ContextMode.TEXT -> emptyList() // Needs text prediction (SpellCheckerSession) implementation
        }
    }
}
