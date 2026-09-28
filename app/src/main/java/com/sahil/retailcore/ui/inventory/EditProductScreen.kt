package com.sahil.retailcore.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahil.retailcore.RetailApp
import com.sahil.retailcore.data.local.entity.ProductEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    productId: String,
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

    var product by remember { mutableStateOf<ProductEntity?>(null) }
    
    // Form fields
    var productName by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var modelNumber by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var hsnCode by remember { mutableStateOf("") }
    var warranty by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var minStock by remember { mutableStateOf("") }
    var gstPercent by remember { mutableStateOf("") }

    LaunchedEffect(productId) {
        viewModel.getProductById(productId)?.let { p ->
            product = p
            productName = p.name
            brand = p.brand ?: ""
            category = p.categoryId
            modelNumber = p.modelNumber
            barcode = p.sku ?: ""
            hsnCode = p.hsnCode ?: ""
            warranty = p.warrantyMonths.toString()
            purchasePrice = p.purchasePrice.toString()
            sellingPrice = p.sellingPrice.toString()
            minStock = p.minimumStock.toString()
            gstPercent = p.gstPercent.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Product") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        product?.let { p ->
                            val updated = p.copy(
                                name = productName,
                                brand = brand,
                                categoryId = category,
                                modelNumber = modelNumber,
                                sku = barcode,
                                hsnCode = hsnCode,
                                warrantyMonths = warranty.toIntOrNull() ?: 0,
                                purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                                sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0,
                                minimumStock = minStock.toIntOrNull() ?: 1,
                                gstPercent = gstPercent.toDoubleOrNull() ?: 18.0
                            )
                            viewModel.updateProductDetails(updated) { success, _ ->
                                if (success) onNavigateBack()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        if (product == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(value = productName, onValueChange = { productName = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = modelNumber, onValueChange = { modelNumber = it }, label = { Text("Model Number") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode") }, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = hsnCode, onValueChange = { hsnCode = it }, label = { Text("HSN Code") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = gstPercent, onValueChange = { gstPercent = it }, label = { Text("GST %") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = purchasePrice, onValueChange = { purchasePrice = it }, label = { Text("Purchase Price") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        OutlinedTextField(value = sellingPrice, onValueChange = { sellingPrice = it }, label = { Text("Selling Price") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = warranty, onValueChange = { warranty = it }, label = { Text("Warranty (M)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = minStock, onValueChange = { minStock = it }, label = { Text("Min Stock") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                item {
                    Text("Stock Quantity: ${product!!.stockQuantity} (Cannot be edited directly)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
