package com.sahil.retailcore.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.sahil.retailcore.RetailApp
import com.sahil.retailcore.utils.GoogleAuthHelper
import android.widget.Toast
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as RetailApp
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(app.settingsRepository)
    )

    val shopName by viewModel.shopName.collectAsState()
    val shopAddress by viewModel.shopAddress.collectAsState()
    val shopGst by viewModel.shopGst.collectAsState()

    val googleAccountEmail by viewModel.googleAccountEmail.collectAsState()
    val lastBackupStatus by viewModel.lastBackupStatus.collectAsState()
    val backupInProgress by viewModel.backupInProgress.collectAsState()
    val restoreInProgress by viewModel.restoreInProgress.collectAsState()
    val restoreSuccessful by viewModel.restoreSuccessful.collectAsState()
    val cloudBackups by viewModel.cloudBackups.collectAsState()
    val isFetchingBackups by viewModel.isFetchingBackups.collectAsState()
    val nextBackupTime by viewModel.nextBackupTime.collectAsState()

    var name by remember(shopName) { mutableStateOf(shopName) }
    var address by remember(shopAddress) { mutableStateOf(shopAddress) }
    var gst by remember(shopGst) { mutableStateOf(shopGst) }

    var showBackupsDialog by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            viewModel.updateGoogleAccount(account, context)
        } catch (e: com.google.android.gms.common.api.ApiException) {
            val errorMessage = "Sign-in failed: Code ${e.statusCode}. " +
                when (e.statusCode) {
                    7 -> "SIGN_IN_FAILED: Likely configuration issue (SHA-1, package name, or google-services.json mismatch)."
                    10 -> "DEVELOPER_ERROR: Common when SHA-1 is not registered in Google Cloud Console."
                    12500 -> "SIGN_IN_CANCELLED: User cancelled or play services issue."
                    12501 -> "SIGN_IN_CANCELLED"
                    else -> e.message ?: "Unknown error"
                }
            android.util.Log.e("GoogleSignIn", errorMessage, e)
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.util.Log.e("GoogleSignIn", "Unexpected error during sign-in", e)
            Toast.makeText(context, "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkGoogleAccount(context)
        viewModel.toastMessage.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.saveSettings(name, address, gst)
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Shop Details", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Shop Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Shop Address") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = gst,
                    onValueChange = { gst = it },
                    label = { Text("GST Number") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                HorizontalDivider()
                Text("Google Drive Backup", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (googleAccountEmail == null) {
                            Button(
                                onClick = {
                                    val client = GoogleAuthHelper.getGoogleSignInClient(context)
                                    googleSignInLauncher.launch(client.signInIntent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connect Google Account")
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Account", style = MaterialTheme.typography.labelSmall)
                                    Text(googleAccountEmail!!, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = {
                                    val client = GoogleAuthHelper.getGoogleSignInClient(context)
                                    client.signOut().addOnCompleteListener {
                                        viewModel.updateGoogleAccount(null, context)
                                    }
                                }) {
                                    Text("Disconnect", color = MaterialTheme.colorScheme.error)
                                }
                            }

                            if (nextBackupTime != null) {
                                Text(
                                    text = "Next scheduled backup: $nextBackupTime",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Last Backup", style = MaterialTheme.typography.labelSmall)
                                    Text(lastBackupStatus)
                                }
                                if (backupInProgress) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Button(
                                        onClick = { viewModel.performBackup(context) },
                                        enabled = !restoreInProgress
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Backup Now")
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (restoreInProgress) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Restoring...", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.performRestore(context) },
                                        enabled = !backupInProgress,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Restore Latest")
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            TextButton(
                                onClick = {
                                    viewModel.fetchCloudBackups(context)
                                    showBackupsDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("View Cloud Backups (Dev Only)")
                            }
                        }
                    }
                }
                
                Text(
                    "Backups are stored securely in your private Google Drive app folder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    if (showBackupsDialog) {
        AlertDialog(
            onDismissRequest = { showBackupsDialog = false },
            title = { Text("Cloud Backups") },
            text = {
                if (isFetchingBackups) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (cloudBackups.isEmpty()) {
                    Text("No backups found in appDataFolder.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(cloudBackups.size) { index ->
                            val file = cloudBackups[index]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(file.name ?: "Unknown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("ID: ${file.id ?: "N/A"}", style = MaterialTheme.typography.labelSmall)
                                    Text("Size: ${file.size ?: "Unknown"}", style = MaterialTheme.typography.labelSmall)
                                    Text("Date: ${file.createdTime?.toString() ?: "Unknown"}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (restoreSuccessful) {
        AlertDialog(
            onDismissRequest = { /* Non-dismissible */ },
            title = { Text("Restore Completed") },
            text = {
                Text("Your backup has been restored successfully.\nThe application needs to restart to load the restored data.")
            },
            confirmButton = {
                Button(onClick = {
                    val app = context.applicationContext as RetailApp
                    app.closeDatabase()
                    
                    val packageManager = context.packageManager
                    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                    val componentName = intent?.component
                    val mainIntent = android.content.Intent.makeRestartActivityTask(componentName)
                    context.startActivity(mainIntent)
                    Runtime.getRuntime().exit(0)
                }) {
                    Text("Restart Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRestoreSuccess() }) {
                    Text("Cancel")
                }
            }
        )
    }
}
