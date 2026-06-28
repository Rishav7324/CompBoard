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
        val KEY_LAYOUT = stringPreferencesKey("current_layout")
        val KEY_PROFILE = stringPreferencesKey("keyboard_profile")
        val KEY_REMAPS = stringPreferencesKey("key_remaps")
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { it[KEY_THEME] ?: "amoled_dark" }
    
    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    val layoutFlow: Flow<String> = context.dataStore.data.map { it[KEY_LAYOUT] ?: "QWERTY" }
    
    suspend fun setLayout(layout: String) {
        context.dataStore.edit { it[KEY_LAYOUT] = layout }
    }

    val profileFlow: Flow<String> = context.dataStore.data.map { it[KEY_PROFILE] ?: "WINDOWS" }
    
    suspend fun setProfile(profile: String) {
        context.dataStore.edit { it[KEY_PROFILE] = profile }
    }

    val remapsFlow: Flow<Map<String, String>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_REMAPS] ?: "{}"
        try {
            val json = org.json.JSONObject(jsonStr)
            val map = mutableMapOf<String, String>()
            val keys = json.keys()
            while(keys.hasNext()) {
                val k = keys.next()
                map[k] = json.getString(k)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    suspend fun saveRemaps(remaps: Map<String, String>) {
        val json = org.json.JSONObject()
        for ((k, v) in remaps) {
            json.put(k, v)
        }
        context.dataStore.edit { it[KEY_REMAPS] = json.toString() }
    }
}
