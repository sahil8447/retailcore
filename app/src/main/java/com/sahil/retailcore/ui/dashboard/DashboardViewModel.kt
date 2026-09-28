package com.sahil.retailcore.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sahil.retailcore.data.local.entity.*
import com.sahil.retailcore.data.repository.*
import kotlinx.coroutines.flow.*
import java.util.*

data class DashboardUiState(
    val todaySales: Double = 0.0,
    val todayProfit: Double = 0.0,
    val todayPurchases: Double = 0.0,
    val todayExpenses: Double = 0.0,
    val customerOutstanding: Double = 0.0,
    val supplierOutstanding: Double = 0.0,
    val totalProducts: Int = 0,
    val lowStockCount: Int = 0,
    val recentTransactions: List<Any> = emptyList()
)

class DashboardViewModel(
    private val saleRepository: SaleRepository,
    private val purchaseRepository: PurchaseRepository,
    private val expenseRepository: ExpenseRepository,
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        saleRepository.allSales,
        purchaseRepository.allPurchases,
        expenseRepository.allExpenses,
        productRepository.allProducts,
        customerRepository.allCustomers,
        supplierRepository.allSuppliers
    ) { flows ->
        val sales = flows[0] as List<SaleEntity>
        val purchases = flows[1] as List<PurchaseEntity>
        val expenses = flows[2] as List<ExpenseEntity>
        val products = flows[3] as List<ProductEntity>
        val customers = flows[4] as List<CustomerEntity>
        val suppliers = flows[5] as List<SupplierEntity>

        val startOfDay = getStartOfDay()

        val todaySalesList = sales.filter { it.createdAt >= startOfDay }
        val todayPurchasesList = purchases.filter { it.createdAt >= startOfDay }
        val todayExpensesList = expenses.filter { it.date >= startOfDay }

        val todaySalesAmt = todaySalesList.sumOf { it.totalAmount }
        val todayPurchasesAmt = todayPurchasesList.sumOf { it.grandTotal }
        val todayExpensesAmt = todayExpensesList.sumOf { it.amount }

        val todayProfit = todaySalesAmt - todayPurchasesAmt - todayExpensesAmt

        DashboardUiState(
            todaySales = todaySalesAmt,
            todayProfit = todayProfit,
            todayPurchases = todayPurchasesAmt,
            todayExpenses = todayExpensesAmt,
            customerOutstanding = customers.sumOf { it.outstandingBalance },
            supplierOutstanding = suppliers.sumOf { it.outstandingBalance },
            totalProducts = products.size,
            lowStockCount = products.count { it.stockQuantity <= it.minimumStock },
            recentTransactions = (sales.take(5) + purchases.take(5)).sortedByDescending { 
                if (it is SaleEntity) it.createdAt else (it as PurchaseEntity).createdAt 
            }.take(10)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

class DashboardViewModelFactory(
    private val saleRepository: SaleRepository,
    private val purchaseRepository: PurchaseRepository,
    private val expenseRepository: ExpenseRepository,
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val supplierRepository: SupplierRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(
                saleRepository,
                purchaseRepository,
                expenseRepository,
                productRepository,
                customerRepository,
                supplierRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
