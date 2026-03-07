package com.example.businessproplus

import android.app.Application
import android.util.Log
import androidx.work.*
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class BusinessProApp : Application(), Configuration.Provider {

    // 🛡️ Optimized Coroutine Scope for Startup Tasks
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        setupCrashHandler()
        
        // 🛡️ Pre-warm components in background to reduce UI-thread blocking
        applicationScope.launch {
            preWarmDatabase()
        }
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Note: We only write to disk here during a fatal event, 
            // so it doesn't impact normal startup time.
            try {
                val logFile = File(filesDir, "production_error_log.txt")
                logFile.appendText("\n--- CRASH: ${throwable.message} ---\n")
            } catch (e: Exception) {}
            
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun preWarmDatabase() {
        // Trigger a simple database call to initialize the Room instance early.
        // This ensures the dashboard loads instantly when MainActivity starts.
        try {
            val db = AppDatabase.getDatabase(this)
            db.openHelper.readableDatabase
            Log.d("BusinessProApp", "Database pre-warmed successfully")
        } catch (e: Exception) {
            Log.e("BusinessProApp", "Database pre-warm failed", e)
        }
    }
}