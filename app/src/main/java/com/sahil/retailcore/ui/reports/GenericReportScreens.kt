package com.sahil.retailcore.ui.reports

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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockReportScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as RetailApp
    val viewModel: ReportViewModel = viewModel(factory = ReportViewModelFactory(app.saleRepository, app.purchaseRepository, app.productRepository, app.customerRepository, app.supplierRepository, app.expenseRepository))

    val products by viewModel.stockReport.collectAsState()
    val totalStockValue = products.sumOf { it.stockQuantity * it.purchasePrice }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Stock Report") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Inventory Value", style = MaterialTheme.typography.labelMedium)
                    Text("₹${String.format(Locale.getDefault(), "%.2f", totalStockValue)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(products) { p ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(p.name, fontWeight = FontWeight.Bold)
                                Text("SKU: ${p.sku}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${p.stockQuantity} units", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutstandingReportScreen(type: String, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as RetailApp
    val viewModel: ReportViewModel = viewModel(factory = ReportViewModelFactory(app.saleRepository, app.purchaseRepository, app.productRepository, app.customerRepository, app.supplierRepository, app.expenseRepository))

    val customers by viewModel.customerOutstandingReport.collectAsState()
    val suppliers by viewModel.supplierOutstandingReport.collectAsState()
    
    val list = if (type == "CUSTOMER") customers.map { it.name to it.outstandingBalance } else suppliers.map { it.name to it.outstandingBalance }
    val total = list.sumOf { it.second }

    Scaffold(
        topBar = { TopAppBar(title = { Text("$type Outstanding") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Outstanding", style = MaterialTheme.typography.labelMedium)
                    Text("₹${String.format(Locale.getDefault(), "%.2f", total)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.first, fontWeight = FontWeight.Bold)
                            Text("₹${String.format(Locale.getDefault(), "%.2f", item.second)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GstReportScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as RetailApp
    val viewModel: ReportViewModel = viewModel(factory = ReportViewModelFactory(app.saleRepository, app.purchaseRepository, app.productRepository, app.customerRepository, app.supplierRepository, app.expenseRepository))

    val gstData by viewModel.gstReport.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("GST Report") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                ReportSummaryCard("GST Collected (Sales)", gstData.gstCollected, MaterialTheme.colorScheme.primary)
            }
            item {
                ReportSummaryCard("GST Paid (Purchases)", gstData.gstPaid, MaterialTheme.colorScheme.secondary)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Net GST Payable", style = MaterialTheme.typography.labelMedium)
                        Text("₹${String.format(Locale.getDefault(), "%.2f", gstData.netGst)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                HorizontalDivider()
                Text("Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                DetailRow("Taxable Sales", gstData.taxableAmountSales)
            }
            item {
                DetailRow("Taxable Purchases", gstData.taxableAmountPurchases)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitReportScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as RetailApp
    val viewModel: ReportViewModel = viewModel(factory = ReportViewModelFactory(app.saleRepository, app.purchaseRepository, app.productRepository, app.customerRepository, app.supplierRepository, app.expenseRepository))

    val profitData by viewModel.profitReport.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profit Report") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                ReportSummaryCard("Net Profit", profitData.netProfit, if (profitData.netProfit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            item {
                HorizontalDivider()
                Text("Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item { DetailRow("Total Revenue", profitData.revenue) }
            item { DetailRow("Cost of Goods Sold (COGS)", profitData.cogs, isNegative = true) }
            item { DetailRow("Gross Profit", profitData.grossProfit, isBold = true) }
            item { DetailRow("Total Expenses", profitData.totalExpenses, isNegative = true) }
            item { DetailRow("Net Profit", profitData.netProfit, isBold = true, color = if (profitData.netProfit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun ReportSummaryCard(title: String, amount: Double, color: androidx.compose.ui.graphics.Color) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text("₹${String.format(Locale.getDefault(), "%.2f", amount)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun DetailRow(label: String, amount: Double, isNegative: Boolean = false, isBold: Boolean = false, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        val prefix = if (isNegative) "- " else ""
        Text("$prefix₹${String.format(Locale.getDefault(), "%.2f", amount)}", fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, color = color)
    }
}
