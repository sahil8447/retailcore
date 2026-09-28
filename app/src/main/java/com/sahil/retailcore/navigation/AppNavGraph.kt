package com.sahil.retailcore.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sahil.retailcore.ui.dashboard.DashboardBottomBar
import com.sahil.retailcore.ui.dashboard.DashboardScreen
import com.sahil.retailcore.ui.inventory.AddInventoryScreen
import com.sahil.retailcore.ui.inventory.InventoryScreen
import com.sahil.retailcore.ui.inventory.StockHistoryScreen
import com.sahil.retailcore.ui.inventory.ProductDetailScreen
import com.sahil.retailcore.ui.inventory.EditProductScreen
import com.sahil.retailcore.ui.purchase.NewPurchaseScreen
import com.sahil.retailcore.ui.purchase.PurchaseDetailScreen
import com.sahil.retailcore.ui.purchase.PurchaseListScreen
import com.sahil.retailcore.ui.sale.NewSaleScreen
import com.sahil.retailcore.ui.sale.SaleDetailScreen
import com.sahil.retailcore.ui.sale.SaleListScreen
import com.sahil.retailcore.ui.ledger.LedgerListScreen
import com.sahil.retailcore.ui.ledger.CustomerLedgerScreen
import com.sahil.retailcore.ui.ledger.SupplierLedgerScreen
import com.sahil.retailcore.ui.reports.ReportsScreen
import com.sahil.retailcore.ui.reports.SalesReportScreen
import com.sahil.retailcore.ui.reports.PurchaseReportScreen
import com.sahil.retailcore.ui.reports.GstReportScreen
import com.sahil.retailcore.ui.reports.ProfitReportScreen
import com.sahil.retailcore.ui.reports.StockReportScreen
import com.sahil.retailcore.ui.reports.OutstandingReportScreen
import com.sahil.retailcore.ui.settings.SettingsScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != Screen.AddProduct.route) {
                DashboardBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(Screen.Dashboard.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToInventory = { navController.navigate(Screen.Inventory.route) },
                    onNavigateToAddProduct = { navController.navigate(Screen.AddProduct.route) },
                    onNavigateToNewSale = { navController.navigate(Screen.NewSale.route) },
                    onNavigateToLedger = { navController.navigate(Screen.LedgerList.route) },
                    onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Inventory.route) {
                InventoryScreen(
                    onNavigateToAddInventory = { navController.navigate(Screen.AddStock.route) },
                    onNavigateToStockHistory = { navController.navigate(Screen.StockHistory.route) },
                    onNavigateToProductDetail = { productId -> 
                        navController.navigate(Screen.ProductDetail.createRoute(productId)) 
                    }
                )
            }
            composable(Screen.AddProduct.route) {
                AddInventoryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AddStock.route) {
                AddInventoryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.StockHistory.route) {
                StockHistoryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.EditProduct.route,
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                EditProductScreen(
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.ProductDetail.route,
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                ProductDetailScreen(
                    productId = productId,
                    onNavigateToEdit = { id -> navController.navigate(Screen.EditProduct.createRoute(id)) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.PurchaseList.route) {
                PurchaseListScreen(
                    onNavigateToNewPurchase = { navController.navigate(Screen.NewPurchase.route) },
                    onNavigateToPurchaseDetail = { purchaseId ->
                        navController.navigate(Screen.PurchaseDetail.createRoute(purchaseId))
                    }
                )
            }
            composable(Screen.NewPurchase.route) {
                NewPurchaseScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.PurchaseDetail.route,
                arguments = listOf(navArgument("purchaseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val purchaseId = backStackEntry.arguments?.getString("purchaseId") ?: ""
                PurchaseDetailScreen(
                    purchaseId = purchaseId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.SaleList.route) {
                SaleListScreen(
                    onNavigateToNewSale = { navController.navigate(Screen.NewSale.route) },
                    onNavigateToSaleDetail = { saleId -> 
                        navController.navigate(Screen.SaleDetail.createRoute(saleId))
                    }
                )
            }
            composable(Screen.NewSale.route) {
                NewSaleScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.SaleDetail.route,
                arguments = listOf(navArgument("saleId") { type = NavType.StringType })
            ) { backStackEntry ->
                val saleId = backStackEntry.arguments?.getString("saleId") ?: ""
                SaleDetailScreen(
                    saleId = saleId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.LedgerList.route) {
                LedgerListScreen(
                    onNavigateToCustomerLedger = { customerId ->
                        navController.navigate(Screen.CustomerLedger.createRoute(customerId))
                    },
                    onNavigateToSupplierLedger = { supplierId ->
                        navController.navigate(Screen.SupplierLedger.createRoute(supplierId))
                    }
                )
            }
            composable(
                route = Screen.CustomerLedger.route,
                arguments = listOf(navArgument("customerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                CustomerLedgerScreen(
                    customerId = customerId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.SupplierLedger.route,
                arguments = listOf(navArgument("supplierId") { type = NavType.StringType })
            ) { backStackEntry ->
                val supplierId = backStackEntry.arguments?.getString("supplierId") ?: ""
                SupplierLedgerScreen(
                    supplierId = supplierId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Reports.route) {
                ReportsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToReport = { route -> navController.navigate(route) }
                )
            }
            composable(Screen.SalesReport.route) {
                SalesReportScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.PurchaseReport.route) {
                PurchaseReportScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.ExpenseReport.route) {
                PlaceholderScreen("Expense Report")
            }
            composable(Screen.GstReport.route) {
                GstReportScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.ProfitReport.route) {
                ProfitReportScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.StockReport.route) {
                StockReportScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.CustomerOutstandingReport.route) {
                OutstandingReportScreen(type = "CUSTOMER", onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.SupplierOutstandingReport.route) {
                OutstandingReportScreen(type = "SUPPLIER", onNavigateBack = { navController.popBackStack() })
            }
            composable("sales") {
                SaleListScreen(
                    onNavigateToNewSale = { navController.navigate(Screen.NewSale.route) },
                    onNavigateToSaleDetail = { saleId -> 
                        navController.navigate(Screen.SaleDetail.createRoute(saleId))
                    }
                )
            }
            composable("profile") {
                SettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$name Screen Coming Soon",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
