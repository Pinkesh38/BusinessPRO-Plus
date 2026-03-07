package com.example.businessproplus

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class InventoryCheckWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val lowStockItems = db.itemDao().getLowStockItems()

        if (lowStockItems.isNotEmpty()) {
            val notificationHelper = NotificationHelper(applicationContext)
            val message = "Alert: ${lowStockItems.size} items are below minimum stock level!"
            notificationHelper.sendAlert("Low Stock Alert", message, 2001)
        }

        return Result.success()
    }
}