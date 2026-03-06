package com.example.businessproplus

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

class CustomerReportActivity : AppCompatActivity() {
    
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_report)

        val etSearchCustomerReport = findViewById<EditText>(R.id.etSearchCustomerReport)
        val btnGenerateCustomerReport = findViewById<Button>(R.id.btnGenerateCustomerReport)
        val tvCustomerTotalSpent = findViewById<TextView>(R.id.tvCustomerTotalSpent)
        val lvCustomerOrders = findViewById<ListView>(R.id.lvCustomerOrders)
        val progressBar = findViewById<ProgressBar>(R.id.progressBarCustomerReport)

        btnGenerateCustomerReport.setOnClickListener {
            val customerName = etSearchCustomerReport.text.toString().trim()

            if (customerName.isEmpty()) {
                Toast.makeText(this, "Please enter a name first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🛡️ QA FIX: Prevent "Search Spam" and UI Hangs
            searchJob?.cancel() // Cancel any existing search
            btnGenerateCustomerReport.isEnabled = false
            progressBar?.visibility = View.VISIBLE

            searchJob = lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val database = AppDatabase.getDatabase(applicationContext)
                    val orders = database.orderDao().getOrdersForCustomer(customerName)
                    val totalSpent = database.orderDao().getTotalRevenueForCustomer(customerName) ?: 0.0

                    withContext(Dispatchers.Main) {
                        if (!isFinishing && !isDestroyed) {
                            if (orders.isEmpty()) {
                                Toast.makeText(this@CustomerReportActivity, "No records found for $customerName", Toast.LENGTH_SHORT).show()
                                tvCustomerTotalSpent.text = "₹0.00"
                                lvCustomerOrders.adapter = null
                            } else {
                                tvCustomerTotalSpent.text = String.format("₹%.2f", totalSpent)
                                val displayList = orders.map { "Date: ${it.orderDate}\nItem: ${it.itemDescription} | ₹${it.total}" }
                                lvCustomerOrders.adapter = ArrayAdapter(this@CustomerReportActivity, android.R.layout.simple_list_item_1, displayList)
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CustomerReportActivity, "Search error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        btnGenerateCustomerReport.isEnabled = true
                        progressBar?.visibility = View.GONE
                    }
                }
            }
        }
    }
}