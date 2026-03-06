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
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.businessproplus.databinding.ActivityOrderHistoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrderHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderHistoryBinding
    private lateinit var adapter: OrderHistoryAdapter
    private var searchJob: Job? = null
    private var orderList: List<Order> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOrderHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupRecyclerView()
        setupFilters()
        setupSwipeToActions()

        // 🎯 CUSTOMER FILTER HANDLER: Check if we are viewing a specific person's orders
        val partyName = intent.getStringExtra("PARTY_NAME")
        if (partyName != null) {
            binding.etSearch.setText(partyName)
            binding.toolbar.title = "Orders: $partyName"
        }

        val filterStatus = intent.getStringExtra("FILTER_STATUS")
        if (filterStatus != null) {
            when (filterStatus) {
                "Pending" -> binding.chipGroupFilters.check(R.id.chipPending)
                "Working" -> binding.chipGroupFilters.check(R.id.chipWorking)
                "Completed" -> binding.chipGroupFilters.check(R.id.chipCompleted)
            }
        }

        binding.etSearch.doAfterTextChanged { text ->
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(300)
                loadOrders(text.toString())
            }
        }

        binding.fabAddOrder.setOnClickListener {
            startActivity(Intent(this, NewOrderActivity::class.java))
        }

        loadOrders(binding.etSearch.text.toString())
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = OrderHistoryAdapter(
            this,
            emptyList(),
            onOrderClick = { order ->
                val intent = Intent(this, OrderProcessingActivity::class.java)
                intent.putExtra("ORDER_ID", order.id)
                startActivity(intent)
            },
            onDeleteOrder = { order -> deleteOrder(order) }
        )
        binding.rvOrderHistory.layoutManager = LinearLayoutManager(this)
        binding.rvOrderHistory.adapter = adapter
    }

    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, _ -> loadOrders(binding.etSearch.text.toString()) }
    }

    private fun setupSwipeToActions() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val order = orderList[position]
                if (direction == ItemTouchHelper.LEFT) {
                    deleteOrder(order)
                } else {
                    sendWhatsAppMessage(order.contactNumber, "Hello ${order.customerName}, your order #${order.id} status is: ${order.status}.")
                    adapter.notifyItemChanged(position)
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvOrderHistory)
    }

    private fun sendWhatsAppMessage(number: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://api.whatsapp.com/send?phone=$number&text=" + Uri.encode(message))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadOrders(query: String = "") {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val selectedStatus = when (binding.chipGroupFilters.checkedChipId) {
                R.id.chipPending -> "Pending"
                R.id.chipWorking -> "Working"
                R.id.chipCompleted -> "Completed"
                R.id.chipDelivered -> "Delivered"
                R.id.chipCancelled -> "Cancelled"
                else -> "All"
            }
            // Use exact name query if we are filtering by party, otherwise use LIKE
            val filteredList = db.orderDao().getFilteredOrders(query, "", selectedStatus, "", "", 1, 100, 0)
            withContext(Dispatchers.Main) {
                orderList = filteredList
                adapter.updateList(filteredList)
            }
        }
    }

    private fun deleteOrder(order: Order) {
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(applicationContext).orderDao().delete(order)
            withContext(Dispatchers.Main) { loadOrders(binding.etSearch.text.toString()) }
        }
    }
}