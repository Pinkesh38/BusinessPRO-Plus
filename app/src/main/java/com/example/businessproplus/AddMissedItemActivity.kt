package com.example.businessproplus

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.businessproplus.databinding.ActivityAddMissedItemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AddMissedItemActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAddMissedItemBinding
    private val isoSdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private var selectedDateTime: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddMissedItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.addMissedRootContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBar.setPadding(0, systemBars.top, 0, 0)
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val reasons = arrayOf("Out of Stock", "Price Too High", "Quality Issue", "Delivery Time", "Other")
        binding.spinnerMissedReason.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, reasons)

        binding.btnPickDateTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                TimePickerDialog(this, { _, hourOfDay, minute ->
                    selectedDateTime.set(year, month, dayOfMonth, hourOfDay, minute)
                    binding.btnPickDateTime.text = isoSdf.format(selectedDateTime.time)
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnSaveMissedItem.setOnClickListener {
            binding.btnSaveMissedItem.isEnabled = false
            
            val itemDesc = binding.etMissedItemDesc.text.toString().trim()
            val partyName = binding.etMissedPartyName.text.toString().trim()
            val dateStr = isoSdf.format(selectedDateTime.time)
            val reason = binding.spinnerMissedReason.selectedItem.toString()
            val value = binding.etMissedValue.text.toString().toDoubleOrNull() ?: 0.0

            if (itemDesc.isEmpty()) {
                Toast.makeText(this, "Item description required!", Toast.LENGTH_SHORT).show()
                binding.btnSaveMissedItem.isEnabled = true
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val database = AppDatabase.getDatabase(applicationContext)
                val newItem = MissedItem(
                    itemDescription = itemDesc,
                    partyName = partyName,
                    dateAsked = dateStr, 
                    reason = reason,
                    estimatedValue = value,
                    competitorInfo = binding.etCompetitorInfo.text.toString().trim()
                )
                database.missedItemDao().insertMissedItem(newItem)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddMissedItemActivity, "Logged & Secured", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}