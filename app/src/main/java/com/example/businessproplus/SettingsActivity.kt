package com.example.businessproplus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.example.businessproplus.databinding.ActivitySettingsBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Toast.makeText(this, "Sign-In Success", Toast.LENGTH_SHORT).show()
                // Auto-trigger backup after sign-in
                val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                performBackup(prefs)
            } catch (e: ApiException) {
                Toast.makeText(this, "Sign-In Failed: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViews()
        setupListeners()

        // 🛡️ Initialize Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupViews() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val businessPrefs = getSharedPreferences("BusinessProPrefs", Context.MODE_PRIVATE)
        val userRole = businessPrefs.getString("USER_ROLE", "Staff")

        binding.switchBiometric.isChecked = prefs.getBoolean("USE_BIOMETRIC", true)
        binding.switchDarkMode.isChecked = prefs.getBoolean("DARK_MODE", false)

        if (userRole != "Admin") {
            binding.layoutAdminSettings.visibility = View.GONE
        }

        // Display Last Backup Time
        val lastBackupTime = BackupPrefs(this).getLastBackupTime()
        binding.tvLastBackup.text = "Last Backup: $lastBackupTime"

        // Timeout Spinner
        val timeouts = arrayOf("5 Minutes", "10 Minutes", "30 Minutes", "Never")
        binding.spinnerTimeout.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, timeouts)
        val currentTimeout = prefs.getString("LOGOUT_TIMEOUT", "10 Minutes")
        binding.spinnerTimeout.setSelection(timeouts.indexOf(currentTimeout))

        // Language Spinner
        val languages = arrayOf("English", "हिंदी (Hindi)", "ગુજરાતી (Gujarati)")
        binding.spinnerLanguageSettings.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
    }

    private fun setupListeners() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("USE_BIOMETRIC", isChecked).apply()
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("DARK_MODE", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.spinnerTimeout.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val timeouts = arrayOf("5 Minutes", "10 Minutes", "30 Minutes", "Never")
                prefs.edit().putString("LOGOUT_TIMEOUT", timeouts[p2]).apply()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        binding.spinnerLanguageSettings.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedLang = when (p2) {
                    1 -> "hi"
                    2 -> "gu"
                    else -> "en"
                }
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selectedLang))
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        binding.layoutChangePin.setOnClickListener { startActivity(Intent(this, ChangePinActivity::class.java)) }
        binding.layoutErrorLog.setOnClickListener { startActivity(Intent(this, ErrorLogActivity::class.java)) }
        
        // 🛡️ FIX: Added missing listener for Activity Logs
        binding.layoutActivityLog.setOnClickListener { startActivity(Intent(this, UserActivityActivity::class.java)) }
        
        // BACKUP BUTTON CLICK
        binding.btnBackup.setOnClickListener {
            val account = GoogleSignIn.getLastSignedInAccount(this)
            if (account == null) {
                signInLauncher.launch(googleSignInClient.signInIntent)
            } else {
                performBackup(prefs)
            }
        }

        // RESTORE BUTTON CLICK WITH CONFIRMATION DIALOG
        binding.btnRestore.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Restore Backup?")
                .setMessage("This will replace all current data. Continue?")
                .setPositiveButton("Yes") { _, _ ->
                    lifecycleScope.launch {
                        binding.loadingOverlay.visibility = View.VISIBLE
                        val success = BackupManager(this@SettingsActivity).restoreFromDrive()
                        binding.loadingOverlay.visibility = View.GONE
                        if (success) {
                            Toast.makeText(this@SettingsActivity, "Restore Success", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@SettingsActivity, SplashActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@SettingsActivity, "Restore Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnHardReset.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hard Reset")
                .setMessage(R.string.reset_confirmation)
                .setPositiveButton("Reset Everything") { _, _ -> performHardReset() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun performBackup(prefs: android.content.SharedPreferences) {
        lifecycleScope.launch {
            binding.loadingOverlay.visibility = View.VISIBLE
            val success = BackupManager(this@SettingsActivity).performFullBackup()
            binding.loadingOverlay.visibility = View.GONE
            if (success) {
                val lastBackupTime = BackupPrefs(this@SettingsActivity).getLastBackupTime()
                binding.tvLastBackup.text = "Last Backup: $lastBackupTime"
            }
        }
    }

    private fun performHardReset() {
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(applicationContext).clearAllTables()
            withContext(Dispatchers.Main) {
                val intent = Intent(this@SettingsActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }
}
