package com.example.businessproplus

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class BackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Logic is already running on Dispatchers.IO inside BackupManager
            val success = BackupManager(applicationContext).performFullBackup()

            if (success) {
                Result.success()
            } else {
                // 🔄 Retry if it's a transient failure (like network or Drive temporary issues)
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            // Critical failure
            Result.failure()
        }
    }
}