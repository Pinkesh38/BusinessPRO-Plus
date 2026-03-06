package com.example.businessproplus

import android.content.Context
import java.io.File

object SimpleBackup {

    fun backupDatabase(context: Context): Boolean {
        return try {
            // Close database first
            AppDatabase.getDatabase(context).close()

            val dbFile = context.getDatabasePath("business_pro_database")

            if (!dbFile.exists()) {
                return false
            }

            // 🛡️ SECURITY: Using internal storage to prevent backup file exposure
            val backupFile = File(
                context.filesDir,
                "business_backup.db"
            )

            dbFile.copyTo(backupFile, overwrite = true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}