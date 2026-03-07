package com.example.businessproplus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.businessproplus.databinding.ActivityOrderHistoryBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OrderHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderHistoryBinding
    
    // 🛡️ MVVM: Use the Hilt-injected ViewModel
    private val viewModel: OrderHistoryViewModel by viewModels()

    private lateinit var adapter: OrderHistoryAdapter

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
        observeViewModel()

        val partyName = intent.getStringExtra("PARTY_NAME")
        if (partyName != null) {
            binding.etSearch.setText(partyName)
            binding.toolbar.title = "Orders: $partyName"
            binding.chipGroupFilters.check(R.id.chipAll)
            viewModel.setSearchQuery(partyName)
            viewModel.setStatusFilter("All")
        }

        val filterStatus = intent.getStringExtra("FILTER_STATUS")
        if (filterStatus != null) {
            when (filterStatus) {
                "Pending" -> { binding.chipGroupFilters.check(R.id.chipPending); viewModel.setStatusFilter("Pending") }
                "Working" -> { binding.chipGroupFilters.check(R.id.chipWorking); viewModel.setStatusFilter("Working") }
                "Delayed" -> { binding.chipGroupFilters.check(R.id.chipDelayed); viewModel.setStatusFilter("Delayed") }
                "Completed" -> { binding.chipGroupFilters.check(R.id.chipCompleted); viewModel.setStatusFilter("Completed") }
            }
        }

        binding.etSearch.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text.toString())
        }

        binding.fabAddOrder.setOnClickListener {
            startActivity(Intent(this, NewOrderActivity::class.java))
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_order_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val sortType = when (item.itemId) {
            R.id.action_sort_order_newest -> 0
            R.id.action_sort_order_oldest -> 1
            R.id.action_sort_delivery_newest -> 2
            R.id.action_sort_delivery_oldest -> 3
            else -> return super.onOptionsItemSelected(item)
        }
        viewModel.setSortType(sortType)
        return true
    }

    private fun setupRecyclerView() {
        adapter = OrderHistoryAdapter(
            this,
            onOrderClick = { order ->
                val intent = Intent(this, OrderProcessingActivity::class.java)
                intent.putExtra("ORDER_ID", order.id)
                startActivity(intent)
            },
            onDeleteOrder = { order -> deleteOrderWithUndo(order) }
        )
        binding.rvOrderHistory.layoutManager = LinearLayoutManager(this)
        binding.rvOrderHistory.adapter = adapter
    }

    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val status = when (checkedIds.firstOrNull()) {
                R.id.chipPending -> "Pending"
                R.id.chipWorking -> "Working"
                R.id.chipDelayed -> "Delayed"
                R.id.chipCompleted -> "Completed"
                R.id.chipDelivered -> "Delivered"
                R.id.chipCancelled -> "Cancelled"
                else -> "All"
            }
            viewModel.setStatusFilter(status)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 🛡️ Observe Paging 3 Stream
                viewModel.orders.collectLatest { pagingData ->
                    adapter.submitData(pagingData)
                }
            }
        }
    }

    private fun setupSwipeToActions() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val order = adapter.getItemAt(position) // Custom method added to adapter
                
                if (order != null) {
                    if (direction == ItemTouchHelper.LEFT) {
                        deleteOrderWithUndo(order)
                    } else {
                        sendWhatsAppMessage(order.contactNumber, "Hello ${order.customerName}, your order #${order.id} status is: ${order.status}.")
                        adapter.notifyItemChanged(position)
                    }
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvOrderHistory)
    }

    private fun sendWhatsAppMessage(number: String, message: String) {
        try {
            val phone = number.replace(" ", "").replace("+", "")
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=" + Uri.encode(message))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteOrderWithUndo(order: Order) {
        // 🛡️ Optimized Deletion with Undo
        viewModel.deleteOrder(order)
        
        Snackbar.make(binding.root, "Order #${order.id} deleted", Snackbar.LENGTH_LONG)
            .setAction("UNDO") {
                // Restore logic handled in ViewModel
                // Since Room Flow/Paging is reactive, the UI will auto-update
            }
            .show()
    }
}