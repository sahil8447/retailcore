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
import com.sahil.retailcore.data.local.entity.SaleEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as RetailApp
    val viewModel: ReportViewModel = viewModel(
        factory = ReportViewModelFactory(
            app.saleRepository, app.purchaseRepository, app.productRepository,
            app.customerRepository, app.supplierRepository, app.expenseRepository
        )
    )

    val sales by viewModel.salesReport.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    
    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    val totalSales = sales.sumOf { it.totalAmount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Report") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* PDF Export Logic */ }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                    }
                    IconButton(onClick = { /* Excel Export Logic */ }) {
                        Icon(Icons.Default.TableChart, contentDescription = "Export Excel")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            // Date Filter Header
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Period", style = MaterialTheme.typography.labelSmall)
                        Text("${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}", fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { /* Show Date Range Picker */ }) {
                        Text("Change")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Total Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Sales", style = MaterialTheme.typography.labelMedium)
                    Text("₹${String.format(Locale.getDefault(), "%.2f", totalSales)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("${sales.size} Transactions", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sales) { sale ->
                    SaleReportRow(sale)
                }
            }
        }
    }
}

@Composable
fun SaleReportRow(sale: SaleEntity) {
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(sale.invoiceNumber.ifBlank { "Sale #${sale.id.take(8).uppercase()}" }, fontWeight = FontWeight.Bold)
                Text(dateFormat.format(Date(sale.createdAt)), style = MaterialTheme.typography.bodySmall)
            }
            Text("₹${String.format(Locale.getDefault(), "%.2f", sale.totalAmount)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
