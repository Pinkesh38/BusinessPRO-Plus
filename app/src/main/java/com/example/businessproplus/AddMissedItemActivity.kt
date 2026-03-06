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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AddMissedItemActivity : AppCompatActivity() {
    
    private val isoSdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private var selectedDateTime: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_missed_item)

        val rootView = findViewById<View>(R.id.add_missed_root)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val etMissedItemDesc = findViewById<EditText>(R.id.etMissedItemDesc)
        val etMissedPartyName = findViewById<EditText>(R.id.etMissedPartyName)
        val spinnerMissedReason = findViewById<Spinner>(R.id.spinnerMissedReason)
        val etMissedValue = findViewById<EditText>(R.id.etMissedValue)
        val etCompetitorInfo = findViewById<EditText>(R.id.etCompetitorInfo)
        val btnPickDateTime = findViewById<Button>(R.id.btnPickDateTime)
        val btnSaveMissedItem = findViewById<Button>(R.id.btnSaveMissedItem)

        val reasons = arrayOf("Out of Stock", "Price Too High", "Quality Issue", "Delivery Time", "Other")
        spinnerMissedReason.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, reasons)

        btnPickDateTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                TimePickerDialog(this, { _, hourOfDay, minute ->
                    selectedDateTime.set(year, month, dayOfMonth, hourOfDay, minute)
                    btnPickDateTime.text = isoSdf.format(selectedDateTime.time)
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnSaveMissedItem.setOnClickListener {
            // 🛡️ QA GUARD: UI Lock
            btnSaveMissedItem.isEnabled = false
            
            val itemDesc = etMissedItemDesc.text.toString().trim()
            val partyName = etMissedPartyName.text.toString().trim()
            val dateStr = isoSdf.format(selectedDateTime.time)
            val reason = spinnerMissedReason.selectedItem.toString()
            val value = etMissedValue.text.toString().toDoubleOrNull() ?: 0.0

            if (itemDesc.isEmpty()) {
                Toast.makeText(this, "Item description required!", Toast.LENGTH_SHORT).show()
                btnSaveMissedItem.isEnabled = true
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val database = AppDatabase.getDatabase(applicationContext)
                val newItem = MissedItem(
                    itemDescription = itemDesc,
                    partyName = partyName,
                    dateAsked = dateStr, // Safe ISO format
                    reason = reason,
                    estimatedValue = value,
                    competitorInfo = etCompetitorInfo.text.toString().trim()
                )
                database.missedItemDao().insertMissedItem(newItem)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddMissedItemActivity, "Logged & Secured", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}