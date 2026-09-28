package com.sahil.retailcore.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahil.retailcore.RetailApp
import com.sahil.retailcore.data.local.entity.StockEntryEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockHistoryScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as RetailApp
    val viewModel: InventoryViewModel = viewModel(
        factory = InventoryViewModelFactory(
            app.stockRepository,
            app.productRepository,
            app.supplierRepository
        )
    )

    val entries by viewModel.stockEntries.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val suppliers by viewModel.allSuppliers.collectAsState()

    var entryToDelete by remember { mutableStateOf<StockEntryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries) { entry ->
                val product = products.find { it.id == entry.productId }
                val supplier = suppliers.find { it.id == entry.supplierId }
                StockEntryCard(
                    entry = entry,
                    productName = product?.name ?: "Unknown",
                    supplierName = supplier?.name ?: "None",
                    onDelete = { entryToDelete = entry }
                )
            }
        }

        entryToDelete?.let { entry ->
            AlertDialog(
                onDismissRequest = { entryToDelete = null },
                title = { Text("Delete Stock Entry") },
                text = { Text("Are you sure you want to delete this stock entry? This will reduce the product stock.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteStockEntry(entry.id) { _, _ -> }
                        entryToDelete = null
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { entryToDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun StockEntryCard(
    entry: StockEntryEntity,
    productName: String,
    supplierName: String,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(productName, fontWeight = FontWeight.Bold)
                Text("Qty: ${entry.quantity}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Text("Date: ${dateFormat.format(Date(entry.entryDate))}", style = MaterialTheme.typography.bodySmall)
            Text("Price: ₹${entry.purchasePrice} (+ ${entry.gstPercent}% GST)", style = MaterialTheme.typography.bodySmall)
            Text("Supplier: $supplierName", style = MaterialTheme.typography.bodySmall)
            if (!entry.notes.isNullOrBlank()) {
                Text("Notes: ${entry.notes}", style = MaterialTheme.typography.bodySmall)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
