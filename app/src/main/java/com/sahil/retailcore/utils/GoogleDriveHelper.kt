package com.sahil.retailcore.utils

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class GoogleDriveHelper(private val context: Context, private val account: GoogleSignInAccount) {

    private val driveService: Drive by lazy {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account

        Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("RetailCore ERP").build()
    }

    suspend fun uploadBackupFile(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val metadata = com.google.api.services.drive.model.File()
                .setName(file.name)
                .setParents(Collections.singletonList("appDataFolder"))

            val content = FileContent("application/zip", file)
            
            driveService.files().create(metadata, content).execute()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun listBackupFiles(): List<com.google.api.services.drive.model.File> = withContext(Dispatchers.IO) {
        try {
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name, createdTime, size)")
                .setOrderBy("createdTime desc")
                .execute()
            result.getFiles() ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun downloadBackupFile(fileId: String, targetFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            targetFile.outputStream().use { outputStream ->
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteFile(fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            driveService.files().delete(fileId).execute()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
