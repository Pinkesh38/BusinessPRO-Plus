package com.example.businessproplus

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePinActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_pin)
        // 1. Turn on the built-in Android Back Arrow!
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val etCurrentPin = findViewById<EditText>(R.id.etCurrentPin)
        val etNewPin = findViewById<EditText>(R.id.etNewPin)
        val etConfirmNewPin = findViewById<EditText>(R.id.etConfirmNewPin)
        val btnUpdatePin = findViewById<Button>(R.id.btnUpdatePin)

        // Retrieve the logged-in User's ID from memory
        val sharedPrefs = getSharedPreferences("BusinessProPrefs", Context.MODE_PRIVATE)
        val currentUserId = sharedPrefs.getInt("USER_ID", -1)

        btnUpdatePin.setOnClickListener {
            val currentPinInput = etCurrentPin.text.toString()
            val newPin = etNewPin.text.toString()
            val confirmPin = etConfirmNewPin.text.toString()

            // 1. Basic validation
            if (newPin.length < 4) {
                Toast.makeText(this, "New PIN must be at least 4 digits!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPin != confirmPin) {
                Toast.makeText(this, "New PINs do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Database validation and update
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)
                val user = db.userDao().getUserById(currentUserId)

                withContext(Dispatchers.Main) {
                    if (user != null && user.pinCode == currentPinInput) {
                        // Current PIN is correct! Update it in the background.
                        lifecycleScope.launch(Dispatchers.IO) {
                            db.userDao().updatePin(currentUserId, newPin)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ChangePinActivity, "PIN Successfully Updated!", Toast.LENGTH_LONG).show()
                                finish() // Close the screen
                            }
                        }
                    } else {
                        Toast.makeText(this@ChangePinActivity, "Incorrect Current PIN!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // 2. This function listens for the top Back Arrow being clicked
    override fun onSupportNavigateUp(): Boolean {
        finish() // This safely closes the current screen and drops you back to the Dashboard!
        return true
    }
}
