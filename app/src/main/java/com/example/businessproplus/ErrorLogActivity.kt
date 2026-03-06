package com.example.businessproplus

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class ErrorLogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error_log)
        // 1. Turn on the built-in Android Back Arrow!
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tvErrorOutput = findViewById<TextView>(R.id.tvErrorOutput)
        val btnClearLogs = findViewById<Button>(R.id.btnClearLogs)

        val logFile = File(applicationContext.filesDir, "error_logs.txt")

        // 1. Read the file and display it
        if (logFile.exists()) {
            val logs = logFile.readText()
            if (logs.isNotEmpty()) {
                tvErrorOutput.text = logs
                tvErrorOutput.setTextColor(android.graphics.Color.parseColor("#FFEB3B")) // Yellow text for warnings
            }
        }

        // 2. Clear the file
        btnClearLogs.setOnClickListener {
            if (logFile.exists()) {
                logFile.writeText("") // Overwrite with blank text
                tvErrorOutput.text = "Logs cleared. System stable."
                tvErrorOutput.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Green text
                Toast.makeText(this, "Logs Cleared", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // 2. This function listens for the top Back Arrow being clicked
    override fun onSupportNavigateUp(): Boolean {
        finish() // This safely closes the current screen and drops you back to the Dashboard!
        return true
    }
}
