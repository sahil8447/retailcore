package com.sahil.retailcore.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sahil.retailcore.RetailApp
import com.sahil.retailcore.data.local.entity.SettingsEntity
import java.text.SimpleDateFormat
import java.util.*

class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as RetailApp
        val settingsRepo = app.settingsRepository
        
        // 1. Check if user is signed in
        val account = GoogleAuthHelper.getSignedInAccount(applicationContext) ?: return Result.failure()

        try {
            // 2. Check if database has changed
            val dbFile = applicationContext.getDatabasePath("retail_database")
            val lastModified = dbFile.lastModified()
            val lastBackedUpChange = settingsRepo.getSetting("last_database_change_timestamp")?.toLongOrNull() ?: 0L

            if (lastModified <= lastBackedUpChange) {
                return Result.success()
            }

            // 3. Perform Backup
            val backupManager = BackupManager(applicationContext)
            val backupFile = backupManager.createBackupFile() ?: return Result.retry()

            val driveHelper = GoogleDriveHelper(applicationContext, account)
            val success = driveHelper.uploadBackupFile(backupFile)

            if (success) {
                val timestamp = System.currentTimeMillis()
                val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))
                
                settingsRepo.saveSetting("last_successful_backup_timestamp", timestamp.toString())
                settingsRepo.saveSetting("last_backup_status", "Auto-backup successful at $dateStr")
                
                // Track database modification time to avoid redundant backups
                settingsRepo.saveSetting("last_database_change_timestamp", lastModified.toString())
                
                backupManager.cleanUp(backupFile)

                // 4. Cleanup old backups (Keep latest 10)
                try {
                    val backups = driveHelper.listBackupFiles()
                        .filter { it.name?.startsWith("RetailCore_Backup_") == true && it.name?.endsWith(".zip") == true }
                    
                    if (backups.size > 10) {
                        val toDelete = backups.drop(10)
                        var deletedCount = 0
                        for (file in toDelete) {
                            if (driveHelper.deleteFile(file.id)) {
                                deletedCount++
                            }
                        }
                        android.util.Log.d("BackupWorker", "Cleaned up $deletedCount old backups from Google Drive")
                    }
                } catch (cleanupEx: Exception) {
                    // Fail silently on cleanup - backup was already successful
                    android.util.Log.e("BackupWorker", "Failed to cleanup old backups", cleanupEx)
                }

                return Result.success()
            } else {
                backupManager.cleanUp(backupFile)
                return Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
