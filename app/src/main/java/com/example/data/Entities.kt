package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "macros")
data class Macro(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val keySequence: String,
    val triggerKey: String,
    val delay: Long = 0
)

@Entity(tableName = "clipboard_history")
data class ClipboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)
