package com.example.businessproplus

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.businessproplus.databinding.ActivityStockManagementBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StockManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStockManagementBinding
    private val db by lazy { AppDatabase.getDatabase(applicationContext) }
    private lateinit var adapter: StockAdapter
    private var allItemsList: List<Item> = emptyList()
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityStockManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.stockRootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        setupRecyclerView()
        setupListeners()
        loadItems()
    }

    private fun setupRecyclerView() {
        adapter = StockAdapter(
            onItemClick = { item ->
                val intent = Intent(this, AddItemActivity::class.java)
                intent.putExtra("ITEM_ID", item.id)
                startActivity(intent)
            },
            onDeleteClick = { item ->
                showDeleteConfirmation(item)
            }
        )
        binding.rvStockItems.layoutManager = LinearLayoutManager(this)
        binding.rvStockItems.adapter = adapter
    }

    private fun showDeleteConfirmation(item: Item) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete '${item.itemName}'? This will hide it from active inventory.")
            .setPositiveButton("Delete") { _, _ ->
                deleteItem(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItem(item: Item) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                // We use soft delete to maintain order history integrity
                item.isDeleted = true
                db.itemDao().updateItem(item)
            }
            Toast.makeText(this@StockManagementActivity, "Item deleted", Toast.LENGTH_SHORT).show()
            loadItems()
        }
    }

    private fun setupListeners() {
        binding.btnManageCategories.setOnClickListener {
            startActivity(Intent(this, CategoryManagementActivity::class.java))
        }

        binding.btnStockAddNew.setOnClickListener {
            startActivity(Intent(this, AddItemActivity::class.java))
        }

        binding.etSearchItem.doAfterTextChanged { text ->
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(300)
                filterItems(text.toString())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadItems()
    }

    private fun loadItems() {
        lifecycleScope.launch(Dispatchers.IO) {
            allItemsList = db.itemDao().getAllItems()
            withContext(Dispatchers.Main) {
                filterItems(binding.etSearchItem.text.toString())
            }
        }
    }

    private fun filterItems(query: String) {
        val filtered = allItemsList.filter { 
            it.itemName.contains(query, ignoreCase = true) || it.itemDescription.contains(query, ignoreCase = true)
        }
        adapter.submitList(filtered)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
