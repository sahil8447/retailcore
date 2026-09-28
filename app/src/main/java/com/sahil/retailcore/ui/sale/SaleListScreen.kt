package com.sahil.retailcore.ui.sale

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahil.retailcore.RetailApp
import com.sahil.retailcore.data.local.entity.SaleEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleListScreen(
    onNavigateToNewSale: () -> Unit,
    onNavigateToSaleDetail: (String) -> Unit
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

    val sales by viewModel.sales.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Sales") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToNewSale) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        val context = LocalContext.current
        val app = context.applicationContext as RetailApp
        val customerRepository = remember { app.customerRepository }
        
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sales) { sale ->
                var customerName by remember { mutableStateOf<String?>(null) }
                var customerPhone by remember { mutableStateOf<String?>(null) }
                
                LaunchedEffect(sale.customerId) {
                    sale.customerId?.let { id ->
                        val customer = customerRepository.getCustomerById(id)
                        customerName = customer?.name
                        customerPhone = customer?.phone
                    }
                }
                
                SaleCard(
                    sale = sale,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    onClick = { onNavigateToSaleDetail(sale.id) },
                    onShare = { viewModel.shareInvoice(context, it) },
                    onPrint = { viewModel.printInvoice(context, it) }
                )
            }
        }
    }
}

@Composable
fun SaleCard(
    sale: SaleEntity, 
    customerName: String?, 
    customerPhone: String?, 
    onClick: () -> Unit, 
    onShare: (SaleEntity) -> Unit, 
    onPrint: (SaleEntity) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(sale.invoiceNumber.ifBlank { "Sale #${sale.id.take(8).uppercase()}" }, fontWeight = FontWeight.Bold)
                    Text(dateFormat.format(Date(sale.createdAt)), style = MaterialTheme.typography.bodySmall)
                }
                Text("₹${String.format("%.2f", sale.totalAmount)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (customerName != null) {
                Text(customerName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(customerPhone ?: "", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Cash Customer", style = MaterialTheme.typography.bodyMedium)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (sale.invoicePdfPath != null) {
                    IconButton(onClick = { onShare(sale) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { onPrint(sale) }) {
                        Icon(Icons.Default.Print, contentDescription = "Print", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
