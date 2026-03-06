package com.example.businessproplus

import android.app.Application
import android.util.Log
import androidx.work.*
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class BusinessProApp : Application(), Configuration.Provider {

    // 🛡️ Custom WorkManager configuration for better stability
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // 1. IMPROVED CRASH HANDLER: Don't block the UI thread during logging
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log crash in a simple way
            try {
                val logFile = File(filesDir, "production_error_log.txt")
                logFile.appendText("\n--- CRASH: ${throwable.message} ---\n")
            } catch (e: Exception) {}
            
            defaultHandler?.uncaughtException(thread, throwable)
        }
        
        // Note: scheduleDailyBackup() moved to MainActivity to prevent startup freezes
    }
}