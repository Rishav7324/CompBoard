package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MacroDao {
    @Query("SELECT * FROM macros")
    fun getAllMacros(): Flow<List<Macro>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: Macro)
    
    @Query("DELETE FROM macros WHERE id = :id")
    suspend fun deleteMacroById(id: Int)
}

@Dao
interface ClipboardHistoryDao {
    @Query("SELECT * FROM clipboard_history ORDER BY timestamp DESC LIMIT 50")
    fun getHistory(): Flow<List<ClipboardEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: ClipboardEntry)
    
    @Query("DELETE FROM clipboard_history WHERE id = :id")
    suspend fun deleteEntryById(id: Int)
    
    @Query("DELETE FROM clipboard_history WHERE isPinned = 0")
    suspend fun clearUnpinned()
}
