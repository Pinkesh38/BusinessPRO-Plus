package com.example.businessproplus

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.*

class SummaryWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val db = AppDatabase.getDatabase(context)
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        val dailyRevenue = db.orderDao().getDailyRevenue(today) ?: 0.0
        val dailyOrders = db.orderDao().getDailyOrderCount(today)

        val message = "Today\'s Summary: ₹$dailyRevenue in revenue from $dailyOrders orders."
        
        NotificationHelper(context).sendAlert(
            "Daily Business Summary",
            message,
            401
        )

        return Result.success()
    }
}