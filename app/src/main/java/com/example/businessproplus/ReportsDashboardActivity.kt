package com.example.businessproplus

import android.app.DatePickerDialog
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.businessproplus.databinding.ActivityReportsDashboardBinding
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ReportsDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsDashboardBinding
    private val db by lazy { AppDatabase.getDatabase(applicationContext) }
    
    private val viewModel: ReportsDashboardViewModel by viewModels()
    
    private var startDate: Calendar = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
    private var endDate: Calendar = Calendar.getInstance()
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val createPdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { exportToPdf(it) }
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
        observeViewModel()
        triggerLoad()
    }

    private fun setupFilters() {
        binding.btnStartDate.text = sdf.format(startDate.time)
        binding.btnEndDate.text = sdf.format(endDate.time)

        binding.btnStartDate.setOnClickListener {
            showDatePicker(startDate, isStartDate = true) {
                if (startDate.after(endDate)) {
                    endDate.time = startDate.time
                    binding.btnEndDate.text = sdf.format(endDate.time)
                }
                binding.btnStartDate.text = sdf.format(startDate.time)
                triggerLoad()
            }
        }

        binding.btnEndDate.setOnClickListener {
            showDatePicker(endDate, isStartDate = false) {
                binding.btnEndDate.text = sdf.format(endDate.time)
                triggerLoad()
            }
        }

        val reportTypes = arrayOf("Sales Analysis", "Customer Activity", "Revenue Trends")
        binding.spinnerReportType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, reportTypes)
        binding.spinnerReportType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) { triggerLoad() }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (isFinishing || isDestroyed) return@collect
                    
                    when (state) {
                        is ReportUiState.Loading -> {
                            binding.progressBarReports.visibility = View.VISIBLE
                        }
                        is ReportUiState.Success -> {
                            binding.progressBarReports.visibility = View.GONE
                            updateUI(state.data)
                        }
                        is ReportUiState.Error -> {
                            binding.progressBarReports.visibility = View.GONE
                            Toast.makeText(applicationContext, "Data Error: ${state.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun triggerLoad() {
        val startStr = sdf.format(startDate.time)
        val endStr = sdf.format(endDate.time)
        val reportType = binding.spinnerReportType.selectedItem?.toString() ?: "Sales Analysis"
        
        val prevStart = Calendar.getInstance().apply { time = startDate.time; add(Calendar.MONTH, -1) }
        val prevEnd = Calendar.getInstance().apply { time = endDate.time; add(Calendar.MONTH, -1) }
        
        viewModel.loadData(
            reportType, startStr, endStr,
            sdf.format(prevStart.time), sdf.format(prevEnd.time)
        )
    }

    private fun updateUI(data: ReportData) {
        // Updated to use modular KPI cards via binding
        binding.cardRevenue.tvLabel.text = "REVENUE"
        binding.cardRevenue.tvValue.text = String.format("₹%.2f", data.revenue)
        
        binding.cardPending.tvLabel.text = "PENDING"
        binding.cardPending.tvValue.text = String.format("₹%.2f", data.totalPending)
        
        binding.cardOrders.tvLabel.text = "ORDERS"
        binding.cardOrders.tvValue.text = data.orderCount.toString()
        
        binding.cardGrowth.tvLabel.text = "GROWTH"
        binding.cardGrowth.tvValue.text = String.format("%s%.1f%%", if (data.growth >= 0) "+" else "", data.growth)

        updateCharts(data.topItems, data.trends)
    }

    private fun updateCharts(topItems: List<ReportItem>, trends: List<TrendItem>) {
        if (topItems.isEmpty()) {
            binding.pieChartProducts.clear()
            binding.pieChartProducts.setNoDataText("No Sales Data Found")
        } else {
            val pieEntries = topItems.map { PieEntry(it.count.toFloat(), it.name) }
            val pieDataSet = PieDataSet(pieEntries, "").apply {
                colors = ColorTemplate.MATERIAL_COLORS.toList()
                valueTextColor = Color.WHITE
                valueTextSize = 12f
            }
            binding.pieChartProducts.data = PieData(pieDataSet)
            binding.pieChartProducts.animateY(800)
        }
        binding.pieChartProducts.invalidate()

        if (trends.isEmpty()) {
            binding.barChartRevenue.clear()
        } else {
            val barEntries = trends.mapIndexed { index, trend -> BarEntry(index.toFloat(), trend.dailyTotal.toFloat()) }
            val barDataSet = BarDataSet(barEntries, "Daily Revenue").apply {
                color = Color.parseColor("#1E3A8A")
            }
            binding.barChartRevenue.data = BarData(barDataSet)
            binding.barChartRevenue.animateY(800)
        }
        binding.barChartRevenue.invalidate()
    }

    private fun setupExportButtons() {
        binding.btnExportPdf.setOnClickListener {
            if (binding.btnExportPdf.isEnabled) {
                binding.btnExportPdf.isEnabled = false
                createPdfLauncher.launch("BusinessPRO_Report_${System.currentTimeMillis()}.pdf")
            }
        }
    }

    private fun exportToPdf(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val allOrders = db.orderDao().getAllOrders()
                val success = PdfExporter(applicationContext).exportOrdersToPdf(uri, "Business Report", allOrders)
                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) {
                        binding.btnExportPdf.isEnabled = true
                        Toast.makeText(applicationContext, if (success) "Report Saved!" else "Export Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) {
                        binding.btnExportPdf.isEnabled = true
                    }
                }
            }
        }
    }

    private fun showDatePicker(calendar: Calendar, isStartDate: Boolean, onDateSet: () -> Unit) {
        if (isFinishing) return
        val picker = DatePickerDialog(this, { _, year, month, day ->
            calendar.set(year, month, day)
            onDateSet()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        
        if (!isStartDate) {
            picker.datePicker.minDate = startDate.timeInMillis
        }
        picker.datePicker.maxDate = System.currentTimeMillis()
        picker.show()
    }
}
