package com.example.businessproplus

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uriString = inputData.getString("URI") ?: return@withContext Result.failure()
        val type = inputData.getString("TYPE") ?: "PDF"
        val title = inputData.getString("TITLE") ?: "Report"
        val uri = Uri.parse(uriString)

        val db = AppDatabase.getDatabase(applicationContext)
        val orders = db.orderDao().getAllOrders()

        val success = if (type == "PDF") {
            PdfExporter(applicationContext).exportOrdersToPdf(uri, title, orders)
        } else {
            CsvExporter(applicationContext).exportOrdersToCsv(uri, orders)
        }

        if (success) {
            val notificationHelper = NotificationHelper(applicationContext)
            notificationHelper.sendAlert("Export Complete", "$type report saved successfully.", 3001)
            Result.success(workDataOf("MESSAGE" to "Export successful"))
        } else {
            Result.failure(workDataOf("MESSAGE" to "Export failed"))
        }
    }
}