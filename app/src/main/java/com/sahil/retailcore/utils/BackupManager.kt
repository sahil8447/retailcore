package com.sahil.retailcore.utils

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(private val context: Context) {

    private val dbName = "retail_database"

    fun createBackupFile(): File? {
        val dbFile = context.getDatabasePath(dbName)
        val shmFile = File(dbFile.absolutePath + "-shm")
        val walFile = File(dbFile.absolutePath + "-wal")

        val timestamp = SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.getDefault()).format(Date())
        val backupFileName = "RetailCore_Backup_$timestamp.zip"
        val backupFile = File(context.cacheDir, backupFileName)

        return try {
            ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
                addFileToZip(dbFile, zos)
                if (shmFile.exists()) addFileToZip(shmFile, zos)
                if (walFile.exists()) addFileToZip(walFile, zos)
            }
            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun addFileToZip(file: File, zos: ZipOutputStream) {
        FileInputStream(file).use { fis ->
            val zipEntry = ZipEntry(file.name)
            zos.putNextEntry(zipEntry)
            val buffer = ByteArray(1024)
            var length: Int
            while (fis.read(buffer).also { length = it } >= 0) {
                zos.write(buffer, 0, length)
            }
            zos.closeEntry()
        }
    }
    
    fun cleanUp(file: File) {
        if (file.exists()) {
            file.delete()
        }
    }

    fun restoreFromBackup(zipFile: File): Boolean {
        val dbFile = context.getDatabasePath(dbName)
        val dbDir = dbFile.parentFile ?: return false
        
        return try {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val targetFile = File(dbDir, entry.name)
                    FileOutputStream(targetFile).use { fos ->
                        val buffer = ByteArray(1024)
                        var length: Int
                        while (zis.read(buffer).also { length = it } >= 0) {
                            fos.write(buffer, 0, length)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
