package com.sahil.retailcore.ui.purchase

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahil.retailcore.RetailApp
import com.sahil.retailcore.data.local.entity.PurchaseEntity
import com.sahil.retailcore.data.local.entity.PurchaseItemEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailScreen(
    purchaseId: String,
    onNavigateBack: () -> Unit
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

    var purchase by remember { mutableStateOf<PurchaseEntity?>(null) }
    var items by remember { mutableStateOf<List<PurchaseItemEntity>>(emptyList()) }
    val suppliers by viewModel.suppliers.collectAsState()
    val allProducts by viewModel.products.collectAsState()

    LaunchedEffect(purchaseId) {
        val result = viewModel.getPurchaseWithItems(purchaseId)
        purchase = result.first
        items = result.second
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Purchase Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        purchase?.let { p ->
            val supplier = suppliers.find { it.id == p.supplierId }
            
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Invoice: ${p.invoiceNumber}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Supplier: ${supplier?.name ?: "Unknown"}")
                            Text("Date: ${dateFormat.format(Date(p.invoiceDate))}")
                            Text("Status: ${p.paymentStatus}", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                item {
                    Text("Products", style = MaterialTheme.typography.titleMedium)
                }

                items(items) { item ->
                    val product = allProducts.find { it.id == item.productId }
                    PurchaseItemDetailRow(item, product?.name ?: "Unknown Product")
                }

                item {
                    HorizontalDivider()
                    SummaryRow("Subtotal", "₹${String.format("%.2f", p.subtotal)}")
                    SummaryRow("Total GST", "₹${String.format("%.2f", p.totalGST)}")
                    SummaryRow("Grand Total", "₹${String.format("%.2f", p.grandTotal)}", isBold = true)
                }

                if (!p.notes.isNullOrBlank()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Notes", fontWeight = FontWeight.Bold)
                                Text(p.notes)
                            }
                        }
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun PurchaseItemDetailRow(item: PurchaseItemEntity, productName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(productName, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Qty: ${item.quantity}")
                Text("Rate: ₹${item.purchasePriceWithoutGST}")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("GST: ${item.gstPercent}% (₹${String.format("%.2f", item.gstAmount * item.quantity)})")
                Text("Total: ₹${String.format("%.2f", item.lineTotal)}")
            }
        }
    }
}
