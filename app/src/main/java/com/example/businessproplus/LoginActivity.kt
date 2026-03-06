package com.example.businessproplus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.businessproplus.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    
    private val db by lazy { AppDatabase.getDatabase(applicationContext) }
    private var failedAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.loginRootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🛡️ Ensure Master Admin exists in DB
        val masterPin = BuildConfig.MASTER_PIN
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (db.userDao().getUserCount() == 0) {
                    db.userDao().insertUser(User(username = "Master Admin", pinCode = masterPin, role = "Admin"))
                }
            }
        }

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    
                    // 🛡️ PERSISTENCE FIX: Fetch latest name from DB instead of potentially cleared Prefs
                    lifecycleScope.launch {
                        val lastUserId = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getInt("LAST_USER_ID", 1)
                        val user = withContext(Dispatchers.IO) { db.userDao().getUserById(lastUserId) }
                        
                        if (user != null) {
                            loginUser(user.role, user.id, user.username)
                        } else {
                            // Fallback if user deleted
                            loginUser("Admin", 1, "Master Admin")
                        }
                    }
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("BusinessPRO+ Login")
            .setSubtitle("Scan fingerprint to unlock")
            .setNegativeButtonText("Use PIN Code")
            .build()

        binding.ivFingerprint.setOnClickListener { biometricPrompt.authenticate(promptInfo) }

        // AUTO LOGIN FEATURE
        binding.etAdminCode.doAfterTextChanged { text ->
            val enteredPin = text.toString()
            if (enteredPin.length == 4) { 
                checkPin(enteredPin)
            }
        }

        binding.btnExitApp.setOnClickListener { finishAffinity() }
    }

    private fun checkPin(enteredPin: String) {
        if (failedAttempts >= 5) {
            Toast.makeText(this, "Too many failed attempts! App Locked.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            // 🛡️ PERSISTENCE FIX: Use the name saved in DB for the Master PIN
            if (enteredPin == BuildConfig.MASTER_PIN) {
                val masterAdmin = withContext(Dispatchers.IO) { db.userDao().getUserByPin(BuildConfig.MASTER_PIN) }
                failedAttempts = 0
                if (masterAdmin != null) {
                    loginUser(masterAdmin.role, masterAdmin.id, masterAdmin.username)
                } else {
                    loginUser("Admin", 1, "Master Admin")
                }
                return@launch
            }

            val foundUser = withContext(Dispatchers.IO) {
                db.userDao().getUserByPin(enteredPin)
            }

            if (foundUser != null) {
                failedAttempts = 0
                loginUser(foundUser.role, foundUser.id, foundUser.username)
            } else {
                failedAttempts++
                Toast.makeText(this@LoginActivity, "PIN Not Found.", Toast.LENGTH_SHORT).show()
                binding.etAdminCode.text?.clear()
            }
        }
    }

    private fun loginUser(role: String, userId: Int, username: String) {
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    db.userActivityDao().insertActivity(UserActivity(userId = userId, userName = username, action = "Logged In", timestamp = timestamp))
                } catch (e: Exception) {}
            }
            
            // 🛡️ Session Prefs (Cleared on Logout)
            getSharedPreferences("BusinessProPrefs", Context.MODE_PRIVATE).edit()
                .putString("USER_ROLE", role)
                .putInt("USER_ID", userId)
                .putString("USER_NAME", username)
                .apply()

            // 🛡️ Persistence Prefs (Never Cleared)
            getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit()
                .putInt("LAST_USER_ID", userId)
                .apply()

            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        }
    }
}
