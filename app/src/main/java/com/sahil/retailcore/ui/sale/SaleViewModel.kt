package com.sahil.retailcore.ui.sale

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sahil.retailcore.data.local.entity.*
import com.sahil.retailcore.data.repository.*
import com.sahil.retailcore.utils.InvoiceGenerator
import com.sahil.retailcore.utils.InvoiceUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class SaleViewModel(
    private val saleRepository: SaleRepository,
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val sales: StateFlow<List<SaleEntity>> = saleRepository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = productRepository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    suspend fun getCustomerByPhone(phone: String): CustomerEntity? {
        return customerRepository.getCustomerByPhone(phone)
    }

    suspend fun getSaleWithItems(saleId: String) = saleRepository.getSaleWithItems(saleId)
    
    suspend fun getSalePayments(saleId: String) = saleRepository.getSalePayments(saleId)

    fun saveSale(
        context: Context,
        customerName: String,
        customerPhone: String,
        customerAddress: String?,
        customerGst: String?,
        items: List<SaleItemUiState>,
        payments: List<PaymentEntity>,
        totalDiscount: Double,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (customerName.isBlank() || customerPhone.isBlank()) {
            onResult(false, "Customer Name and Mobile are required")
            return
        }
        if (items.isEmpty()) {
            onResult(false, "Add at least one product")
            return
        }

        viewModelScope.launch {
            try {
                // 1. Find or Create/Update Customer
                var customer = customerRepository.getCustomerByPhone(customerPhone)
                val customerId = customer?.id ?: UUID.randomUUID().toString()
                
                val updatedCustomer = if (customer == null) {
                    CustomerEntity(
                        id = customerId,
                        name = customerName,
                        phone = customerPhone,
                        address = customerAddress,
                        gstNumber = customerGst
                    )
                } else {
                    customer.copy(
                        name = customerName,
                        address = customerAddress,
                        gstNumber = customerGst,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                customerRepository.insertCustomer(updatedCustomer)

                // 2. Prepare Sale
                val saleId = UUID.randomUUID().toString()
                var subtotal = 0.0
                var totalTax = 0.0

                val saleItems = items.map { uiItem ->
                    val product = productRepository.getProductById(uiItem.productId)
                    val purchasePrice = product?.purchasePrice ?: 0.0

                    val lineTotalBeforeTax = uiItem.sellingPrice * uiItem.quantity
                    val lineTax = (lineTotalBeforeTax * uiItem.gstPercent) / 100.0
                    subtotal += lineTotalBeforeTax
                    totalTax += lineTax

                    SaleItemEntity(
                        id = UUID.randomUUID().toString(),
                        saleId = saleId,
                        productId = uiItem.productId,
                        quantity = uiItem.quantity,
                        unitPrice = uiItem.sellingPrice,
                        purchasePrice = purchasePrice,
                        discount = uiItem.discount,
                        gstPercent = uiItem.gstPercent,
                        subtotal = lineTotalBeforeTax + lineTax
                    )
                }

                val grandTotal = subtotal + totalTax - totalDiscount
                val paymentSummary = payments.filter { it.amount > 0 }.joinToString(" + ") { it.paymentMode }

                val sale = SaleEntity(
                    id = saleId,
                    invoiceNumber = "", 
                    customerId = customerId,
                    totalAmount = grandTotal,
                    discount = totalDiscount,
                    tax = totalTax,
                    paymentMethod = paymentSummary.ifBlank { "Cash" },
                    status = "Completed"
                )

                // 3. Save Sale
                val invoiceNumber = saleRepository.saveSale(sale, saleItems, payments.map { it.copy(customerId = customerId) })
                val finalSale = sale.copy(invoiceNumber = invoiceNumber)

                // 4. Generate Invoice PDF
                val shopName = settingsRepository.getSetting("shop_name") ?: "RetailCore"
                val shopAddress = settingsRepository.getSetting("shop_address") ?: ""
                val shopGst = settingsRepository.getSetting("shop_gst") ?: ""
                
                val pairItems = mutableListOf<Pair<SaleItemEntity, ProductEntity>>()
                for (si in saleItems) {
                    productRepository.getProductById(si.productId)?.let {
                        pairItems.add(si to it)
                    }
                }
                
                val pdfFile = InvoiceGenerator.generateSaleInvoice(
                    context, shopName, shopAddress, shopGst, finalSale, updatedCustomer, pairItems
                )
                
                if (pdfFile != null) {
                    saleRepository.updateInvoicePdfPath(saleId, pdfFile.absolutePath)
                }

                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun shareInvoice(context: Context, sale: SaleEntity) {
        val path = sale.invoicePdfPath
        if (path != null) {
            val file = java.io.File(path)
            if (file.exists()) {
                InvoiceUtils.shareInvoice(context, file)
            }
        }
    }

    fun printInvoice(context: Context, sale: SaleEntity) {
        val path = sale.invoicePdfPath
        if (path != null) {
            val file = java.io.File(path)
            if (file.exists()) {
                InvoiceUtils.printInvoice(context, file)
            }
        }
    }

    fun getSaleItemsByProduct(productId: String) = saleRepository.getSaleItemsByProduct(productId)
    
    suspend fun getProductById(id: String) = productRepository.getProductById(id)
}

data class SaleItemUiState(
    val productId: String,
    val productName: String,
    val modelNumber: String,
    val brand: String,
    val category: String,
    val quantity: Int,
    val sellingPrice: Double,
    val gstPercent: Double,
    val discount: Double = 0.0,
    val warranty: Int = 0,
    val availableStock: Int = 0
)

class SaleViewModelFactory(
    private val saleRepository: SaleRepository,
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SaleViewModel(saleRepository, customerRepository, productRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
