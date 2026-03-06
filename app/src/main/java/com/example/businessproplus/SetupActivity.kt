package com.example.businessproplus

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.businessproplus.databinding.ActivitySetupBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private var imageUri: Uri? = null
    private var imagePath: String? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera() else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            binding.ivSetupProfile.setImageURI(imageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupLanguageDropdown()

        binding.fabSetupPhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnSubmitSetup.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun setupLanguageDropdown() {
        val languages = arrayOf("English", "हिंदी (Hindi)", "ગુજરાતી (Gujarati)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, languages)
        binding.spinnerSetupLanguage.setAdapter(adapter)
        binding.spinnerSetupLanguage.setText("English", false)
    }

    private fun openCamera() {
        val file = File(getExternalFilesDir(null), "SETUP_PROFILE.jpg")
        imagePath = file.absolutePath
        imageUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        takePhotoLauncher.launch(imageUri!!)
    }

    private fun validateAndSubmit() {
        val name = binding.etSetupName.text.toString().trim()
        val mobile = binding.etSetupMobile.text.toString().trim()
        val pin = binding.etSetupPin.text.toString().trim()
        val lang = binding.spinnerSetupLanguage.text.toString()

        if (name.isEmpty() || mobile.isEmpty() || pin.length != 4) {
            Toast.makeText(this, "Please fill all details correctly", Toast.LENGTH_SHORT).show()
            return
        }

        // Apply Language immediately
        val localeTag = when (lang) {
            "हिंदी (Hindi)" -> "hi"
            "ગુજરાતી (Gujarati)" -> "gu"
            else -> "en"
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag))

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            
            // The first user ever created is the Admin
            val isFirstUser = db.userDao().getUserCount() == 0
            val newUser = User(
                username = name,
                pinCode = pin,
                role = if (isFirstUser) "Admin" else "Staff",
                mobileNo = mobile,
                imagePath = imagePath
            )
            
            db.userDao().insertUser(newUser)

            withContext(Dispatchers.Main) {
                getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit()
                    .putBoolean("IS_SETUP_COMPLETE", true)
                    .apply()

                if (isFirstUser) {
                    Toast.makeText(this@SetupActivity, "Admin Setup Complete!", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@SetupActivity, LoginActivity::class.java))
                } else {
                    Toast.makeText(this@SetupActivity, "Setup submitted successfully.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@SetupActivity, LoginActivity::class.java))
                }
                finish()
            }
        }
    }
}