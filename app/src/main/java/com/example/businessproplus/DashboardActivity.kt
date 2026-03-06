package com.example.businessproplus

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvTotalOrders: TextView
    private lateinit var tvPendingOrders: TextView
    private lateinit var tvStockAlerts: TextView
    private lateinit var tvMissedSales: TextView
    private lateinit var barChart: BarChart
    private lateinit var cardStockAlerts: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        tvTotalOrders = findViewById(R.id.tvTotalOrders)
        tvPendingOrders = findViewById(R.id.tvPendingOrders)
        tvStockAlerts = findViewById(R.id.tvStockAlerts)
        tvMissedSales = findViewById(R.id.tvMissedSales)
        barChart = findViewById(R.id.dashboardBarChart)
        cardStockAlerts = findViewById(R.id.cardStockAlerts)

        findViewById<View>(R.id.fabQuickAction).setOnClickListener {
            startActivity(Intent(this, NewOrderActivity::class.java))
        }

        findViewById<View>(R.id.ivProfileToolbar).setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        loadDashboardData()
    }

    private fun loadDashboardData() {
        // Show subtle loading state (could add a ProgressBar here)
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            
            val totalOrders = db.orderDao().getTotalOrderCount()
            val pendingOrders = db.orderDao().getPendingOrderCount()
            val lowStockCount = db.itemDao().getLowStockItems().size
            
            val calendar = Calendar.getInstance()
            val currentMonthYear = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(calendar.time)
            val missedSales = db.missedItemDao().getMonthlyEstimatedLoss(currentMonthYear) ?: 0.0

            // Load trend data for the last 6 months
            val barEntries = ArrayList<BarEntry>()
            for (i in 5 downTo 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -i)
                val monthLabel = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(cal.time)
                val monthlyRev = db.orderDao().getMonthlyRevenue(monthLabel) ?: 0.0
                barEntries.add(BarEntry((5 - i).toFloat(), monthlyRev.toFloat()))
            }

            withContext(Dispatchers.Main) {
                tvTotalOrders.text = totalOrders.toString()
                tvPendingOrders.text = pendingOrders.toString()
                tvStockAlerts.text = lowStockCount.toString()
                tvMissedSales.text = "₹${missedSales.toInt()}"

                // Highlight low stock in soft red
                if (lowStockCount > 0) {
                    cardStockAlerts.setCardBackgroundColor(getColor(R.color.accent_red_soft))
                }

                setupBarChart(barEntries)
            }
        }
    }

    private fun setupBarChart(entries: List<BarEntry>) {
        val dataSet = BarDataSet(entries, "Monthly Revenue").apply {
            color = Color.parseColor("#1E3A8A")
            valueTextColor = Color.BLACK
            valueTextSize = 10f
        }

        barChart.data = BarData(dataSet)
        barChart.description.isEnabled = false
        barChart.xAxis.isEnabled = false // Minimal look
        barChart.axisLeft.setDrawGridLines(false)
        barChart.animateY(1000)
        barChart.invalidate()
    }
}