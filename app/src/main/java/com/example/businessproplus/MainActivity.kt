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
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val db by lazy { AppDatabase.getDatabase(applicationContext) }
    private val logoutHandler = Handler(Looper.getMainLooper())
    private val logoutRunnable = Runnable { logoutUser() }
    private var sessionTimeoutMs = 10 * 60 * 1000L 

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
        loadCachedDashboard()
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                scheduleBackgroundTasks()
                // 🛡️ MEMORY FIX: Use applicationContext instead of 'this'
                UpdateManager(applicationContext).checkForUpdates()
            } catch (e: Exception) {
                Log.e("MainActivity", "Init error", e)
            }
        }
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.btnProfile)?.setOnClickListener { startActivity(Intent(this, UserProfileActivity::class.java)) }
        findViewById<View>(R.id.btnInventory)?.setOnClickListener { startActivity(Intent(this, StockManagementActivity::class.java)) }
        
        val addNewOrderAction = View.OnClickListener { startActivity(Intent(this, NewOrderActivity::class.java)) }
        findViewById<View>(R.id.cardAddNewOrder)?.setOnClickListener(addNewOrderAction)
        findViewById<View>(R.id.fabAddNewOrder)?.setOnClickListener(addNewOrderAction)

        findViewById<View>(R.id.btnDues)?.setOnClickListener { startActivity(Intent(this, DueAmountActivity::class.java)) }
        findViewById<View>(R.id.btnOrderHistory)?.setOnClickListener { startActivity(Intent(this, OrderHistoryActivity::class.java)) }
        findViewById<View>(R.id.btnManageParties)?.setOnClickListener { startActivity(Intent(this, PartyManagementActivity::class.java)) }
        findViewById<View>(R.id.btnTools)?.setOnClickListener { startActivity(Intent(this, OhmsCalculatorActivity::class.java)) }
        findViewById<View>(R.id.btnReports)?.setOnClickListener { startActivity(Intent(this, ReportsDashboardActivity::class.java)) }
        findViewById<ImageButton>(R.id.btnSettings)?.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<View>(R.id.btnLogout)?.setOnClickListener { logoutUser() }

        findViewById<View>(R.id.cardTotalOrders)?.setOnClickListener { startActivity(Intent(this, OrderHistoryActivity::class.java)) }
        findViewById<View>(R.id.cardPendingOrders)?.setOnClickListener {
            val intent = Intent(this, OrderHistoryActivity::class.java)
            intent.putExtra("FILTER_STATUS", "Pending")
            startActivity(intent)
        }
        findViewById<View>(R.id.cardLowStock)?.setOnClickListener { startActivity(Intent(this, StockManagementActivity::class.java)) }
        findViewById<View>(R.id.cardMissedSales)?.setOnClickListener { startActivity(Intent(this, MissedSellingActivity::class.java)) }
    }

    private fun scheduleBackgroundTasks() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val backupRequest = PeriodicWorkRequestBuilder<DailyBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork("DailyBackupWork", ExistingPeriodicWorkPolicy.KEEP, backupRequest)

        val stockCheckRequest = PeriodicWorkRequestBuilder<InventoryCheckWorker>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork("StockCheckWork", ExistingPeriodicWorkPolicy.KEEP, stockCheckRequest)
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

    private fun loadCachedDashboard() {
        val prefs = getSharedPreferences("DashboardCache", MODE_PRIVATE)
        findViewById<TextView>(R.id.tvTotalOrders)?.text = prefs.getString("total_orders", "0")
        findViewById<TextView>(R.id.tvPendingOrders)?.text = prefs.getString("pending_orders", "0")
        findViewById<TextView>(R.id.tvStockAlerts)?.text = prefs.getString("low_stock", "0")
        findViewById<TextView>(R.id.tvMissedSales)?.text = prefs.getString("missed_sales", "₹0")
    }

    private fun refreshDashboardTotals() {
        lifecycleScope.launch {
            val results = withContext(Dispatchers.IO) {
                try {
                    if (isFinishing || isDestroyed) return@withContext null

                    val calendar = Calendar.getInstance()
                    val currentMonthYear = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(calendar.time)

                    val totalOrdersDeferred = async { db.orderDao().getTotalOrderCount().toString() }
                    val pendingOrdersDeferred = async { db.orderDao().getPendingOrderCount().toString() }
                    val lowStockDeferred = async { db.itemDao().getLowStockItems().size.toString() }
                    val missedSalesDeferred = async { db.missedItemDao().getMonthlyEstimatedLoss(currentMonthYear) ?: 0.0 }
                    
                    val totalOrders = totalOrdersDeferred.await()
                    val pendingOrders = pendingOrdersDeferred.await()
                    val lowStockCount = lowStockDeferred.await()
                    val lossValue = missedSalesDeferred.await()
                    val missedSales = String.format("₹%.0f", lossValue)
                    
                    getSharedPreferences("DashboardCache", MODE_PRIVATE).edit()
                        .putString("total_orders", totalOrders)
                        .putString("pending_orders", pendingOrders)
                        .putString("low_stock", lowStockCount)
                        .putString("missed_sales", missedSales)
                        .apply()
                    
                    val userPrefs = getSharedPreferences("BusinessProPrefs", MODE_PRIVATE)
                    val userName = userPrefs.getString("USER_NAME", "Business Owner")
                    val lastBackup = BackupPrefs(applicationContext).getLastBackupTime()
                    
                    DashboardData(totalOrders, pendingOrders, lowStockCount, missedSales, userName ?: "User", lastBackup)
                } catch (e: Exception) { 
                    null 
                }
            }

            results?.let { data ->
                if (!isFinishing && !isDestroyed) {
                    findViewById<TextView>(R.id.tvTotalOrders)?.text = data.totalOrders
                    findViewById<TextView>(R.id.tvPendingOrders)?.text = data.pendingOrders
                    findViewById<TextView>(R.id.tvStockAlerts)?.text = data.lowStock
                    findViewById<TextView>(R.id.tvMissedSales)?.text = data.missedSales
                    findViewById<TextView>(R.id.tvDashboardTitle)?.text = data.userName
                    findViewById<TextView>(R.id.tvLastBackup)?.text = "Last Backup: ${data.lastBackup}"
                }
            }
        }
    }

    private data class DashboardData(
        val totalOrders: String, val pendingOrders: String, val lowStock: String,
        val missedSales: String, val userName: String, val lastBackup: String
    )

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
        if (isFinishing || isDestroyed) return
        getSharedPreferences("BusinessProPrefs", MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        // 🛡️ MEMORY FIX: Remove callbacks to prevent leaking Activity context
        logoutHandler.removeCallbacks(logoutRunnable)
        super.onDestroy()
    }
}