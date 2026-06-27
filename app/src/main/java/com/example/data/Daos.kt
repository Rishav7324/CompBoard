package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MacroDao {
    @Query("SELECT * FROM macros ORDER BY isFavorite DESC, createdAt DESC")
    fun getAllMacros(): Flow<List<Macro>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: Macro)
    
    @Query("DELETE FROM macros WHERE id = :id")
    suspend fun deleteMacroById(id: Int)
}

@Dao
interface ClipboardHistoryDao {
    @Query("SELECT * FROM clipboard_history ORDER BY isPinned DESC, timestamp DESC LIMIT :limit")
    fun getHistory(limit: Int = 50): Flow<List<ClipboardEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: ClipboardEntry)
    
    @Query("DELETE FROM clipboard_history WHERE id = :id")
    suspend fun deleteEntryById(id: Int)
    
    @Query("DELETE FROM clipboard_history WHERE isPinned = 0")
    suspend fun clearUnpinned()
    
    @Query("DELETE FROM clipboard_history WHERE timestamp < :timeLimit AND isPinned = 0")
    suspend fun deleteOlderThan(timeLimit: Long)
}

@Dao
interface AppLayoutRuleDao {
    @Query("SELECT * FROM app_layout_rules")
    fun getAllRules(): Flow<List<AppLayoutRule>>
    
    @Query("SELECT * FROM app_layout_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRuleForPackage(packageName: String): AppLayoutRule?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AppLayoutRule)
    
    @Query("DELETE FROM app_layout_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)
}
