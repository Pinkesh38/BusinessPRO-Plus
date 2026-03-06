package com.example.businessproplus

import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File as javaFile

class DriveServiceHelper(private val driveService: Drive) {

    /**
     * Uploads a file to the user's Google Drive root folder.
     * If a file with the same name exists, it updates it.
     */
    suspend fun uploadFileToDrive(localFile: javaFile): String? = withContext(Dispatchers.IO) {
        try {
            // 1. Search for existing file with the same name
            val query = "name = '${localFile.name}' and trashed = false"
            val result = driveService.files().list()
                .setQ(query)
                .setFields("files(id)")
                .execute()
            
            val existingFile = result.files.firstOrNull()

            // 2. Prepare metadata and content
            val mediaContent = FileContent("application/octet-stream", localFile)

            return@withContext if (existingFile != null) {
                // Update existing file
                driveService.files().update(existingFile.id, null, mediaContent).execute().id
            } else {
                // Create new file
                val metadata = File().apply {
                    name = localFile.name
                }
                driveService.files().create(metadata, mediaContent).execute().id
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}