package com.example.businessproplus

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.businessproplus.databinding.ActivityOhmsCalculatorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

class OhmsCalculatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOhmsCalculatorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOhmsCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupListeners() {
        binding.btnViewHistory.setOnClickListener {
            startActivity(Intent(this, CalculationHistoryActivity::class.java))
        }

        binding.btnCalculate.setOnClickListener {
            performCalculation()
        }

        binding.btnReset.setOnClickListener {
            resetFields()
        }
    }

    private fun performCalculation() {
        val v = binding.etCalcVoltage.text.toString().toDoubleOrNull()
        val i = binding.etCalcAmpere.text.toString().toDoubleOrNull()
        val r = binding.etCalcOhms.text.toString().toDoubleOrNull()
        val p = binding.etCalcWatts.text.toString().toDoubleOrNull()

        val inputs = listOf(v, i, r, p)
        val count = inputs.count { it != null }

        if (count != 2) {
            Toast.makeText(this, "Please enter exactly two values!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            var resV = v ?: 0.0
            var resI = i ?: 0.0
            var resR = r ?: 0.0
            var resP = p ?: 0.0

            if (v != null && i != null) {
                resR = v / i
                resP = v * i
            } else if (v != null && r != null) {
                resI = v / r
                resP = (v * v) / r
            } else if (v != null && p != null) {
                resI = p / v
                resR = (v * v) / p
            } else if (i != null && r != null) {
                resV = i * r
                resP = i * i * r
            } else if (i != null && p != null) {
                resV = p / i
                resR = p / (i * i)
            } else if (r != null && p != null) {
                resV = sqrt(p * r)
                resI = sqrt(p / r)
            }

            binding.etCalcVoltage.setText(formatResult(resV))
            binding.etCalcAmpere.setText(formatResult(resI))
            binding.etCalcOhms.setText(formatResult(resR))
            binding.etCalcWatts.setText(formatResult(resP))

            binding.cardResult.visibility = View.VISIBLE
            binding.tvMainResult.text = "Results: V=${formatResult(resV)}, I=${formatResult(resI)}, R=${formatResult(resR)}, P=${formatResult(resP)}"

            saveToHistory(formatResult(resV), formatResult(resI), formatResult(resR), formatResult(resP))

        } catch (e: Exception) {
            Toast.makeText(this, "Calculation error!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetFields() {
        binding.etCalcVoltage.text?.clear()
        binding.etCalcAmpere.text?.clear()
        binding.etCalcOhms.text?.clear()
        binding.etCalcWatts.text?.clear()
        binding.cardResult.visibility = View.GONE
        binding.etCalcVoltage.requestFocus()
    }

    private fun saveToHistory(v: String, i: String, r: String, p: String) {
        val timestamp = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date())
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            db.calculationHistoryDao().insertCalculation(
                CalculationHistory(voltage = v, ampere = i, ohms = r, watts = p, timestamp = timestamp)
            )
        }
    }

    private fun formatResult(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }
}