package com.sahil.retailcore.ui.inventory

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahil.retailcore.RetailApp
import com.sahil.retailcore.data.local.entity.ProductEntity
import com.sahil.retailcore.data.local.entity.SupplierEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInventoryScreen(
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

    val suppliers by viewModel.allSuppliers.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    val filteredProducts by viewModel.getFilteredProducts(searchQuery).collectAsState(initial = emptyList())

    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var isOpeningStock by remember { mutableStateOf(false) }

    // Common/Template Fields
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var warranty by remember { mutableStateOf("0") }
    var hsnCode by remember { mutableStateOf("") }
    var minStock by remember { mutableStateOf("1") }
    var gstPercent by remember { mutableStateOf("18") }
    
    // Multi-model state
    val modelItems = remember { mutableStateListOf(InventoryModelItem()) }

    // Inventory/Transaction Fields
    var amountPaid by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("Cash") }
    var selectedSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var invoiceNumber by remember { mutableStateOf("") }
    var entryDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var notes by remember { mutableStateOf("") }

    val categories = listOf("Refrigerator", "Washing Machine", "LED TV", "Cooler", "Fan", "Geyser", "Induction", "Mixer", "Juicer", "Air Conditioner", "Microwave", "Water Purifier", "Chimney", "Iron", "Air Cooler", "Other")
    val brands = listOf("LG", "Samsung", "Whirlpool", "Haier", "Voltas", "Godrej", "Panasonic", "IFB", "Bosch", "Philips", "Prestige", "Bajaj", "Havells", "Orient", "Crompton", "Usha", "Other")

    var showCategoryMenu by remember { mutableStateOf(false) }
    var showBrandMenu by remember { mutableStateOf(false) }
    var showSupplierMenu by remember { mutableStateOf(false) }
    
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Calculation for grand total
    val lineTotal = if (selectedProduct != null) {
        val qty = modelItems.firstOrNull()?.quantity?.toIntOrNull() ?: 0
        val price = modelItems.firstOrNull()?.purchasePrice?.toDoubleOrNull() ?: 0.0
        val gst = selectedProduct?.gstPercent ?: 18.0
        (price * qty) * (1 + (gst / 100.0))
    } else {
        modelItems.sumOf { 
            val qty = it.quantity.toIntOrNull() ?: 0
            val price = it.purchasePrice.toDoubleOrNull() ?: 0.0
            (price * qty) * (1 + (gstPercent.toDoubleOrNull() ?: 0.0) / 100.0) 
        }
    }

    LaunchedEffect(lineTotal, isOpeningStock) {
        if (isOpeningStock) {
            amountPaid = "0.0"
        } else if (amountPaid.isEmpty() || amountPaid == "0.0") {
            amountPaid = String.format("%.2f", lineTotal)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Inventory") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (selectedProduct != null) {
                                val item = modelItems.first()
                                viewModel.saveExistingProductStock(
                                    product = selectedProduct!!,
                                    isOpeningStock = isOpeningStock,
                                    quantity = item.quantity.toIntOrNull() ?: 1,
                                    purchasePrice = item.purchasePrice.toDoubleOrNull() ?: 0.0,
                                    supplierName = selectedSupplier?.name,
                                    supplierPhone = selectedSupplier?.phone,
                                    supplierGst = selectedSupplier?.gstNumber,
                                    amountPaid = amountPaid.toDoubleOrNull() ?: 0.0,
                                    paymentMode = paymentMode,
                                    notes = notes,
                                    entryDate = entryDate
                                ) { success, _ -> if (success) onNavigateBack() }
                            } else {
                                val commonInfo = ProductEntity(
                                    id = "",
                                    name = "",
                                    brand = brand,
                                    categoryId = category,
                                    modelNumber = "",
                                    warrantyMonths = warranty.toIntOrNull() ?: 0,
                                    hsnCode = hsnCode,
                                    minimumStock = minStock.toIntOrNull() ?: 1,
                                    gstPercent = gstPercent.toDoubleOrNull() ?: 18.0,
                                    stockQuantity = 0,
                                    purchasePrice = 0.0,
                                    sellingPrice = 0.0,
                                    sku = ""
                                )
                                viewModel.saveMultiModelInventory(
                                    commonProductInfo = commonInfo,
                                    isOpeningStock = isOpeningStock,
                                    models = modelItems.toList(),
                                    supplierName = selectedSupplier?.name,
                                    supplierPhone = selectedSupplier?.phone,
                                    supplierGst = selectedSupplier?.gstNumber,
                                    amountPaid = amountPaid.toDoubleOrNull() ?: 0.0,
                                    paymentMode = paymentMode,
                                    entryDate = entryDate
                                ) { success, _ -> if (success) onNavigateBack() }
                            }
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
            // Search existing
            item {
                SearchProductField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    onProductSelected = { p ->
                        selectedProduct = p
                        searchQuery = ""
                        // Pre-populate multi-model list with 1 item using existing product prices
                        modelItems.clear()
                        modelItems.add(InventoryModelItem(
                            modelNumber = p.modelNumber,
                            barcode = p.sku ?: "",
                            quantity = "1",
                            purchasePrice = p.purchasePrice.toString(),
                            sellingPrice = p.sellingPrice.toString()
                        ))
                    },
                    filteredProducts = filteredProducts
                )
            }

            // Mode Selector
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Opening Stock", modifier = Modifier.weight(1f))
                    Switch(checked = isOpeningStock, onCheckedChange = { isOpeningStock = it })
                }
            }

            if (selectedProduct != null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selectedProduct!!.name, fontWeight = FontWeight.Bold)
                                Text("${selectedProduct!!.brand} | ${selectedProduct!!.modelNumber}", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { selectedProduct = null; modelItems.clear(); modelItems.add(InventoryModelItem()) }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    }
                }
            } else {
                // New Product Template Fields
                item { Text("Common Details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = showBrandMenu,
                            onExpandedChange = { showBrandMenu = !showBrandMenu },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = brand, onValueChange = { brand = it }, label = { Text("Brand") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showBrandMenu) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = showBrandMenu, onDismissRequest = { showBrandMenu = false }) {
                                brands.forEach { b -> DropdownMenuItem(text = { Text(b) }, onClick = { brand = b; showBrandMenu = false }) }
                            }
                        }
                        ExposedDropdownMenuBox(
                            expanded = showCategoryMenu,
                            onExpandedChange = { showCategoryMenu = !showCategoryMenu },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = category, onValueChange = { category = it }, label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                                categories.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { category = c; showCategoryMenu = false }) }
                            }
                        }
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = gstPercent, onValueChange = { gstPercent = it }, label = { Text("GST %") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        OutlinedTextField(value = hsnCode, onValueChange = { hsnCode = it }, label = { Text("HSN") }, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = warranty, onValueChange = { warranty = it }, label = { Text("Warranty (M)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = minStock, onValueChange = { minStock = it }, label = { Text("Min Stock") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }

                item { HorizontalDivider() }
                item { Text("Models", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                
                items(modelItems) { item ->
                    ModelEntryRow(
                        item = item,
                        onUpdate = { updated ->
                            val index = modelItems.indexOf(item)
                            if (index != -1) modelItems[index] = updated
                        },
                        onRemove = { if (modelItems.size > 1) modelItems.remove(item) }
                    )
                }

                item {
                    TextButton(onClick = { 
                        val lastItem = modelItems.lastOrNull()
                        modelItems.add(InventoryModelItem(
                            purchasePrice = lastItem?.purchasePrice ?: "",
                            sellingPrice = lastItem?.sellingPrice ?: ""
                        )) 
                    }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Another Model")
                    }
                }
            }

            if (selectedProduct != null) {
                // When product exists, we only show Quantity/Price for that one model
                item {
                    val item = modelItems.first()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = item.quantity, onValueChange = { modelItems[0] = item.copy(quantity = it) }, label = { Text("Quantity") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = item.purchasePrice, onValueChange = { modelItems[0] = item.copy(purchasePrice = it) }, label = { Text("Purchase Price") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    }
                }
            }

            item { HorizontalDivider() }
            item { Text("Inventory Details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }

            if (!isOpeningStock) {
                item {
                    ExposedDropdownMenuBox(
                        expanded = showSupplierMenu,
                        onExpandedChange = { showSupplierMenu = !showSupplierMenu }
                    ) {
                        OutlinedTextField(
                            value = selectedSupplier?.name ?: "Select Supplier",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Supplier") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSupplierMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = showSupplierMenu, onDismissRequest = { showSupplierMenu = false }) {
                            suppliers.forEach { s -> DropdownMenuItem(text = { Text(s.name) }, onClick = { selectedSupplier = s; showSupplierMenu = false }) }
                        }
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = invoiceNumber, onValueChange = { invoiceNumber = it }, label = { Text("Invoice No") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = dateFormat.format(Date(entryDate)), onValueChange = {}, label = { Text("Date") }, readOnly = true,
                            trailingIcon = { IconButton(onClick = { /* DatePicker */ }) { Icon(Icons.Default.CalendarToday, null) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(value = amountPaid, onValueChange = { amountPaid = it }, label = { Text("Amount Paid") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total (Incl. GST): ₹${String.format("%.2f", lineTotal)}", fontWeight = FontWeight.Bold)
                            val outstanding = lineTotal - (amountPaid.toDoubleOrNull() ?: 0.0)
                            Text("Outstanding: ₹${String.format("%.2f", outstanding)}", color = if (outstanding > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
        }
    }
}

@Composable
fun SearchProductField(
    value: String,
    onValueChange: (String) -> Unit,
    onProductSelected: (ProductEntity) -> Unit,
    filteredProducts: List<ProductEntity>
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Search Existing Product") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (value.isNotBlank()) IconButton(onClick = { onValueChange("") }) { Icon(Icons.Default.Clear, null) } }
        )
        if (value.isNotBlank()) {
            Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 4.dp, shape = MaterialTheme.shapes.small) {
                Column {
                    filteredProducts.forEach { p ->
                        ListItem(
                            headlineContent = { Text(p.name) },
                            supportingContent = { Text("${p.brand} | ${p.modelNumber}") },
                            modifier = Modifier.clickable { onProductSelected(p) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModelEntryRow(
    item: InventoryModelItem,
    onUpdate: (InventoryModelItem) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), border = CardDefaults.outlinedCardBorder()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Model Details", style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
            }
            OutlinedTextField(value = item.modelNumber, onValueChange = { onUpdate(item.copy(modelNumber = it)) }, label = { Text("Model Number") }, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = item.barcode, onValueChange = { onUpdate(item.copy(barcode = it)) }, label = { Text("Barcode") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = item.quantity, onValueChange = { onUpdate(item.copy(quantity = it)) }, label = { Text("Qty") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = item.purchasePrice, onValueChange = { onUpdate(item.copy(purchasePrice = it)) }, label = { Text("Purchase Price") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = item.sellingPrice, onValueChange = { onUpdate(item.copy(sellingPrice = it)) }, label = { Text("Selling Price") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        }
    }
}
