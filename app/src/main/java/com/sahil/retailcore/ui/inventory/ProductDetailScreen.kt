package com.sahil.retailcore.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahil.retailcore.RetailApp
import com.sahil.retailcore.data.local.entity.*
import com.sahil.retailcore.ui.sale.SaleViewModel
import com.sahil.retailcore.ui.sale.SaleViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onNavigateToEdit: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as RetailApp
    val inventoryViewModel: InventoryViewModel = viewModel(
        factory = InventoryViewModelFactory(
            app.stockRepository,
            app.productRepository,
            app.supplierRepository
        )
    )
    val saleViewModel: SaleViewModel = viewModel(
        factory = SaleViewModelFactory(
            app.saleRepository,
            app.customerRepository,
            app.productRepository,
            app.settingsRepository
        )
    )

    var product by remember { mutableStateOf<ProductEntity?>(null) }
    val stockEntriesState = inventoryViewModel.getStockEntriesForProduct(productId).collectAsState(initial = emptyList())
    val recentSalesState = saleViewModel.getSaleItemsByProduct(productId).collectAsState(initial = emptyList())
    val suppliersState = inventoryViewModel.allSuppliers.collectAsState()

    val stockEntries = stockEntriesState.value
    val recentSales = recentSalesState.value
    val suppliers = suppliersState.value

    LaunchedEffect(productId) {
        product = inventoryViewModel.getProductById(productId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(productId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Product")
                    }
                }
            )
        }
    ) { padding ->
        product?.let { p ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(p.brand ?: "Brand", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(p.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Model: ${p.modelNumber}", style = MaterialTheme.typography.titleMedium)
                            Text("Category: ${p.categoryId}", style = MaterialTheme.typography.bodyMedium)
                            if (!p.sku.isNullOrBlank()) Text("Barcode: ${p.sku}", style = MaterialTheme.typography.bodySmall)
                            if (!p.hsnCode.isNullOrBlank()) Text("HSN: ${p.hsnCode}", style = MaterialTheme.typography.bodySmall)
                            Text("Warranty: ${p.warrantyMonths} Months", style = MaterialTheme.typography.bodySmall)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StockStat("Current Stock", p.stockQuantity.toString(), if (p.stockQuantity <= p.minimumStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                StockStat("Min Stock", p.minimumStock.toString(), MaterialTheme.colorScheme.outline)
                                val margin = p.sellingPrice - p.purchasePrice
                                val marginPct = if (p.sellingPrice > 0) (margin / p.sellingPrice * 100) else 0.0
                                StockStat("Profit Margin", "₹${String.format(Locale.getDefault(), "%.2f", margin)} (${String.format(Locale.getDefault(), "%.1f", marginPct)}%)", MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Purchase Price", style = MaterialTheme.typography.labelSmall)
                            Text("₹${p.purchasePrice}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text("Selling Price", style = MaterialTheme.typography.labelSmall)
                            Text("₹${p.sellingPrice}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Text("Recent Stock Entries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(stockEntries.take(5)) { entry ->
                    val supplier = suppliers.find { it.id == entry.supplierId }
                    StockEntryMiniCard(entry, supplier?.name ?: "Direct")
                }

                item {
                    Text("Recent Sales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(recentSales.take(5)) { saleItem ->
                    SaleItemMiniCard(saleItem)
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun StockStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StockEntryMiniCard(entry: StockEntryEntity, supplierName: String) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(dateFormat.format(Date(entry.entryDate)), fontWeight = FontWeight.Bold)
                Text("Supplier: $supplierName", style = MaterialTheme.typography.bodySmall)
                Text("Price: ₹${entry.purchasePrice}", style = MaterialTheme.typography.bodySmall)
            }
            Text("+${entry.quantity}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun SaleItemMiniCard(item: SaleItemEntity) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(dateFormat.format(Date(item.createdAt)), fontWeight = FontWeight.Bold)
                Text("Sale ID: ${item.saleId.take(8).uppercase()}", style = MaterialTheme.typography.bodySmall)
                Text("Price: ₹${item.unitPrice}", style = MaterialTheme.typography.bodySmall)
            }
            Text("-${item.quantity}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
    }
}
