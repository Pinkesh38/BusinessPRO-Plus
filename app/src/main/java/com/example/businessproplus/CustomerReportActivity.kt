package com.example.businessproplus

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomerReportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_report)

        val etSearchCustomerReport = findViewById<EditText>(R.id.etSearchCustomerReport)
        val btnGenerateCustomerReport = findViewById<Button>(R.id.btnGenerateCustomerReport)
        val tvCustomerTotalSpent = findViewById<TextView>(R.id.tvCustomerTotalSpent)
        val lvCustomerOrders = findViewById<ListView>(R.id.lvCustomerOrders)

        btnGenerateCustomerReport.setOnClickListener {
            val customerName = etSearchCustomerReport.text.toString().trim()

            if (customerName.isEmpty()) {
                Toast.makeText(this, "Please enter a name first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Search the database for this specific person
            lifecycleScope.launch(Dispatchers.IO) {
                val database = AppDatabase.getDatabase(applicationContext)
                val orders = database.orderDao().getOrdersForCustomer(customerName)
                val totalSpent = database.orderDao().getTotalRevenueForCustomer(customerName) ?: 0.0

                withContext(Dispatchers.Main) {
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
        }
    }
}