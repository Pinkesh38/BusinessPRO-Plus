package com.example.businessproplus

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalculationHistoryActivity : AppCompatActivity() {

    private lateinit var lvCalcHistory: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calculation_history)

        val rootView = findViewById<View>(R.id.history_root_layout)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "History"

        lvCalcHistory = findViewById(R.id.lvCalcHistory)
        val btnClearHistory = findViewById<Button>(R.id.btnClearHistory)

        loadHistory()

        btnClearHistory.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                AppDatabase.getDatabase(applicationContext).calculationHistoryDao().clearHistory()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CalculationHistoryActivity, "History Cleared", Toast.LENGTH_SHORT).show()
                    loadHistory()
                }
            }
        }
    }

    private fun loadHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            val history = AppDatabase.getDatabase(applicationContext).calculationHistoryDao().getRecentCalculations()
            val displayList = history.map { 
                "V: ${it.voltage}V | I: ${it.ampere}A\nR: ${it.ohms}Ω | P: ${it.watts}W\nTime: ${it.timestamp}"
            }

            withContext(Dispatchers.Main) {
                lvCalcHistory.adapter = ArrayAdapter(this@CalculationHistoryActivity, android.R.layout.simple_list_item_1, displayList)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}