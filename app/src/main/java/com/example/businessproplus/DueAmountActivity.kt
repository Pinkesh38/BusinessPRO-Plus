package com.example.businessproplus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.businessproplus.databinding.ActivityDueAmountBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DueAmountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDueAmountBinding
    private val db by lazy { AppDatabase.getDatabase(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDueAmountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        loadDueOrders()
    }

    private fun setupRecyclerView() {
        binding.rvDueOrders.layoutManager = LinearLayoutManager(this)
    }

    private fun loadDueOrders() {
        lifecycleScope.launch {
            val dueOrders = withContext(Dispatchers.IO) {
                db.orderDao().getPendingPayments()
            }
            
            if (dueOrders.isEmpty()) {
                binding.tvNoDues.visibility = View.VISIBLE
                binding.rvDueOrders.visibility = View.GONE
            } else {
                binding.tvNoDues.visibility = View.GONE
                binding.rvDueOrders.visibility = View.VISIBLE
                
                binding.rvDueOrders.adapter = DueOrderAdapter(dueOrders, 
                    onRemindClick = { order -> sendWhatsAppReminder(order) },
                    onCollectClick = { order -> 
                        val intent = Intent(this@DueAmountActivity, NewOrderActivity::class.java)
                        intent.putExtra("ORDER_ID", order.id)
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun sendWhatsAppReminder(order: Order) {
        try {
            val phone = order.contactNumber.replace(" ", "")
            val message = "Hello ${order.customerName}, this is a friendly reminder for your pending payment of ₹${order.remainingPayment} for ${order.itemDescription}. Promised Date: ${order.paymentPromiseDate}. Thank you!"
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadDueOrders()
    }
}
