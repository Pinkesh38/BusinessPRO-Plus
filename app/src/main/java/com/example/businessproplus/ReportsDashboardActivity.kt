package com.example.businessproplus

import android.app.DatePickerDialog
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.businessproplus.databinding.ActivityReportsDashboardBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ReportsDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsDashboardBinding
    private val db by lazy { AppDatabase.getDatabase(applicationContext) }
    
    private var startDate: Calendar = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
    private var endDate: Calendar = Calendar.getInstance()
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val createPdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { exportToPdf(it) }
    }

    private val createCsvLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { exportToCsv(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityReportsDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.reportsRootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupFilters()
        setupExportButtons()
        loadData()
    }

    private fun setupFilters() {
        binding.btnStartDate.text = sdf.format(startDate.time)
        binding.btnEndDate.text = sdf.format(endDate.time)

        binding.btnStartDate.setOnClickListener {
            showDatePicker(startDate) {
                binding.btnStartDate.text = sdf.format(startDate.time)
                loadData()
            }
        }

        binding.btnEndDate.setOnClickListener {
            showDatePicker(endDate) {
                binding.btnEndDate.text = sdf.format(endDate.time)
                loadData()
            }
        }

        val reportTypes = arrayOf("Sales Analysis", "Customer Activity", "Revenue Trends")
        binding.spinnerReportType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, reportTypes)
        binding.spinnerReportType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) { loadData() }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun setupExportButtons() {
        binding.btnExportPdf.setOnClickListener {
            val fileName = "BusinessPRO_Full_Report_${System.currentTimeMillis()}.pdf"
            createPdfLauncher.launch(fileName)
        }

        binding.btnExportCsv.setOnClickListener {
            val fileName = "BusinessPRO_Sales_Export_${System.currentTimeMillis()}.csv"
            createCsvLauncher.launch(fileName)
        }
    }

    private fun exportToPdf(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val allOrders = db.orderDao().getAllOrders()
            val success = PdfExporter(this@ReportsDashboardActivity)
                .exportOrdersToPdf(uri, "BusinessPRO+ Master Order Report", allOrders)
            
            withContext(Dispatchers.Main) {
                val msg = if (success) "PDF Exported Successfully!" else "PDF Export Failed"
                Toast.makeText(this@ReportsDashboardActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportToCsv(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val allOrders = db.orderDao().getAllOrders()
            val success = CsvExporter(this@ReportsDashboardActivity)
                .exportOrdersToCsv(uri, allOrders)
            
            withContext(Dispatchers.Main) {
                val msg = if (success) "CSV Exported Successfully!" else "CSV Export Failed"
                Toast.makeText(this@ReportsDashboardActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker(calendar: Calendar, onDateSet: () -> Unit) {
        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(year, month, day)
            onDateSet()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val startStr = sdf.format(startDate.time)
            val endStr = sdf.format(endDate.time)

            val revenue = db.orderDao().getRevenueForRange(startStr, endStr) ?: 0.0
            val orderCount = db.orderDao().getOrderCountForRange(startStr, endStr)
            val totalPending = db.orderDao().getTotalToCollect() ?: 0.0

            val prevStart = Calendar.getInstance().apply { time = startDate.time; add(Calendar.MONTH, -1) }
            val prevEnd = Calendar.getInstance().apply { time = endDate.time; add(Calendar.MONTH, -1) }
            val prevRevenue = db.orderDao().getRevenueForRange(sdf.format(prevStart.time), sdf.format(prevEnd.time)) ?: 0.0
            val growth = if (prevRevenue > 0) ((revenue - prevRevenue) / prevRevenue) * 100 else 0.0

            val topItems = db.orderDao().getTopSellingItems(5)
            val trends = db.orderDao().getDailyRevenueTrend(startStr)

            withContext(Dispatchers.Main) {
                binding.tvReportRevenue.text = String.format("₹%.2f", revenue)
                binding.tvReportToCollect.text = String.format("₹%.2f", totalPending)
                binding.tvReportOrders.text = orderCount.toString()
                binding.tvGrowthPercent.text = String.format("%s%.1f%%", if (growth >= 0) "+" else "", growth)
                binding.tvGrowthPercent.setTextColor(if (growth >= 0) Color.parseColor("#2E7D32") else Color.RED)

                updateCharts(topItems, trends)
            }
        }
    }

    private fun updateCharts(topItems: List<ReportItem>, trends: List<TrendItem>) {
        val pieEntries = topItems.map { PieEntry(it.count.toFloat(), it.name) }
        val pieDataSet = PieDataSet(pieEntries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }
        binding.pieChartProducts.data = PieData(pieDataSet)
        binding.pieChartProducts.invalidate()

        val barEntries = trends.mapIndexed { index, trend -> BarEntry(index.toFloat(), trend.dailyTotal.toFloat()) }
        val barDataSet = BarDataSet(barEntries, "Daily Revenue").apply {
            color = Color.parseColor("#1E3A8A")
        }
        binding.barChartRevenue.data = BarData(barDataSet)
        binding.barChartRevenue.invalidate()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
