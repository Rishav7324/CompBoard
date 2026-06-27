package com.example.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    
    companion object {
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_REPEAT_DELAY = longPreferencesKey("repeat_delay")
        val KEY_REPEAT_INTERVAL = longPreferencesKey("repeat_interval")
        val KEY_SWAP_CTRL_CAPS = booleanPreferencesKey("swap_ctrl_caps")
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { it[KEY_THEME] ?: "amoled_dark" }
    
    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }
}
