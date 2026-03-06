package com.example.businessproplus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val db by lazy { AppDatabase.getDatabase(applicationContext) }
    private val logoutHandler = Handler(Looper.getMainLooper())
    private val logoutRunnable = Runnable { logoutUser() }
    private var sessionTimeoutMs = 10 * 60 * 1000L // Default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.main_root_container)
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        setupClickListeners()
        
        // 🛡️ SAFE START: Delay background work scheduling to avoid startup freezes
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                scheduleDailyBackup()
                // 🛡️ OTA UPDATE CHECK: Check for updates on every app launch
                UpdateManager(this@MainActivity).checkForUpdates()
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to init background tasks", e)
            }
        }
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.btnProfile)?.setOnClickListener { startActivity(Intent(this, UserProfileActivity::class.java)) }
        findViewById<View>(R.id.btnInventory)?.setOnClickListener { startActivity(Intent(this, StockManagementActivity::class.java)) }
        
        val addNewOrderAction = View.OnClickListener { startActivity(Intent(this, NewOrderActivity::class.java)) }
        findViewById<View>(R.id.cardAddNewOrder)?.setOnClickListener(addNewOrderAction)
        findViewById<View>(R.id.fabAddNewOrder)?.setOnClickListener(addNewOrderAction)

        findViewById<View>(R.id.btnOrderHistory)?.setOnClickListener { startActivity(Intent(this, OrderHistoryActivity::class.java)) }
        findViewById<View>(R.id.btnManageParties)?.setOnClickListener { startActivity(Intent(this, PartyManagementActivity::class.java)) }
        findViewById<View>(R.id.btnTools)?.setOnClickListener { startActivity(Intent(this, OhmsCalculatorActivity::class.java)) }
        findViewById<View>(R.id.btnReports)?.setOnClickListener { startActivity(Intent(this, ReportsDashboardActivity::class.java)) }
        findViewById<ImageButton>(R.id.btnSettings)?.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<View>(R.id.btnLogout)?.setOnClickListener { logoutUser() }

        findViewById<View>(R.id.cardTotalOrders)?.setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }
        
        findViewById<View>(R.id.cardPendingOrders)?.setOnClickListener {
            val intent = Intent(this, OrderHistoryActivity::class.java)
            intent.putExtra("FILTER_STATUS", "Pending")
            startActivity(intent)
        }
        
        findViewById<View>(R.id.cardLowStock)?.setOnClickListener {
            startActivity(Intent(this, StockManagementActivity::class.java))
        }
        
        findViewById<View>(R.id.cardMissedSales)?.setOnClickListener {
            startActivity(Intent(this, MissedSellingActivity::class.java))
        }
    }

    private fun scheduleDailyBackup() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val backupRequest = PeriodicWorkRequestBuilder<DailyBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DailyBackupWork",
            ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val timeoutSetting = prefs.getString("LOGOUT_TIMEOUT", "10 Minutes")
        
        sessionTimeoutMs = when (timeoutSetting) {
            "5 Minutes" -> 5 * 60 * 1000L
            "10 Minutes" -> 10 * 60 * 1000L
            "30 Minutes" -> 30 * 60 * 1000L
            "Never" -> Long.MAX_VALUE
            else -> 10 * 60 * 1000L
        }

        refreshDashboardTotals()
        resetLogoutTimer()
    }

    private fun refreshDashboardTotals() {
        lifecycleScope.launch {
            val results = withContext(Dispatchers.IO) {
                try {
                    val totalOrders = db.orderDao().getTotalOrderCount()
                    val pendingOrders = db.orderDao().getPendingOrderCount()
                    val lowStockCount = db.itemDao().getLowStockItems().size
                    
                    val calendar = Calendar.getInstance()
                    val currentMonthYear = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(calendar.time)
                    val missedSales = db.missedItemDao().getMonthlyEstimatedLoss(currentMonthYear) ?: 0.0
                    
                    val userPrefs = getSharedPreferences("BusinessProPrefs", MODE_PRIVATE)
                    val userName = userPrefs.getString("USER_NAME", "Business Owner")
                    val lastBackup = BackupPrefs(applicationContext).getLastBackupTime()
                    
                    Triple(Triple(totalOrders, pendingOrders, lowStockCount), missedSales, Pair(userName, lastBackup))
                } catch (e: Exception) {
                    null
                }
            }

            results?.let {
                findViewById<TextView>(R.id.tvTotalOrders)?.text = it.first.first.toString()
                findViewById<TextView>(R.id.tvPendingOrders)?.text = it.first.second.toString()
                findViewById<TextView>(R.id.tvStockAlerts)?.text = it.first.third.toString()
                findViewById<TextView>(R.id.tvMissedSales)?.text = String.format("₹%.0f", it.second)
                findViewById<TextView>(R.id.tvDashboardTitle)?.text = it.third.first
                findViewById<TextView>(R.id.tvLastBackup)?.text = "Last Backup: ${it.third.second}"
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetLogoutTimer()
    }

    private fun resetLogoutTimer() {
        logoutHandler.removeCallbacks(logoutRunnable)
        if (sessionTimeoutMs != Long.MAX_VALUE) {
            logoutHandler.postDelayed(logoutRunnable, sessionTimeoutMs)
        }
    }

    private fun logoutUser() {
        getSharedPreferences("BusinessProPrefs", MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}