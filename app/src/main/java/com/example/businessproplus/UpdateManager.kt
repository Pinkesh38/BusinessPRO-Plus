package com.example.businessproplus

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class UpdateManager(private val context: Context) {

    // 🛡️ SECURITY: Use HTTPS only. 
    private val UPDATE_JSON_URL = "https://raw.githubusercontent.com/Pinkesh38/BusinessPRO-Plus/main/update.json"

    suspend fun checkForUpdates() {
        val updateInfo = fetchUpdateInfo() ?: return

        val currentVersionCode = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (e: Exception) { 0 }

        if (updateInfo.versionCode > currentVersionCode) {
            withContext(Dispatchers.Main) {
                showUpdateDialog(updateInfo)
            }
        }
    }

    private suspend fun fetchUpdateInfo(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val jsonStr = URL(UPDATE_JSON_URL).readText()
            val json = JSONObject(jsonStr)
            UpdateInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.getString("versionName"),
                apkUrl = json.getString("apkUrl"),
                releaseNotes = json.getString("releaseNotes")
            )
        } catch (e: Exception) {
            Log.e("UpdateManager", "Check failed. Check connection.")
            null
        }
    }

    private fun showUpdateDialog(info: UpdateInfo) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Security & Feature Update")
            .setMessage("Version ${info.versionName} is available.\n\nNotes:\n${info.releaseNotes}")
            .setCancelable(false) // 🛡️ Prevent bypassing critical updates
            .setPositiveButton("Update Now") { _, _ ->
                downloadAndInstallApk(info.apkUrl)
            }
            .setNegativeButton("Remind Later", null)
            .show()
    }

    private fun downloadAndInstallApk(url: String) {
        // 🛡️ Validation: Ensure the URL is from your trusted domain
        if (!url.contains("github.com/Pinkesh38")) {
            Toast.makeText(context, "Untrusted update source!", Toast.LENGTH_LONG).show()
            return
        }

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("BusinessPRO+ Security Update")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "update.apk")

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    installApk()
                    context.unregisterReceiver(this)
                }
            }
        }
        
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(onComplete, filter)
        }
    }

    private fun installApk() {
        val file = java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    data class UpdateInfo(val versionCode: Int, val versionName: String, val apkUrl: String, val releaseNotes: String)
}