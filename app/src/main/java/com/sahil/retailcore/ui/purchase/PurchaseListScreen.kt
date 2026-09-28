package com.sahil.retailcore.ui.purchase

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahil.retailcore.RetailApp
import com.sahil.retailcore.data.local.entity.PurchaseEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseListScreen(
    onNavigateToNewPurchase: () -> Unit,
    onNavigateToPurchaseDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as RetailApp
    val viewModel: PurchaseViewModel = viewModel(
        factory = PurchaseViewModelFactory(
            app.purchaseRepository,
            app.supplierRepository,
            app.productRepository,
            app.settingsRepository
        )
    )

    val purchases by viewModel.purchases.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Purchases") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToNewPurchase) {
                Icon(Icons.Default.Add, contentDescription = "New Purchase")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search Invoice...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(purchases) { purchase ->
                    val supplierName = suppliers.find { it.id == purchase.supplierId }?.name ?: "Unknown"
                    PurchaseItemCard(
                        purchase = purchase,
                        supplierName = supplierName,
                        onClick = { onNavigateToPurchaseDetail(purchase.id) },
                        onShare = { viewModel.shareInvoice(context, it) },
                        onPrint = { viewModel.printInvoice(context, it) }
                    )
                }
            }
        }
    }
}

@Composable
fun PurchaseItemCard(
    purchase: PurchaseEntity,
    supplierName: String,
    onClick: () -> Unit,
    onShare: (PurchaseEntity) -> Unit,
    onPrint: (PurchaseEntity) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Inv: ${purchase.invoiceNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₹${purchase.grandTotal}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = supplierName,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = dateFormat.format(Date(purchase.invoiceDate)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = purchase.paymentStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (purchase.paymentStatus == "Paid") 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
                
                if (purchase.invoicePdfPath != null) {
                    Row {
                        IconButton(onClick = { onShare(purchase) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onPrint(purchase) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Print, contentDescription = "Print", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
