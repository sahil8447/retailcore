package com.sahil.retailcore.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Inventory : Screen("inventory")
    object AddProduct : Screen("add_product")
    object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }
    object AddStock : Screen("add_stock")
    object StockHistory : Screen("stock_history")
    object EditProduct : Screen("edit_product/{productId}") {
        fun createRoute(productId: String) = "edit_product/$productId"
    }
    object PurchaseList : Screen("purchase_list")
    object NewPurchase : Screen("new_purchase")
    object PurchaseDetail : Screen("purchase_detail/{purchaseId}") {
        fun createRoute(purchaseId: String) = "purchase_detail/$purchaseId"
    }
    object SaleList : Screen("sale_list")
    object NewSale : Screen("new_sale")
    object SaleDetail : Screen("sale_detail/{saleId}") {
        fun createRoute(saleId: String) = "sale_detail/$saleId"
    }
    object LedgerList : Screen("ledger_list")
    object CustomerLedger : Screen("customer_ledger/{customerId}") {
        fun createRoute(customerId: String) = "customer_ledger/$customerId"
    }
    object SupplierLedger : Screen("supplier_ledger/{supplierId}") {
        fun createRoute(supplierId: String) = "supplier_ledger/$supplierId"
    }
    object Reports : Screen("reports")
    object SalesReport : Screen("sales_report")
    object PurchaseReport : Screen("purchase_report")
    object ExpenseReport : Screen("expense_report")
    object ProfitReport : Screen("profit_report")
    object GstReport : Screen("gst_report")
    object StockReport : Screen("stock_report")
    object CustomerOutstandingReport : Screen("customer_outstanding_report")
    object SupplierOutstandingReport : Screen("supplier_outstanding_report")
    object Settings : Screen("settings")
}
