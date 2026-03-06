package com.example.businessproplus

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.businessproplus.databinding.ActivityAddPartyBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddPartyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPartyBinding
    private var currentPartyId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddPartyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.addPartyRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupPartyTypeDropdown()

        // 🎯 EDIT MODE HANDLER: Load existing party if ID is provided
        currentPartyId = intent.getIntExtra("PARTY_ID", 0)
        if (currentPartyId != 0) {
            binding.toolbar.title = "Edit Party Details"
            binding.btnSaveParty.text = "Update Party Profile"
            loadExistingParty(currentPartyId)
        }

        binding.btnSaveParty.setOnClickListener {
            validateAndSave()
        }
    }

    private fun setupPartyTypeDropdown() {
        val partyTypes = arrayOf("Customer", "Supplier")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, partyTypes)
        binding.spinnerPartyType.setAdapter(adapter)
        binding.spinnerPartyType.setText("Customer", false)
    }

    private fun loadExistingParty(id: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val party = db.partyDao().getPartyById(id)
            withContext(Dispatchers.Main) {
                party?.let {
                    binding.spinnerPartyType.setText(it.partyType, false)
                    binding.etCompanyName.setText(it.companyName)
                    binding.etContactPerson.setText(it.contactPerson)
                    binding.etPartyContactNo.setText(it.contactNo)
                    binding.etPartyContactNo2.setText(it.contactNo2)
                    binding.etGstNo.setText(it.gstNo)
                    binding.etAddress.setText(it.address)
                    binding.etCreditLimit.setText(it.creditLimit.toString())
                    binding.etCreditPeriod.setText(it.creditPeriodDays.toString())
                    binding.etNotes.setText(it.notes)
                    binding.switchBlacklist.isChecked = it.isBlacklisted
                }
            }
        }
    }

    private fun validateAndSave() {
        val companyName = binding.etCompanyName.text.toString().trim()

        if (companyName.isEmpty()) {
            Toast.makeText(this, "Company Name is required!", Toast.LENGTH_SHORT).show()
            return
        }

        val partyType = binding.spinnerPartyType.text.toString()
        val contactPerson = binding.etContactPerson.text.toString().trim()
        val contactNo = binding.etPartyContactNo.text.toString().trim()
        val contactNo2 = binding.etPartyContactNo2.text.toString().trim()
        val gstNo = binding.etGstNo.text.toString().trim().uppercase()
        val address = binding.etAddress.text.toString().trim()
        val creditLimit = binding.etCreditLimit.text.toString().toDoubleOrNull() ?: 0.0
        val creditPeriodDays = binding.etCreditPeriod.text.toString().toIntOrNull() ?: 0
        val notes = binding.etNotes.text.toString().trim()
        val isBlacklisted = binding.switchBlacklist.isChecked

        lifecycleScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(applicationContext)
            
            // Duplicate Detection (Only for new parties)
            if (currentPartyId == 0) {
                val existing = database.partyDao().getPartyByName(companyName)
                if (existing != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AddPartyActivity, "A party with this name already exists!", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
            }

            val party = Party(
                id = currentPartyId,
                partyType = partyType,
                companyName = companyName,
                contactPerson = contactPerson,
                contactNo = contactNo,
                contactNo2 = contactNo2,
                gstNo = gstNo,
                address = address,
                creditLimit = creditLimit,
                creditPeriodDays = creditPeriodDays,
                notes = notes,
                isBlacklisted = isBlacklisted
            )

            if (currentPartyId == 0) {
                database.partyDao().insertParty(party)
            } else {
                database.partyDao().updateParty(party)
            }

            withContext(Dispatchers.Main) {
                val msg = if (currentPartyId == 0) "Party Saved Successfully!" else "Party Updated Successfully!"
                Toast.makeText(this@AddPartyActivity, msg, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}