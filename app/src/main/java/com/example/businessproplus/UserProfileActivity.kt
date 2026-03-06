package com.example.businessproplus

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.businessproplus.databinding.ActivityUserProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private val db by lazy { AppDatabase.getDatabase(applicationContext) }
    private var currentUser: User? = null
    private var currentUserId: Int = -1
    private var imageUri: Uri? = null
    private var imagePath: String? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera() else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            binding.ivProfileImage.setImageURI(imageUri)
            Toast.makeText(this, "Photo updated!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.profileRootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val sharedPrefs = getSharedPreferences("BusinessProPrefs", Context.MODE_PRIVATE)
        currentUserId = sharedPrefs.getInt("USER_ID", -1)
        val userRole = sharedPrefs.getString("USER_ROLE", "Staff")

        // ADMIN FEATURE: Only show "Manage All Users" if the logged-in user is an Admin
        if (userRole == "Admin") {
            binding.btnManageUsersFromProfile.visibility = View.VISIBLE
        }

        loadUserData()

        binding.fabChangePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnSaveProfile.setOnClickListener { saveProfile() }
        binding.btnManageUsersFromProfile.setOnClickListener { startActivity(Intent(this, ManageUsersActivity::class.java)) }
        binding.btnDeleteAccount.setOnClickListener { confirmDeleteAccount() }
    }

    private fun loadUserData() {
        lifecycleScope.launch(Dispatchers.IO) {
            currentUser = db.userDao().getUserById(currentUserId)

            withContext(Dispatchers.Main) {
                currentUser?.let { user ->
                    binding.etProfileName.setText(user.username)
                    binding.etProfileMobile.setText(user.mobileNo)
                    binding.tvProfileRole.text = "Role: ${user.role}"
                    imagePath = user.imagePath
                    
                    if (!user.imagePath.isNullOrEmpty()) {
                        Glide.with(this@UserProfileActivity)
                            .load(user.imagePath)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .into(binding.ivProfileImage)
                    }
                }
            }
        }
    }

    private fun openCamera() {
        val file = createMediaFile("PROFILE_", ".jpg")
        imagePath = file.absolutePath
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        imageUri = uri
        imageUri?.let { takePhotoLauncher.launch(it) }
    }

    private fun createMediaFile(prefix: String, suffix: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File.createTempFile("${prefix}${timeStamp}_", suffix, getExternalFilesDir(Environment.DIRECTORY_PICTURES))
    }

    private fun saveProfile() {
        val newName = binding.etProfileName.text.toString().trim()
        if (newName.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        currentUser?.let { user ->
            user.username = newName
            user.mobileNo = binding.etProfileMobile.text.toString().trim()
            user.imagePath = imagePath

            lifecycleScope.launch(Dispatchers.IO) {
                db.userDao().updateUser(user)
                
                // 🛡️ FIX: Sync with SharedPreferences so dashboard reflects the change
                val sharedPrefs = getSharedPreferences("BusinessProPrefs", Context.MODE_PRIVATE)
                sharedPrefs.edit()
                    .putString("USER_NAME", user.username)
                    .apply()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserProfileActivity, "Profile Updated!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun confirmDeleteAccount() {
        if (currentUserId == 1) {
            Toast.makeText(this, "Security Alert: Cannot delete the Master Admin!", Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure? This cannot be undone.")
            .setPositiveButton("Yes, Delete") { _, _ -> deleteAccount() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        currentUser?.let { user ->
            lifecycleScope.launch(Dispatchers.IO) {
                db.userDao().deleteUser(user)
                withContext(Dispatchers.Main) {
                    logout()
                }
            }
        }
    }

    private fun logout() {
        getSharedPreferences("BusinessProPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
