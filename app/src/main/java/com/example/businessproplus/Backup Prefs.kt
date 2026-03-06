package com.example.businessproplus

import android.content.Context

class BackupPrefs(context: Context) {

    // 🛡️ PERSISTENCE FIX: Using "AppPrefs" instead of "BusinessProPrefs" 
    // to prevent backup time from being wiped during user logout.
    private val prefs = context.applicationContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_BACKUP = "last_backup_time"
    }

    fun saveLastBackupTime(time: String) {
        prefs.edit().putString(KEY_LAST_BACKUP, time).apply()
    }

    fun getLastBackupTime(): String {
        // Safe default: "Never"
        return prefs.getString(KEY_LAST_BACKUP, "Never") ?: "Never"
    }
}
