package com.example.businessproplus

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
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

object LocalBackupManager {

    // 🛡️ SECURITY: 16-character key for AES-128 encryption
    private const val AES_KEY = "BusinessProPlusX"

    /**
     * Creates a local backup, ENCRYPTS it using AES/CBC, and then triggers the cloud upload
     */
    fun backupDatabase(context: Context): Boolean {
        return try {
            val db = AppDatabase.getDatabase(context)
            // Ensure all data is written to the main DB file
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            val dbFile = context.getDatabasePath("business_pro_database")
            if (!dbFile.exists()) return false

            // 1. Create a temporary plain file
            val tempPlainFile = File(context.cacheDir, "temp_db")
            dbFile.copyTo(tempPlainFile, overwrite = true)

            // 2. Encrypt it into the final backup file in internal storage
            val backupFile = File(context.filesDir, "business_backup.db")
            encryptFile(tempPlainFile, backupFile)

            // 3. Clean up temp file
            tempPlainFile.delete()

            // 4. Trigger Google Drive Upload
            uploadToDrive(context, backupFile)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Decrypts the backup file and restores it to the active database
     */
    fun restoreDatabase(context: Context): Boolean {
        return try {
            val backupFile = File(context.filesDir, "business_backup.db")
            if (!backupFile.exists()) return false

            // 1. Decrypt the backup into a temporary plain file
            val tempDecryptedFile = File(context.cacheDir, "temp_decrypted")
            decryptFile(backupFile, tempDecryptedFile)

            // 2. Safely replace the database
            AppDatabase.getDatabase(context).close()
            val dbFile = context.getDatabasePath("business_pro_database")
            
            tempDecryptedFile.copyTo(dbFile, overwrite = true)
            
            // Purge WAL journals to avoid corruption
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()

            // 3. Clean up temp file
            tempDecryptedFile.delete()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Helper to ENCRYPT a file using AES/CBC with random IV (Beginner friendly stream approach)
     */
    private fun encryptFile(inputFile: File, outputFile: File) {
        val secretKey = SecretKeySpec(AES_KEY.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv) 
        val ivSpec = IvParameterSpec(iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

        FileOutputStream(outputFile).use { fos ->
            fos.write(iv) // Save IV at the start of file
            CipherOutputStream(fos, cipher).use { cos ->
                FileInputStream(inputFile).use { fis ->
                    fis.copyTo(cos)
                }
            }
        }
    }

    /**
     * Helper to DECRYPT a file using AES/CBC
     */
    private fun decryptFile(inputFile: File, outputFile: File) {
        val secretKey = SecretKeySpec(AES_KEY.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

        FileInputStream(inputFile).use { fis ->
            val iv = ByteArray(16)
            fis.read(iv) // Load IV from the start of file
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

            CipherInputStream(fis, cipher).use { cis ->
                FileOutputStream(outputFile).use { fos ->
                    cis.copyTo(fos)
                }
            }
        }
    }

    private fun uploadToDrive(context: Context, backupFile: File) {
        Thread {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(context)
                if (account != null) {
                    val credential = GoogleAccountCredential.usingOAuth2(
                        context, Collections.singleton(DriveScopes.DRIVE_FILE)
                    ).setSelectedAccount(account.account)

                    val service = Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                        .setApplicationName("BusinessPRO+").build()

                    val query = "name = 'business_backup.db' and trashed = false"
                    val result = service.files().list().setQ(query).setFields("files(id)").execute()
                    val existingFile = result.files.firstOrNull()

                    val mediaContent = FileContent("application/octet-stream", backupFile)

                    if (existingFile != null) {
                        service.files().update(existingFile.id, null, mediaContent).execute()
                    } else {
                        val fileMetadata = DriveFile().apply { name = "business_backup.db" }
                        service.files().create(fileMetadata, mediaContent).execute()
                    }
                    showToast(context, "Cloud Backup Securely Saved!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}