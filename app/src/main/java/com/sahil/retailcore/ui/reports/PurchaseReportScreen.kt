package com.sahil.retailcore.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
fun PurchaseReportScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as RetailApp
    val viewModel: ReportViewModel = viewModel(
        factory = ReportViewModelFactory(
            app.saleRepository, app.purchaseRepository, app.productRepository,
            app.customerRepository, app.supplierRepository, app.expenseRepository
        )
    )

    val purchases by viewModel.purchaseReport.collectAsState()
    val totalPurchases = purchases.sumOf { it.grandTotal }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Purchase Report") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Purchases", style = MaterialTheme.typography.labelMedium)
                    Text("₹${String.format(Locale.getDefault(), "%.2f", totalPurchases)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("${purchases.size} Invoices", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(purchases) { purchase ->
                    PurchaseReportRow(purchase)
                }
            }
        }
    }
}

@Composable
fun PurchaseReportRow(purchase: PurchaseEntity) {
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Inv: ${purchase.invoiceNumber}", fontWeight = FontWeight.Bold)
                Text(dateFormat.format(Date(purchase.createdAt)), style = MaterialTheme.typography.bodySmall)
            }
            Text("₹${String.format(Locale.getDefault(), "%.2f", purchase.grandTotal)}", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        }
    }
}
