package com.example.businessproplus

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.Collections
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class BackupManager(context: Context) {

    private val appContext = context.applicationContext
    private val notificationHelper = NotificationHelper(appContext)
    
    private val AES_KEY = "BusinessProPlusX"

    /**
     * Entry point for the full process: Local Backup + Encryption + Cloud Upload
     */
    suspend fun performFullBackup(): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupFile = createDatabaseBackupFile() ?: return@withContext false
            val success = uploadFileToDrive(backupFile)
            
            if (success) {
                val currentTime = java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                BackupPrefs(appContext).saveLastBackupTime(currentTime)
                
                notificationHelper.sendAlert("Cloud Sync", "Business data secured successfully.", 1001)
            }
            return@withContext success
        } catch (e: Exception) {
            notificationHelper.sendAlert("Backup Failed", "${e.localizedMessage}", 1002)
            return@withContext false
        }
    }

    suspend fun restoreFromDrive(): Boolean = withContext(Dispatchers.IO) {
        try {
            val encryptedBackup = downloadBackupFromDrive() ?: return@withContext false
            val decryptedFile = File(appContext.cacheDir, "temp_decrypted.db")
            
            decryptFile(encryptedBackup, decryptedFile)

            // 🛡️ SHUTDOWN: Ensure no other components are using the DB
            AppDatabase.getDatabase(appContext).close()

            val dbFile = appContext.getDatabasePath("business_pro_database")
            decryptedFile.copyTo(dbFile, overwrite = true)

            // Purge WAL journals
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()

            encryptedBackup.delete()
            decryptedFile.delete()

            return@withContext true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun downloadBackupFromDrive(): File? = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(appContext) ?: return@withContext null
            val credential = GoogleAccountCredential.usingOAuth2(appContext, Collections.singleton(DriveScopes.DRIVE_FILE)).setSelectedAccount(account.account)
            val service = Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential).setApplicationName("BusinessPRO+").build()

            val query = "name = 'business_backup.db' and trashed = false"
            val result = service.files().list().setQ(query).setSpaces("drive").execute()
            val driveFile = result.files.firstOrNull() ?: return@withContext null

            val localFile = File(appContext.filesDir, "downloaded_encrypted.db")
            FileOutputStream(localFile).use { service.files().get(driveFile.id).executeMediaAndDownloadTo(it) }
            localFile
        } catch (e: Exception) { null }
    }

    private fun createDatabaseBackupFile(): File? {
        return try {
            val db = AppDatabase.getDatabase(appContext)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            
            val dbFile = appContext.getDatabasePath("business_pro_database")
            if (!dbFile.exists()) return null
            
            val tempPlainFile = File(appContext.cacheDir, "temp_db")
            dbFile.copyTo(tempPlainFile, overwrite = true)

            val backupFile = File(appContext.filesDir, "business_backup.db")
            encryptFile(tempPlainFile, backupFile)

            tempPlainFile.delete()
            backupFile
        } catch (e: Exception) { null }
    }

    private suspend fun uploadFileToDrive(localFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(appContext) ?: return@withContext false
            val credential = GoogleAccountCredential.usingOAuth2(appContext, Collections.singleton(DriveScopes.DRIVE_FILE)).setSelectedAccount(account.account)
            val service = Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential).setApplicationName("BusinessPRO+").build()
            
            val query = "name = 'business_backup.db' and trashed = false"
            val result = service.files().list().setQ(query).setSpaces("drive").execute()
            val mediaContent = FileContent("application/octet-stream", localFile)
            
            if (result.files.isNotEmpty()) {
                service.files().update(result.files[0].id, null, mediaContent).execute()
            } else {
                val metadata = DriveFile().apply { name = "business_backup.db" }
                service.files().create(metadata, mediaContent).execute()
            }
            true
        } catch (e: Exception) { false }
    }

    private fun encryptFile(inputFile: File, outputFile: File) {
        val secretKey = SecretKeySpec(AES_KEY.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

        FileOutputStream(outputFile).use { fos ->
            fos.write(iv)
            CipherOutputStream(fos, cipher).use { cos ->
                FileInputStream(inputFile).use { fis ->
                    fis.copyTo(cos)
                }
            }
        }
    }

    private fun decryptFile(inputFile: File, outputFile: File) {
        val secretKey = SecretKeySpec(AES_KEY.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

        FileInputStream(inputFile).use { fis ->
            val iv = ByteArray(16)
            fis.read(iv)
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

            CipherInputStream(fis, cipher).use { cis ->
                FileOutputStream(outputFile).use { fos ->
                    cis.copyTo(fos)
                }
            }
        }
    }
}