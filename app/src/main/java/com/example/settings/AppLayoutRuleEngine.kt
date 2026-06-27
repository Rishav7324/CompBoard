package com.example.settings

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.AppLayoutRule
import kotlinx.coroutines.flow.Flow

class AppLayoutRuleEngine(private val context: Context) {
    
    private val dao = AppDatabase.getDatabase(context).appLayoutRuleDao()
    
    val allRules: Flow<List<AppLayoutRule>> = dao.getAllRules()
    
    suspend fun getRuleFor(packageName: String): AppLayoutRule? {
        return dao.getRuleForPackage(packageName) ?: getBuiltInRuleFor(packageName)
    }
    
    private fun getBuiltInRuleFor(packageName: String): AppLayoutRule? {
        return when {
            packageName == "com.termux" || packageName.startsWith("com.ssh.") -> {
                AppLayoutRule(packageName, "Terminal", enableCursorPad = true, enableSuggestions = false)
            }
            packageName == "com.android.chrome" -> {
                AppLayoutRule(packageName, "Default", enableCursorPad = false, enableSuggestions = true, customProfile = "URL")
            }
            packageName.startsWith("com.microsoft.vscode.") || packageName.startsWith("com.jetbrains.") -> {
                AppLayoutRule(packageName, "Programming", enableCursorPad = true, enableSuggestions = false, customProfile = "Coding")
            }
            packageName.endsWith("mail") -> {
                AppLayoutRule(packageName, "Default", enableCursorPad = false, enableSuggestions = true, customProfile = "Email")
            }
            else -> null
        }
    }
}
