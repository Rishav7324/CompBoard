package com.example.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class BackupRestoreManager(private val context: Context) {

    suspend fun createBackup(): Uri? = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val macros = db.macroDao().getAllMacros().first()
            val rules = db.appLayoutRuleDao().getAllRules().first()
            
            val root = JSONObject()
            
            val macroArray = JSONArray()
            for (m in macros) {
                val obj = JSONObject()
                obj.put("name", m.name)
                obj.put("description", m.description)
                obj.put("steps", m.steps)
                macroArray.put(obj)
            }
            root.put("macros", macroArray)
            
            val rulesArray = JSONArray()
            for (r in rules) {
                val obj = JSONObject()
                obj.put("packageName", r.packageName)
                obj.put("layoutName", r.layoutName)
                rulesArray.put(obj)
            }
            root.put("rules", rulesArray)
            
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFile = File(context.cacheDir, "compboard_backup_$dateStr.json")
            
            FileOutputStream(backupFile).use { fos ->
                fos.write(root.toString(2).toByteArray())
            }
            
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
