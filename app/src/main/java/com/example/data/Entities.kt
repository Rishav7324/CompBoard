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
