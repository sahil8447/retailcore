package com.sahil.retailcore.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.sahil.retailcore.RetailApp
import com.sahil.retailcore.data.repository.SettingsRepository
import com.sahil.retailcore.utils.BackupManager
import com.sahil.retailcore.utils.BackupScheduler
import com.sahil.retailcore.utils.GoogleAuthHelper
import com.sahil.retailcore.utils.GoogleDriveHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _shopName = MutableStateFlow("")
    val shopName = _shopName.asStateFlow()

    private val _shopAddress = MutableStateFlow("")
    val shopAddress = _shopAddress.asStateFlow()

    private val _shopGst = MutableStateFlow("")
    val shopGst = _shopGst.asStateFlow()

    private val _googleAccountEmail = MutableStateFlow<String?>(null)
    val googleAccountEmail = _googleAccountEmail.asStateFlow()

    private val _lastBackupStatus = MutableStateFlow("Never")
    val lastBackupStatus = _lastBackupStatus.asStateFlow()

    private val _backupInProgress = MutableStateFlow(false)
    val backupInProgress = _backupInProgress.asStateFlow()

    private val _restoreInProgress = MutableStateFlow(false)
    val restoreInProgress = _restoreInProgress.asStateFlow()

    private val _restoreSuccessful = MutableStateFlow(false)
    val restoreSuccessful = _restoreSuccessful.asStateFlow()

    private val _cloudBackups = MutableStateFlow<List<com.google.api.services.drive.model.File>>(emptyList())
    val cloudBackups = _cloudBackups.asStateFlow()

    private val _isFetchingBackups = MutableStateFlow(false)
    val isFetchingBackups = _isFetchingBackups.asStateFlow()

    private val _nextBackupTime = MutableStateFlow<String?>(null)
    val nextBackupTime = _nextBackupTime.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            _shopName.value = repository.getSetting("shop_name") ?: ""
            _shopAddress.value = repository.getSetting("shop_address") ?: ""
            _shopGst.value = repository.getSetting("shop_gst") ?: ""
            _lastBackupStatus.value = repository.getSetting("last_backup_status") ?: "Never"
        }
    }

    fun updateNextBackupTime(context: Context) {
        val nextTime = BackupScheduler.getNextBackupTime(context)
        val account = GoogleAuthHelper.getSignedInAccount(context)
        if (account != null && nextTime != null) {
            val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(nextTime))
            _nextBackupTime.value = dateStr
        } else {
            _nextBackupTime.value = null
        }
    }

    fun checkGoogleAccount(context: Context) {
        val account = GoogleAuthHelper.getSignedInAccount(context)
        _googleAccountEmail.value = account?.email
        updateNextBackupTime(context)
    }

    fun updateGoogleAccount(account: GoogleSignInAccount?, context: Context) {
        _googleAccountEmail.value = account?.email
        if (account != null) {
            BackupScheduler.scheduleDailyBackup(context)
        }
        updateNextBackupTime(context)
    }

    fun performBackup(context: Context) {
        val account = GoogleAuthHelper.getSignedInAccount(context)
        if (account == null) {
            viewModelScope.launch { _toastMessage.emit("Please connect Google Account first") }
            return
        }

        viewModelScope.launch {
            _backupInProgress.value = true
            try {
                val backupManager = BackupManager(context)
                val backupFile = backupManager.createBackupFile()
                
                if (backupFile != null) {
                    val driveHelper = GoogleDriveHelper(context, account)
                    val success = driveHelper.uploadBackupFile(backupFile)
                    
                    if (success) {
                        val timestamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
                        val status = "Successful at $timestamp"
                        repository.saveSetting("last_backup_status", status)
                        _lastBackupStatus.value = status
                        _toastMessage.emit("Backup successful")
                        // Refresh the list after successful backup
                        fetchCloudBackups(context)
                    } else {
                        _toastMessage.emit("Upload failed. Check internet connection")
                    }
                    backupManager.cleanUp(backupFile)
                } else {
                    _toastMessage.emit("Failed to create backup file")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Error: ${e.message}")
            } finally {
                _backupInProgress.value = false
            }
        }
    }

    fun performRestore(context: Context) {
        val account = GoogleAuthHelper.getSignedInAccount(context)
        if (account == null) {
            viewModelScope.launch { _toastMessage.emit("Please connect Google Account first") }
            return
        }

        viewModelScope.launch {
            _restoreInProgress.value = true
            try {
                val driveHelper = GoogleDriveHelper(context, account)
                val backups = driveHelper.listBackupFiles()
                
                if (backups.isEmpty()) {
                    _toastMessage.emit("No backup found on Google Drive")
                    return@launch
                }

                val latestBackup = backups.first() // List is ordered by createdTime desc
                val tempFile = File(context.cacheDir, "temp_restore.zip")
                
                val downloadSuccess = driveHelper.downloadBackupFile(latestBackup.id, tempFile)
                if (downloadSuccess) {
                    val backupManager = BackupManager(context)
                    
                    // 1. Close database
                    val app = context.applicationContext as RetailApp
                    app.closeDatabase()
                    
                    // 2. Restore files
                    val restoreSuccess = backupManager.restoreFromBackup(tempFile)
                    
                    if (restoreSuccess) {
                        _restoreSuccessful.value = true
                    } else {
                        _toastMessage.emit("Failed to extract backup files")
                    }
                    backupManager.cleanUp(tempFile)
                } else {
                    _toastMessage.emit("Failed to download backup from Google Drive")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Restore Error: ${e.message}")
            } finally {
                _restoreInProgress.value = false
            }
        }
    }

    fun fetchCloudBackups(context: Context) {
        val account = GoogleAuthHelper.getSignedInAccount(context)
        if (account == null) return

        viewModelScope.launch {
            _isFetchingBackups.value = true
            try {
                val driveHelper = GoogleDriveHelper(context, account)
                _cloudBackups.value = driveHelper.listBackupFiles()
            } catch (e: Exception) {
                _toastMessage.emit("Failed to fetch backups: ${e.message}")
            } finally {
                _isFetchingBackups.value = false
            }
        }
    }

    fun saveSettings(name: String, address: String, gst: String) {
        viewModelScope.launch {
            repository.saveSetting("shop_name", name)
            repository.saveSetting("shop_address", address)
            repository.saveSetting("shop_gst", gst)
            _shopName.value = name
            _shopAddress.value = address
            _shopGst.value = gst
        }
    }

    fun dismissRestoreSuccess() {
        _restoreSuccessful.value = false
    }
}

class SettingsViewModelFactory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
