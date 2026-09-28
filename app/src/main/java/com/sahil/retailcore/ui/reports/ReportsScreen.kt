package com.sahil.retailcore.ui.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sahil.retailcore.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReport: (String) -> Unit
) {
    val reportItems = listOf(
        ReportMenuItem("Sales Report", "Detailed sales history and totals", Icons.Default.Receipt, Screen.SalesReport.route),
        ReportMenuItem("Purchase Report", "Purchase invoices and expenses", Icons.Default.LocalShipping, Screen.PurchaseReport.route),
        ReportMenuItem("Expense Report", "Detailed expenditure reports", Icons.Default.Payments, Screen.ExpenseReport.route),
        ReportMenuItem("Profit Report", "Income vs Expenditure analysis", Icons.Default.TrendingUp, Screen.ProfitReport.route),
        ReportMenuItem("GST Report", "Tax collected and paid", Icons.Default.AccountBalance, Screen.GstReport.route),
        ReportMenuItem("Stock Report", "Current inventory levels and value", Icons.Default.Inventory, Screen.StockReport.route),
        ReportMenuItem("Customer Outstanding", "Unpaid customer balances", Icons.Default.People, Screen.CustomerOutstandingReport.route),
        ReportMenuItem("Supplier Outstanding", "Balances due to suppliers", Icons.Default.Business, Screen.SupplierOutstandingReport.route)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(reportItems) { item ->
                ReportCard(item) { onNavigateToReport(item.route) }
            }
        }
    }
}

@Composable
fun ReportCard(item: ReportMenuItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

data class ReportMenuItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String
)
