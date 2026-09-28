package com.sahil.retailcore.ui.purchase

import android.app.DatePickerDialog
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahil.retailcore.RetailApp
import com.sahil.retailcore.data.local.entity.ProductEntity
import com.sahil.retailcore.data.local.entity.SupplierEntity
import com.sahil.retailcore.data.local.entity.PurchaseEntity
import com.sahil.retailcore.data.local.entity.PurchaseItemEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPurchaseScreen(
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

    val suppliers by viewModel.suppliers.collectAsState()
    val allProducts by viewModel.products.collectAsState()

    var supplierName by remember { mutableStateOf("") }
    var supplierPhone by remember { mutableStateOf("") }
    var supplierGst by remember { mutableStateOf("") }
    
    var invoiceNumber by remember { mutableStateOf("") }
    var invoiceDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var notes by remember { mutableStateOf("") }
    var amountPaid by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("Cash") }
    val purchaseItems = remember { mutableStateListOf<PurchaseItemUiState>() }

    var showSupplierMenu by remember { mutableStateOf(false) }
    var showProductDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val subtotal = purchaseItems.sumOf { it.purchasePriceWithoutGST * it.quantity }
    val totalGST = purchaseItems.sumOf { (it.purchasePriceWithoutGST * it.quantity * it.gstPercent) / 100.0 }
    val grandTotal = subtotal + totalGST

    LaunchedEffect(grandTotal) {
        if (amountPaid.isEmpty()) {
            amountPaid = String.format("%.2f", grandTotal)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Purchase") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.savePurchaseWithSupplier(
                                context,
                                supplierName = supplierName,
                                supplierPhone = supplierPhone,
                                supplierGst = supplierGst.ifBlank { null },
                                invoiceNumber = invoiceNumber,
                                invoiceDate = invoiceDate,
                                items = purchaseItems,
                                notes = notes,
                                amountPaid = amountPaid.toDoubleOrNull() ?: 0.0,
                                paymentMode = paymentMode
                            ) { success, _ -> if (success) onNavigateBack() }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text("Save") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Supplier Details", style = MaterialTheme.typography.titleMedium)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = showSupplierMenu,
                            onExpandedChange = { showSupplierMenu = !showSupplierMenu }
                        ) {
                            OutlinedTextField(
                                value = if (supplierName.isEmpty()) "Select Existing" else supplierName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Existing Supplier") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSupplierMenu) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = showSupplierMenu, onDismissRequest = { showSupplierMenu = false }) {
                                DropdownMenuItem(text = { Text("New Supplier") }, onClick = { 
                                    supplierName = ""; supplierPhone = ""; supplierGst = ""; showSupplierMenu = false 
                                })
                                suppliers.forEach { s ->
                                    DropdownMenuItem(text = { Text(s.name) }, onClick = { 
                                        supplierName = s.name; supplierPhone = s.phone; supplierGst = s.gstNumber ?: ""; showSupplierMenu = false 
                                    })
                                }
                            }
                        }
                        OutlinedTextField(value = supplierName, onValueChange = { supplierName = it }, label = { Text("Supplier Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = supplierPhone, onValueChange = { supplierPhone = it }, label = { Text("Mobile Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = invoiceNumber, onValueChange = { invoiceNumber = it }, label = { Text("Invoice Number") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = dateFormat.format(Date(invoiceDate)),
                        onValueChange = {},
                        label = { Text("Date") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = invoiceDate
                                DatePickerDialog(context, { _, y, m, d ->
                                    calendar.set(y, m, d)
                                    invoiceDate = calendar.timeInMillis
                                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                            }) { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Products", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { showProductDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add Product")
                    }
                }
            }

            items(purchaseItems) { item ->
                PurchaseItemRow(item, onRemove = { purchaseItems.remove(item) })
            }

            item {
                HorizontalDivider()
                SummaryRow("Subtotal", "₹${String.format("%.2f", subtotal)}")
                SummaryRow("Total GST", "₹${String.format("%.2f", totalGST)}")
                OutlinedTextField(
                    value = amountPaid,
                    onValueChange = { amountPaid = it },
                    label = { Text("Amount Paid") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                SummaryRow("Grand Total", "₹${String.format("%.2f", grandTotal)}", isBold = true)
                val outstanding = grandTotal - (amountPaid.toDoubleOrNull() ?: 0.0)
                if (outstanding > 0.01) {
                    Text("Outstanding: ₹${String.format("%.2f", outstanding)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Text("Payment Mode", style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Cash", "UPI", "Card", "Credit").forEach { mode ->
                        FilterChip(selected = paymentMode == mode, onClick = { paymentMode = mode }, label = { Text(mode) })
                    }
                }
            }
        }

        if (showProductDialog) {
            ProductSelectionDialog(
                products = allProducts,
                onDismiss = { showProductDialog = false },
                onProductSelected = { product, qty, price, gst ->
                    purchaseItems.add(PurchaseItemUiState(product.id, product.name, qty, price, gst))
                    showProductDialog = false
                }
            )
        }
    }
}

@Composable
fun PurchaseItemRow(item: PurchaseItemUiState, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.productName, fontWeight = FontWeight.Bold)
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
            Text("Qty: ${item.quantity} | Price: ₹${item.purchasePriceWithoutGST} | GST: ${item.gstPercent}%")
            val total = (item.purchasePriceWithoutGST * item.quantity) * (1 + item.gstPercent / 100.0)
            Text("Total: ₹${String.format("%.2f", total)}", modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSelectionDialog(
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onProductSelected: (ProductEntity, Int, Double, Double) -> Unit
) {
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var quantity by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("") }
    var gst by remember { mutableStateOf("18") }
    var showMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = showMenu, onExpandedChange = { showMenu = !showMenu }) {
                    OutlinedTextField(value = selectedProduct?.name ?: "Select Product", onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMenu) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        products.forEach { p ->
                            DropdownMenuItem(text = { Text(p.name) }, onClick = {
                                selectedProduct = p
                                price = p.purchasePrice.toString()
                                showMenu = false
                            })
                        }
                    }
                }
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Purchase Price") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = gst, onValueChange = { gst = it }, label = { Text("GST %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = {
            Button(onClick = {
                selectedProduct?.let { onProductSelected(it, quantity.toIntOrNull() ?: 1, price.toDoubleOrNull() ?: 0.0, gst.toDoubleOrNull() ?: 0.0) }
            }, enabled = selectedProduct != null) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
