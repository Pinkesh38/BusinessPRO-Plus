package com.example.businessproplus

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.businessproplus.databinding.ActivityCloudSyncBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Collections

class CloudSyncActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCloudSyncBinding
    private var driveServiceHelper: DriveServiceHelper? = null
    private lateinit var googleSignInClient: GoogleSignInClient

    // 1. Google Sign-In Result Launcher
    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            Toast.makeText(this, "Sign-In Success", Toast.LENGTH_SHORT).show()
            initializeDriveService(account.account!!)
        } catch (e: ApiException) {
            Toast.makeText(this, "Sign-In Failed: ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCloudSyncBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🛡️ INITIALIZE GOOGLE SIGN IN OPTIONS
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnSignIn.setOnClickListener { signIn() }
        binding.btnUpload.setOnClickListener { uploadBackup() }
    }

    private fun signIn() {
        signInLauncher.launch(googleSignInClient.signInIntent)
    }

    // 3. Initialize Drive Service after login
    private fun initializeDriveService(account: android.accounts.Account) {
        val credential = GoogleAccountCredential.usingOAuth2(
            this, Collections.singleton(DriveScopes.DRIVE_FILE)
        ).setSelectedAccount(account)

        val googleDriveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("BusinessPRO+").build()

        driveServiceHelper = DriveServiceHelper(googleDriveService)
    }

    // 4. Example: Upload your backup database file
    private fun uploadBackup() {
        if (driveServiceHelper == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
            return
        }

        val backupFile = File(filesDir, "business_backup.db")
        if (!backupFile.exists()) {
            Toast.makeText(this, "Local backup file not found", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.Main) {
            val fileId = driveServiceHelper?.uploadFileToDrive(backupFile)
            if (fileId != null) {
                Toast.makeText(this@CloudSyncActivity, "Upload Success! ID: $fileId", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@CloudSyncActivity, "Upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}