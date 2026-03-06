package com.example.businessproplus

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.businessproplus.databinding.ActivityPartyManagementBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PartyManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPartyManagementBinding
    private lateinit var adapter: PartyAdapter
    
    private var allPartiesList: List<Party> = emptyList()
    private var searchJob: Job? = null
    private var isSortAscending = true // A to Z

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPartyManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupRecyclerView()
        setupFilters()
        loadParties()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_party_management, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort_az -> {
                isSortAscending = true
                filterParties()
                true
            }
            R.id.action_sort_za -> {
                isSortAscending = false
                filterParties()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        adapter = PartyAdapter(
            this,
            emptyList(),
            onEditClick = { party ->
                val intent = Intent(this, AddPartyActivity::class.java)
                intent.putExtra("PARTY_ID", party.id)
                startActivity(intent)
            },
            onViewOrdersClick = { party ->
                val intent = Intent(this, OrderHistoryActivity::class.java)
                intent.putExtra("PARTY_NAME", party.companyName)
                startActivity(intent)
            },
            onDeleteClick = { party ->
                showDeleteConfirmDialog(party)
            }
        )
        binding.rvParties.layoutManager = LinearLayoutManager(this)
        binding.rvParties.adapter = adapter
    }

    private fun showDeleteConfirmDialog(party: Party) {
        AlertDialog.Builder(this)
            .setTitle("Delete Customer")
            .setMessage("Are you sure you want to permanently delete '${party.companyName}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    AppDatabase.getDatabase(applicationContext).partyDao().deleteParty(party)
                    withContext(Dispatchers.Main) {
                        loadParties()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupFilters() {
        binding.cgPartyType.setOnCheckedStateChangeListener { _, _ ->
            filterParties()
        }
        
        binding.btnManageAddNew.setOnClickListener {
            startActivity(Intent(this, AddPartyActivity::class.java))
        }

        binding.etSearchParty.doAfterTextChanged { 
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(300)
                filterParties()
            }
        }
    }

    private fun loadParties() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            allPartiesList = db.partyDao().getAllParties()
            withContext(Dispatchers.Main) {
                filterParties()
            }
        }
    }

    private fun filterParties() {
        val query = binding.etSearchParty.text.toString().lowercase()
        val checkedChipId = binding.cgPartyType.checkedChipId
        
        lifecycleScope.launch(Dispatchers.Default) {
            val filtered = allPartiesList.filter { party ->
                val matchesSearch = party.companyName.lowercase().contains(query) || 
                                   party.contactNo.contains(query)
                val matchesType = when (checkedChipId) {
                    R.id.chipCustomers -> party.partyType == "Customer"
                    R.id.chipSuppliers -> party.partyType == "Supplier"
                    else -> true
                }
                matchesSearch && matchesType
            }
            
            val sortedList = if (isSortAscending) {
                filtered.sortedBy { it.companyName.lowercase() }
            } else {
                filtered.sortedByDescending { it.companyName.lowercase() }
            }
            
            withContext(Dispatchers.Main) {
                adapter.updateList(sortedList)
            }
        }
    }
}