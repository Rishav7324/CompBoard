package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "macros")
data class Macro(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val triggerKey: Int? = null,
    val triggerModifiers: Int = 0,
    val isFnCombo: Boolean = false,
    val fnKey: Int? = null,
    val isLongPress: Boolean = false,
    val longPressKey: Int? = null,
    val steps: String = "[]",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val playCount: Int = 0
)

@Entity(tableName = "clipboard_history")
data class ClipboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val preview: String = "",
    val contentType: String = "text",
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val pasteCount: Int = 0
)

@Entity(tableName = "app_layout_rules")
data class AppLayoutRule(
    @PrimaryKey val packageName: String,
    val layoutName: String,
    val enableCursorPad: Boolean = false,
    val enableSuggestions: Boolean = true,
    val customProfile: String? = null
)

@Entity(tableName = "shortcut_profiles")
data class ShortcutProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isDefault: Boolean = false,
    val targetAppPackage: String? = null
)

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int,
    val modifiers: Int = 0, // Bitmask for Ctrl, Shift, Alt, Meta
    val primaryKeyCode: Int,
    val secondaryKeyCode: Int? = null, // For sequences like Ctrl+K, Ctrl+C
    val type: String = "single", // "single", "double", "triple", "sequence", "long_press", "macro"
    val actionType: String, // "text", "cursor", "keyboard", "app", "system", "macro"
    val actionPayload: String // JSON or string payload for the action
)

@Entity(tableName = "shortcut_analytics")
data class ShortcutAnalytics(
    @PrimaryKey val shortcutId: Int,
    val successCount: Int = 0,
    val conflictCount: Int = 0,
    val lastUsedTimestamp: Long = 0L
)
