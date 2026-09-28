package com.sahil.retailcore.ui.purchase

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

class PurchaseViewModel(
    private val purchaseRepository: PurchaseRepository,
    private val supplierRepository: SupplierRepository,
    private val productRepository: ProductRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val purchases: StateFlow<List<PurchaseEntity>> = purchaseRepository.allPurchases
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) list
            else list.filter { it.invoiceNumber.contains(query, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = supplierRepository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = productRepository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    suspend fun getPurchaseWithItems(purchaseId: String): Pair<PurchaseEntity?, List<PurchaseItemEntity>> {
        return purchaseRepository.getPurchaseWithItems(purchaseId)
    }

    fun savePurchaseWithSupplier(
        context: Context,
        supplierName: String,
        supplierPhone: String,
        supplierGst: String?,
        invoiceNumber: String,
        invoiceDate: Long,
        items: List<PurchaseItemUiState>,
        notes: String?,
        amountPaid: Double,
        paymentMode: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (supplierName.isBlank() || supplierPhone.isBlank()) {
            onResult(false, "Supplier Name and Mobile are required")
            return
        }
        if (items.isEmpty()) {
            onResult(false, "Please add at least one product")
            return
        }

        viewModelScope.launch {
            try {
                // 1. Find or Create/Update Supplier
                var supplier = supplierRepository.getSupplierByPhone(supplierPhone)
                if (supplier == null && !supplierGst.isNullOrBlank()) {
                    supplier = supplierRepository.getSupplierByGst(supplierGst)
                }

                val supplierId = supplier?.id ?: UUID.randomUUID().toString()
                
                val updatedSupplier = if (supplier == null) {
                    SupplierEntity(
                        id = supplierId,
                        name = supplierName,
                        phone = supplierPhone,
                        gstNumber = supplierGst
                    )
                } else {
                    supplier.copy(
                        name = supplierName,
                        gstNumber = supplierGst ?: supplier.gstNumber,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                supplierRepository.insertSupplier(updatedSupplier)

                // 2. Prepare Purchase
                val purchaseId = UUID.randomUUID().toString()
                var subtotal = 0.0
                var totalGST = 0.0
                
                val purchaseItems = items.map { uiItem ->
                    val lineSubtotal = uiItem.purchasePriceWithoutGST * uiItem.quantity
                    val lineGstAmount = (lineSubtotal * uiItem.gstPercent) / 100.0
                    val lineTotal = lineSubtotal + lineGstAmount
                    
                    subtotal += lineSubtotal
                    totalGST += lineGstAmount
                    
                    PurchaseItemEntity(
                        id = UUID.randomUUID().toString(),
                        purchaseId = purchaseId,
                        productId = uiItem.productId,
                        quantity = uiItem.quantity,
                        purchasePriceWithoutGST = uiItem.purchasePriceWithoutGST,
                        gstPercent = uiItem.gstPercent,
                        gstAmount = lineGstAmount / uiItem.quantity,
                        purchasePriceWithGST = (uiItem.purchasePriceWithoutGST * (1 + uiItem.gstPercent / 100.0)),
                        lineTotal = lineTotal
                    )
                }

                val grandTotal = subtotal + totalGST
                val purchase = PurchaseEntity(
                    id = purchaseId,
                    supplierId = supplierId,
                    invoiceNumber = invoiceNumber, // User can provide or leave blank for auto
                    invoiceDate = invoiceDate,
                    paymentStatus = if (amountPaid >= grandTotal) "Paid" else if (amountPaid > 0) "Partial" else "Pending",
                    notes = notes,
                    subtotal = subtotal,
                    totalGST = totalGST,
                    grandTotal = grandTotal
                )

                val finalInvoiceNumber = purchaseRepository.savePurchase(purchase, purchaseItems, amountPaid, paymentMode)
                
                // 3. Generate Purchase PDF (Voucher)
                val shopName = settingsRepository.getSetting("shop_name") ?: "RetailCore"
                val shopAddress = settingsRepository.getSetting("shop_address") ?: ""
                val shopGst = settingsRepository.getSetting("shop_gst") ?: ""
                
                val finalPurchase = purchase.copy(invoiceNumber = finalInvoiceNumber)
                val pairItems = mutableListOf<Pair<PurchaseItemEntity, ProductEntity>>()
                for (si in purchaseItems) {
                    productRepository.getProductById(si.productId)?.let {
                        pairItems.add(si to it)
                    }
                }
                
                val pdfFile = InvoiceGenerator.generatePurchaseInvoice(
                    context, shopName, shopAddress, shopGst, finalPurchase, updatedSupplier, pairItems
                )
                
                if (pdfFile != null) {
                    purchaseRepository.updateInvoicePdfPath(purchaseId, pdfFile.absolutePath)
                }

                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun shareInvoice(context: Context, purchase: PurchaseEntity) {
        val path = purchase.invoicePdfPath
        if (path != null) {
            val file = java.io.File(path)
            if (file.exists()) {
                InvoiceUtils.shareInvoice(context, file)
            }
        }
    }

    fun printInvoice(context: Context, purchase: PurchaseEntity) {
        val path = purchase.invoicePdfPath
        if (path != null) {
            val file = java.io.File(path)
            if (file.exists()) {
                InvoiceUtils.printInvoice(context, file)
            }
        }
    }
}

data class PurchaseItemUiState(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val purchasePriceWithoutGST: Double,
    val gstPercent: Double
)

class PurchaseViewModelFactory(
    private val purchaseRepository: PurchaseRepository,
    private val supplierRepository: SupplierRepository,
    private val productRepository: ProductRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PurchaseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PurchaseViewModel(purchaseRepository, supplierRepository, productRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
