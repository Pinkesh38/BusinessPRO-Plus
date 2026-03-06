package com.example.businessproplus

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailyBackupWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("DailyBackupWorker", "Automatic daily backup starting...")
        
        return try {
            // 🛡️ REPAIR: Using BackupManager instead of LocalBackupManager 
            // to ensure both Local + Google Drive sync happens in the background.
            val backupManager = BackupManager(applicationContext)
            val success = backupManager.performFullBackup()

            if (success) {
                Log.d("DailyBackupWorker", "Automatic backup successful.")
                Result.success()
            } else {
                Log.e("DailyBackupWorker", "Automatic backup failed to complete.")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("DailyBackupWorker", "Critical error during automatic backup", e)
            Result.failure()
        }
    }
}