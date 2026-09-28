package com.sahil.retailcore.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sahil.retailcore.data.local.entity.*
import com.sahil.retailcore.data.repository.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.*

data class GstReportData(
    val taxableAmountSales: Double = 0.0,
    val gstCollected: Double = 0.0,
    val taxableAmountPurchases: Double = 0.0,
    val gstPaid: Double = 0.0,
    val netGst: Double = 0.0
)

data class ProfitReportData(
    val revenue: Double = 0.0,
    val cogs: Double = 0.0,
    val grossProfit: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModel(
    private val saleRepository: SaleRepository,
    private val purchaseRepository: PurchaseRepository,
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val supplierRepository: SupplierRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _startDate = MutableStateFlow(getStartOfMonth())
    val startDate = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow(System.currentTimeMillis())
    val endDate = _endDate.asStateFlow()

    fun updateDateRange(start: Long, end: Long) {
        _startDate.value = start
        _endDate.value = end
    }

    val salesReport = combine(saleRepository.allSales, _startDate, _endDate) { sales, start, end ->
        sales.filter { it.createdAt in start..end }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchaseReport = combine(purchaseRepository.allPurchases, _startDate, _endDate) { purchases, start, end ->
        purchases.filter { it.createdAt in start..end }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gstReport = combine(salesReport, purchaseReport) { sales, purchases ->
        val collected = sales.sumOf { it.tax }
        val paid = purchases.sumOf { it.totalGST }
        val taxableSales = sales.sumOf { it.totalAmount - it.tax + it.discount }
        val taxablePurchases = purchases.sumOf { it.subtotal }
        
        GstReportData(
            taxableAmountSales = taxableSales,
            gstCollected = collected,
            taxableAmountPurchases = taxablePurchases,
            gstPaid = paid,
            netGst = collected - paid
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GstReportData())

    val profitReport = combine(
        salesReport,
        expenseRepository.allExpenses,
        _startDate,
        _endDate
    ) { sales, expenses, start, end ->
        val revenue = sales.sumOf { it.totalAmount }
        val filteredExpenses = expenses.filter { it.date in start..end }.sumOf { it.amount }
        revenue to filteredExpenses
    }.flatMapLatest { (revenue, expenses) ->
        saleRepository.getSaleItemsInRange(_startDate.value, _endDate.value).map { items ->
            val cogs = items.sumOf { it.quantity * it.purchasePrice }
            val grossProfit = revenue - cogs
            ProfitReportData(
                revenue = revenue,
                cogs = cogs,
                grossProfit = grossProfit,
                totalExpenses = expenses,
                netProfit = grossProfit - expenses
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfitReportData())

    val stockReport = productRepository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerOutstandingReport = customerRepository.allCustomers.map { list ->
        list.filter { it.outstandingBalance > 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supplierOutstandingReport = supplierRepository.allSuppliers.map { list ->
        list.filter { it.outstandingBalance > 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun getStartOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

class ReportViewModelFactory(
    private val saleRepository: SaleRepository,
    private val purchaseRepository: PurchaseRepository,
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val supplierRepository: SupplierRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportViewModel(
                saleRepository, purchaseRepository, productRepository,
                customerRepository, supplierRepository, expenseRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
