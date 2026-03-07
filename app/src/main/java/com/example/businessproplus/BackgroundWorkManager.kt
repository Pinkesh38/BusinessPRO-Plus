package com.example.businessproplus

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class BackgroundWorkManager(private val context: Context) {

    fun scheduleAllTasks() {
        scheduleDailyBackup()
        scheduleInventoryCheck()
    }

    private fun scheduleDailyBackup() {
        // 🛡️ BATTERY FIX: Only backup when connected to Wi-Fi and CHARGING
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Wi-Fi only to save data/battery
            .setRequiresCharging(true) // 🔋 Save battery for heavy encryption/upload
            .setRequiresBatteryNotLow(true)
            .build()

        val backupRequest = PeriodicWorkRequestBuilder<DailyBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DailyBackupWork",
            ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )
    }

    private fun scheduleInventoryCheck() {
        // Run every 12 hours with basic battery safety
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val stockCheckRequest = PeriodicWorkRequestBuilder<InventoryCheckWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "StockCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            stockCheckRequest
        )
    }
}