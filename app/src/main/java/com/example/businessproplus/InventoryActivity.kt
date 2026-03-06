package com.example.businessproplus

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InventoryActivity : AppCompatActivity() {

    private lateinit var rvInventory: RecyclerView
    private lateinit var etSearch: TextInputEditText
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var adapter: InventoryAdapter
    
    private var searchJob: Job? = null
    private var itemList: List<Item> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inventory)

        setupToolbar()
        initViews()
        setupRecyclerView()
        loadCategories()
        loadInventory()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initViews() {
        rvInventory = findViewById(R.id.rvInventory)
        etSearch = findViewById(R.id.etSearch)
        chipGroupCategories = findViewById(R.id.chipGroupCategories)
        
        findViewById<View>(R.id.fabAddItem).setOnClickListener {
            startActivity(Intent(this, AddItemActivity::class.java))
        }

        etSearch.doAfterTextChanged { text ->
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(300)
                loadInventory(text.toString())
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = InventoryAdapter(
            context = this,
            items = emptyList(),
            onItemClick = { item ->
                val intent = Intent(this, AddItemActivity::class.java)
                intent.putExtra("ITEM_ID", item.id)
                startActivity(intent)
            },
            onDeleteClick = { item ->
                showDeleteConfirmDialog(item)
            }
        )
        rvInventory.layoutManager = LinearLayoutManager(this)
        rvInventory.adapter = adapter
    }

    private fun showDeleteConfirmDialog(item: Item) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete '${item.itemName}'? This will also remove it from stock MASTER.")
            .setPositiveButton("Delete") { _, _ ->
                deleteItem(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItem(item: Item) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            db.itemDao().deleteItem(item)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@InventoryActivity, "Item deleted from Stock Master", Toast.LENGTH_SHORT).show()
                loadInventory(etSearch.text.toString())
            }
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val categories = db.itemDao().getAllItems().map { it.category }.distinct()
            
            withContext(Dispatchers.Main) {
                categories.forEach { category ->
                    val chip = Chip(ContextThemeWrapper(this@InventoryActivity, com.google.android.material.R.style.Widget_Material3_Chip_Filter)).apply {
                        text = category
                        isCheckable = true
                        setOnCheckedChangeListener { _, _ -> loadInventory(etSearch.text.toString()) }
                    }
                    chipGroupCategories.addView(chip)
                }
            }
        }
    }

    private fun loadInventory(query: String = "") {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            var filteredList = db.itemDao().getAllItems().filter { 
                it.itemName.contains(query, ignoreCase = true)
            }

            val checkedChipId = chipGroupCategories.checkedChipId
            if (checkedChipId != View.NO_ID && checkedChipId != R.id.chipAll) {
                val selectedCategory = findViewById<Chip>(checkedChipId).text.toString()
                filteredList = filteredList.filter { it.category == selectedCategory }
            }
            
            withContext(Dispatchers.Main) {
                itemList = filteredList
                adapter.updateList(filteredList)
                rvInventory.scheduleLayoutAnimation()
            }
        }
    }
}