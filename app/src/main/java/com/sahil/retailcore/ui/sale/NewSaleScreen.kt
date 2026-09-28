package com.sahil.retailcore.ui.sale

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahil.retailcore.RetailApp
import com.sahil.retailcore.data.local.entity.PaymentEntity
import com.sahil.retailcore.data.local.entity.PaymentType
import com.sahil.retailcore.data.local.entity.ProductEntity
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSaleScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as RetailApp
    val viewModel: SaleViewModel = viewModel(
        factory = SaleViewModelFactory(
            app.saleRepository,
            app.customerRepository,
            app.productRepository,
            app.settingsRepository
        )
    )

    // Customer State
    var customerPhone by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var customerGst by remember { mutableStateOf("") }

    // Bill State
    val saleItems = remember { mutableStateListOf<SaleItemUiState>() }
    var totalDiscount by remember { mutableStateOf("") }
    
    // UI State
    var showProductSearch by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }
    val allProducts by viewModel.products.collectAsState()

    // Payment State (Split Payment)
    val paymentModes = listOf("Cash", "UPI", "Card", "Bank Transfer")
    val paymentAmounts = remember { mutableStateMapOf<String, String>().apply {
        if (isEmpty()) { paymentModes.forEach { put(it, "") } }
    } }

    val subtotal = saleItems.sumOf { it.sellingPrice * it.quantity }
    val totalTax = saleItems.sumOf { (it.sellingPrice * it.quantity * it.gstPercent) / 100.0 }
    val grandTotal = subtotal + totalTax - (totalDiscount.toDoubleOrNull() ?: 0.0)
    
    val totalReceived = paymentAmounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
    val outstanding = grandTotal - totalReceived

    // Auto-load customer
    LaunchedEffect(customerPhone) {
        if (customerPhone.length >= 10) {
            viewModel.getCustomerByPhone(customerPhone)?.let {
                customerName = it.name
                customerAddress = it.address ?: ""
                customerGst = it.gstNumber ?: ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Sale") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showSummary = true }, enabled = saleItems.isNotEmpty()) {
                        Text("Checkout", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showProductSearch = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Product") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Customer Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Customer Details", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("Mobile Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Phone, null) }
                        )
                        AnimatedVisibility(visible = customerPhone.length >= 10) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Customer Name") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = customerAddress, onValueChange = { customerAddress = it }, label = { Text("Address (Optional)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = customerGst, onValueChange = { customerGst = it }, label = { Text("GST Number (Optional)") }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }

            // Cart Items
            item {
                Text("Items (${saleItems.size})", style = MaterialTheme.typography.titleSmall)
            }

            items(saleItems) { item ->
                SaleItemCard(
                    item = item,
                    onUpdate = { updated ->
                        val index = saleItems.indexOf(item)
                        saleItems[index] = updated
                    },
                    onRemove = { saleItems.remove(item) }
                )
            }

            // Summary Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SaleSummaryRow("Subtotal", "₹${String.format(Locale.getDefault(), "%.2f", subtotal)}")
                        SaleSummaryRow("GST", "₹${String.format(Locale.getDefault(), "%.2f", totalTax)}")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Discount", modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = totalDiscount,
                                onValueChange = { totalDiscount = it },
                                modifier = Modifier.width(100.dp),
                                prefix = { Text("₹") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        SaleSummaryRow("Grand Total", "₹${String.format(Locale.getDefault(), "%.2f", grandTotal)}", isBold = true, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Payments Section
            item {
                Text("Payments", style = MaterialTheme.typography.titleSmall)
            }

            items(paymentModes) { mode ->
                val amountStr = paymentAmounts[mode] ?: ""
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(mode, modifier = Modifier.width(80.dp), fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { paymentAmounts[mode] = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Received", fontWeight = FontWeight.Bold)
                    Text("₹${String.format("%.2f", totalReceived)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                if (outstanding > 0.01) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Outstanding", color = MaterialTheme.colorScheme.error)
                        Text("₹${String.format("%.2f", outstanding)}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showProductSearch) {
        ProductSearchDialog(
            products = allProducts,
            onDismiss = { showProductSearch = false },
            onProductSelected = { p ->
                val existing = saleItems.find { it.productId == p.id }
                if (existing != null) {
                    // Item already in cart
                } else {
                    saleItems.add(SaleItemUiState(
                        productId = p.id,
                        productName = p.name,
                        modelNumber = p.modelNumber,
                        brand = p.brand ?: "",
                        category = p.categoryId,
                        quantity = 1,
                        sellingPrice = p.sellingPrice,
                        gstPercent = p.gstPercent,
                        warranty = p.warrantyMonths,
                        availableStock = p.stockQuantity
                    ))
                }
                showProductSearch = false
            }
        )
    }

    if (showSummary) {
        AlertDialog(
            onDismissRequest = { showSummary = false },
            title = { Text("Confirm Sale") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Customer: $customerName")
                    Text("Total Items: ${saleItems.sumOf { it.quantity }}")
                    Text("Grand Total: ₹${String.format("%.2f", grandTotal)}", fontWeight = FontWeight.Bold)
                    Text("Amount Received: ₹${String.format("%.2f", totalReceived)}", color = MaterialTheme.colorScheme.primary)
                    if (outstanding > 0.01) {
                        Text("Balance Due: ₹${String.format("%.2f", outstanding)}", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val finalPayments = paymentAmounts.map { (mode, amount) ->
                        PaymentEntity(
                            id = UUID.randomUUID().toString(),
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            paymentMode = mode,
                            type = PaymentType.RECEIVED
                        )
                    }.filter { it.amount > 0 }

                    viewModel.saveSale(
                        context, customerName, customerPhone, customerAddress, customerGst,
                        saleItems, finalPayments, totalDiscount.toDoubleOrNull() ?: 0.0
                    ) { success, _ -> if (success) onNavigateBack() }
                }) { Text("Save & Generate Invoice") }
            },
            dismissButton = {
                TextButton(onClick = { showSummary = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SaleItemCard(
    item: SaleItemUiState,
    onUpdate: (SaleItemUiState) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(item.productName, fontWeight = FontWeight.Bold)
                    Text("${item.brand} | ${item.modelNumber}", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Quantity
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = { if (item.quantity > 1) onUpdate(item.copy(quantity = item.quantity - 1)) }) {
                        Icon(Icons.Default.Remove, null)
                    }
                    Text("${item.quantity}", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { 
                        if (item.quantity < item.availableStock) onUpdate(item.copy(quantity = item.quantity + 1)) 
                    }, enabled = item.quantity < item.availableStock) {
                        Icon(Icons.Default.Add, null)
                    }
                }
                
                // Price
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text("₹${item.sellingPrice}", fontWeight = FontWeight.Bold)
                    Text("GST: ${item.gstPercent}%", style = MaterialTheme.typography.labelSmall)
                }
            }
            
            if (item.availableStock <= 0) {
                Text("OUT OF STOCK", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            } else {
                Text("Stock available: ${item.availableStock}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSearchDialog(
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onProductSelected: (ProductEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    
    val filtered = products.filter {
        it.modelNumber.contains(query, ignoreCase = true) ||
        it.name.contains(query, ignoreCase = true) ||
        it.brand?.contains(query, ignoreCase = true) == true ||
        it.sku?.contains(query, ignoreCase = true) == true
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search Product") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered) { p ->
                        ListItem(
                            headlineContent = { Text(p.name, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("${p.brand} | ${p.modelNumber} | Stock: ${p.stockQuantity}") },
                            trailingContent = { Text("₹${p.sellingPrice}") },
                            modifier = Modifier.clickable { onProductSelected(p) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun SaleSummaryRow(label: String, value: String, isBold: Boolean = false, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, color = color)
    }
}
