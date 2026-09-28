package com.sahil.retailcore.ui.dashboard

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
import com.sahil.retailcore.data.local.entity.SaleEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    onNavigateToInventory: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToNewSale: () -> Unit,
    onNavigateToLedger: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as RetailApp
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(
            app.saleRepository,
            app.purchaseRepository,
            app.expenseRepository,
            app.productRepository,
            app.customerRepository,
            app.supplierRepository
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Scaffold(
        topBar = { DashboardTopBar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNewSale,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = "New Sale")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Statistics Section
            item {
                Text(
                    text = "Financial Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardCard(
                            title = "Today's Sales",
                            value = "₹${String.format("%.2f", uiState.todaySales)}",
                            icon = Icons.Default.Payments,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardCard(
                            title = "Today's Profit",
                            value = "₹${String.format("%.2f", uiState.todayProfit)}",
                            icon = Icons.Default.TrendingUp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardCard(
                            title = "Customer Due",
                            value = "₹${String.format("%.2f", uiState.customerOutstanding)}",
                            icon = Icons.Default.People,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardCard(
                            title = "Supplier Due",
                            value = "₹${String.format("%.2f", uiState.supplierOutstanding)}",
                            icon = Icons.Default.LocalShipping,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Inventory Section
            item {
                Text(
                    text = "Inventory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardCard(
                        title = "Total Products",
                        value = "${uiState.totalProducts}",
                        icon = Icons.Default.Inventory,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardCard(
                        title = "Low Stock",
                        value = "${uiState.lowStockCount}",
                        icon = Icons.Default.Warning,
                        containerColor = if (uiState.lowStockCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Actions Section
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        label = "Inventory",
                        icon = Icons.Default.Inventory2,
                        onClick = onNavigateToInventory,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        label = "Ledger",
                        icon = Icons.Default.AccountBalance,
                        onClick = onNavigateToLedger,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        label = "Reports",
                        icon = Icons.Default.Assessment,
                        onClick = onNavigateToReports,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        label = "Settings",
                        icon = Icons.Default.Settings,
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Recent Activity Section
            item {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(uiState.recentTransactions) { transaction ->
                val title: String
                val subtitle: String
                val icon = if (transaction is SaleEntity) Icons.Default.Receipt else Icons.Default.LocalShipping
                val time: String
                
                if (transaction is SaleEntity) {
                    title = "Sale #${transaction.id.take(8).uppercase()}"
                    subtitle = "Amount: ₹${transaction.totalAmount}"
                    time = dateFormat.format(Date(transaction.createdAt))
                } else if (transaction is PurchaseEntity) {
                    title = "Purchase #${transaction.invoiceNumber}"
                    subtitle = "Amount: ₹${transaction.grandTotal}"
                    time = dateFormat.format(Date(transaction.createdAt))
                } else {
                    title = "Transaction"
                    subtitle = ""
                    time = ""
                }

                RecentActivityCard(
                    title = title,
                    subtitle = subtitle,
                    time = time,
                    icon = icon
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
