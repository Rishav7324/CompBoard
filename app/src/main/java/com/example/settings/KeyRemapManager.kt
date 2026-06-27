package com.example.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

class KeyRemapManager(private val context: Context) {
    
    companion object {
        val KEY_REMAPS = stringPreferencesKey("remaps_json")
    }
    
    sealed class RemapTarget {
        data class ToKey(val keycode: Int, val metaState: Int): RemapTarget()
        data class ToText(val text: String): RemapTarget()
        object Disabled: RemapTarget()
    }
    
    val remapsFlow: Flow<Map<Int, RemapTarget>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_REMAPS] ?: "{}"
        val map = mutableMapOf<Int, RemapTarget>()
        try {
            val json = JSONObject(jsonStr)
            json.keys().forEach { keyStr ->
                val keyCode = keyStr.toInt()
                val targetObj = json.getJSONObject(keyStr)
                val type = targetObj.getString("type")
                val target = when (type) {
                    "key" -> RemapTarget.ToKey(targetObj.getInt("code"), targetObj.getInt("meta"))
                    "text" -> RemapTarget.ToText(targetObj.getString("text"))
                    "disabled" -> RemapTarget.Disabled
                    else -> null
                }
                if (target != null) map[keyCode] = target
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        map
    }
    
    suspend fun saveRemaps(remaps: Map<Int, RemapTarget>) {
        val json = JSONObject()
        remaps.forEach { (keyCode, target) ->
            val targetObj = JSONObject()
            when (target) {
                is RemapTarget.ToKey -> {
                    targetObj.put("type", "key")
                    targetObj.put("code", target.keycode)
                    targetObj.put("meta", target.metaState)
                }
                is RemapTarget.ToText -> {
                    targetObj.put("type", "text")
                    targetObj.put("text", target.text)
                }
                is RemapTarget.Disabled -> {
                    targetObj.put("type", "disabled")
                }
            }
            json.put(keyCode.toString(), targetObj)
        }
        
        context.dataStore.edit { prefs ->
            prefs[KEY_REMAPS] = json.toString()
        }
    }
}
