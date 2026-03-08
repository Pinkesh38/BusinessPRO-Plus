package com.example.businessproplus

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
    private var isSortAscending = true 

    private val createPdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { exportAllPartiesToPdf(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPartyManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.partyRootLayout) { v, insets ->
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
            R.id.action_export_all -> {
                createPdfLauncher.launch("BusinessPRO_All_Customers_${System.currentTimeMillis()}.pdf")
                true
            }
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
            onExportPdfClick = { party ->
                // Individual Export handled inside adapter/activity launcher
                exportIndividualParty(party)
            },
            onDeleteClick = { party ->
                showDeleteConfirmDialog(party)
            }
        )
        binding.rvParties.layoutManager = LinearLayoutManager(this)
        binding.rvParties.adapter = adapter
    }

    private fun exportIndividualParty(party: Party) {
        val launcher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
            uri?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val success = PdfExporter(applicationContext).exportCustomersWithHistoryToPdf(
                        it, "Customer Report: ${party.companyName}", listOf(party), db
                    )
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, if (success) "Report Exported!" else "Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        launcher.launch("${party.companyName}_History.pdf")
    }

    private fun exportAllPartiesToPdf(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val success = PdfExporter(applicationContext).exportCustomersWithHistoryToPdf(
                uri, "Complete Client List", allPartiesList, db
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, if (success) "All Records Exported!" else "Failed", Toast.LENGTH_SHORT).show()
            }
        }
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
            val parties = db.partyDao().getAllParties()
            
            // 🛡️ SYNC FIX: If lastOrderDate is missing in party_table, fetch it from orders_table
            val syncedParties = parties.map { party ->
                if (party.lastOrderDate.isEmpty()) {
                    val latestDate = db.orderDao().getLatestOrderDateForCustomer(party.companyName)
                    if (latestDate != null) {
                        party.lastOrderDate = latestDate
                        db.partyDao().updateParty(party)
                    }
                }
                party
            }
            
            allPartiesList = syncedParties
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
