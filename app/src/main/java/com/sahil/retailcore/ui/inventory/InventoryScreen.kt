package com.sahil.retailcore.ui.inventory

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahil.retailcore.RetailApp

private fun findActivity(context: android.content.Context): ComponentActivity {
    var currentContext = context
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is ComponentActivity) return currentContext
        currentContext = currentContext.baseContext
    }
    throw IllegalStateException("No activity found")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateToAddInventory: () -> Unit,
    onNavigateToStockHistory: () -> Unit,
    onNavigateToProductDetail: (String) -> Unit
) {
    val context = LocalContext.current.applicationContext as RetailApp
    val productViewModel: ProductViewModel = viewModel(
        viewModelStoreOwner = findActivity(LocalContext.current),
        factory = ProductViewModelFactory(context.productRepository)
    )

    val searchQuery by productViewModel.searchQuery.collectAsState()
    val products by productViewModel.products.collectAsState()
    
    val categories = listOf("All", "Refrigerator", "Washing Machine", "LED TV", "Cooler", "Fan", "Geyser", "Induction", "Mixer", "Juicer", "Air Conditioner", "Microwave", "Water Purifier", "Chimney", "Iron", "Air Cooler", "Other")
    var selectedCategory by remember { mutableStateOf("All") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory") },
                actions = {
                    IconButton(onClick = onNavigateToStockHistory) {
                        Icon(Icons.Default.History, contentDescription = "Stock History")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddInventory,
                containerColor = MaterialTheme.colorScheme.primary,
                icon = { Icon(Icons.Default.Add, "Add Inventory") },
                text = { Text("Add Inventory") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { productViewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by Name, Model, Brand, Barcode") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )

            // Category Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Product List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                val filteredProducts = if (selectedCategory == "All") products else products.filter { it.categoryId == selectedCategory }
                
                items(filteredProducts, key = { it.id }) { product ->
                    Box(modifier = Modifier.clickable { onNavigateToProductDetail(product.id) }) {
                        ProductCard(
                            product = product,
                            onEditClick = {
                                productViewModel.setSelectedProduct(product.id)
                                // We might still want to edit basic product info. 
                                // For now, clicking card goes to detail.
                            },
                            onDeleteClick = { productViewModel.deleteProduct(product.id) }
                        )
                    }
                }
            }
        }
    }
}
