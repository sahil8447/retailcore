package com.sahil.retailcore.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sahil.retailcore.data.local.entity.*
import com.sahil.retailcore.data.repository.ProductRepository
import com.sahil.retailcore.data.repository.StockRepository
import com.sahil.retailcore.data.repository.SupplierRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class InventoryModelItem(
    val id: String = UUID.randomUUID().toString(),
    var modelNumber: String = "",
    var barcode: String = "",
    var quantity: String = "1",
    var purchasePrice: String = "",
    var sellingPrice: String = ""
)

class InventoryViewModel(
    private val stockRepository: StockRepository,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    val allProducts = productRepository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSuppliers = supplierRepository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockEntries = stockRepository.allStockEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun findProductByModel(model: String): ProductEntity? {
        return productRepository.getProductByModelNumber(model)
    }

    suspend fun findProductByBarcode(barcode: String): ProductEntity? {
        return productRepository.getProductByBarcode(barcode)
    }

    fun getFilteredProducts(query: String): Flow<List<ProductEntity>> {
        if (query.isBlank()) return flowOf(emptyList())
        return allProducts.map { list ->
            list.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.modelNumber.contains(query, ignoreCase = true) ||
                it.sku?.contains(query, ignoreCase = true) == true ||
                it.brand?.contains(query, ignoreCase = true) == true
            }.take(5)
        }
    }

    fun saveMultiModelInventory(
        commonProductInfo: ProductEntity, // Template for new products
        isOpeningStock: Boolean,
        models: List<InventoryModelItem>,
        supplierName: String?,
        supplierPhone: String?,
        supplierGst: String?,
        amountPaid: Double,
        paymentMode: String?,
        entryDate: Long,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (commonProductInfo.brand.isNullOrBlank()) {
            onResult(false, "Brand is required")
            return
        }
        if (models.isEmpty()) {
            onResult(false, "At least one model is required")
            return
        }

        viewModelScope.launch {
            try {
                // 1. Prepare Supplier (only if not opening stock)
                var supplier: SupplierEntity? = null
                if (!isOpeningStock && (!supplierPhone.isNullOrBlank() || !supplierName.isNullOrBlank())) {
                    val foundSupplier = if (!supplierPhone.isNullOrBlank()) supplierRepository.getSupplierByPhone(supplierPhone) else null
                    val supplierId = foundSupplier?.id ?: UUID.randomUUID().toString()
                    
                    supplier = if (foundSupplier == null) {
                        SupplierEntity(
                            id = supplierId,
                            name = supplierName ?: "Unknown",
                            phone = supplierPhone ?: "",
                            gstNumber = supplierGst
                        )
                    } else {
                        foundSupplier.copy(
                            name = supplierName ?: foundSupplier.name,
                            gstNumber = supplierGst ?: foundSupplier.gstNumber,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                }

                // Calculate total across all models for payment/supplier update
                var totalAmountAllModels = 0.0
                models.forEach { item ->
                    val qty = item.quantity.toIntOrNull() ?: 0
                    val pPrice = item.purchasePrice.toDoubleOrNull() ?: 0.0
                    val lineTotal = (pPrice * qty) * (1 + (commonProductInfo.gstPercent / 100.0))
                    totalAmountAllModels += lineTotal
                }

                // 2. Process each model
                val productEntities = mutableListOf<ProductEntity>()
                val entryEntities = mutableListOf<StockEntryEntity>()

                models.forEach { item ->
                    val productId = UUID.randomUUID().toString()
                    val pPrice = item.purchasePrice.toDoubleOrNull() ?: 0.0
                    val sPrice = item.sellingPrice.toDoubleOrNull() ?: 0.0
                    val qty = item.quantity.toIntOrNull() ?: 0

                    val product = commonProductInfo.copy(
                        id = productId,
                        name = "${commonProductInfo.brand} ${commonProductInfo.categoryId} ${item.modelNumber}",
                        modelNumber = item.modelNumber,
                        sku = item.barcode,
                        purchasePrice = pPrice,
                        sellingPrice = sPrice,
                        stockQuantity = 0
                    )
                    productEntities.add(product)

                    val entryId = UUID.randomUUID().toString()
                    val entry = StockEntryEntity(
                        id = entryId,
                        productId = productId,
                        quantity = qty,
                        purchasePrice = pPrice,
                        gstPercent = commonProductInfo.gstPercent,
                        supplierId = if (isOpeningStock) null else supplier?.id,
                        amountPaid = 0.0,
                        paymentMode = if (isOpeningStock) "Opening Stock" else paymentMode,
                        notes = if (isOpeningStock) "Opening Stock (Bulk)" else "Bulk Purchase",
                        entryDate = entryDate
                    )
                    entryEntities.add(entry)
                }
                
                stockRepository.saveBulkInventory(
                    products = productEntities,
                    entries = entryEntities,
                    supplier = supplier,
                    totalAmountPaid = if (isOpeningStock) 0.0 else amountPaid,
                    paymentMode = paymentMode
                )
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun saveExistingProductStock(
        product: ProductEntity,
        isOpeningStock: Boolean,
        quantity: Int,
        purchasePrice: Double,
        supplierName: String?,
        supplierPhone: String?,
        supplierGst: String?,
        amountPaid: Double,
        paymentMode: String?,
        notes: String?,
        entryDate: Long,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // 1. Prepare Supplier
                var supplier: SupplierEntity? = null
                if (!isOpeningStock && (!supplierPhone.isNullOrBlank() || !supplierName.isNullOrBlank())) {
                    val foundSupplier = if (!supplierPhone.isNullOrBlank()) supplierRepository.getSupplierByPhone(supplierPhone) else null
                    supplier = if (foundSupplier == null) {
                        SupplierEntity(
                            id = UUID.randomUUID().toString(),
                            name = supplierName ?: "Unknown",
                            phone = supplierPhone ?: "",
                            gstNumber = supplierGst
                        )
                    } else {
                        foundSupplier.copy(
                            name = supplierName ?: foundSupplier.name,
                            gstNumber = supplierGst ?: foundSupplier.gstNumber,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                }

                val entryId = UUID.randomUUID().toString()
                val entry = StockEntryEntity(
                    id = entryId,
                    productId = product.id,
                    quantity = quantity,
                    purchasePrice = purchasePrice,
                    gstPercent = product.gstPercent,
                    supplierId = if (isOpeningStock) null else supplier?.id,
                    amountPaid = if (isOpeningStock) 0.0 else amountPaid,
                    paymentMode = if (isOpeningStock) "Opening Stock" else paymentMode,
                    notes = notes,
                    entryDate = entryDate
                )

                var payment: PaymentEntity? = null
                if (!isOpeningStock && amountPaid > 0 && supplier != null) {
                    payment = PaymentEntity(
                        id = UUID.randomUUID().toString(),
                        supplierId = supplier.id,
                        amount = amountPaid,
                        paymentMode = paymentMode ?: "Cash",
                        type = PaymentType.PAID,
                        referenceId = entryId,
                        notes = "Stock Add Payment"
                    )
                }

                stockRepository.saveInventory(
                    product = product,
                    isNewProduct = false,
                    entry = entry,
                    supplier = supplier,
                    payment = payment
                )
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun updateProductDetails(product: ProductEntity, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                productRepository.updateProduct(product.copy(updatedAt = System.currentTimeMillis()))
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun saveInventory(
        product: ProductEntity,
        isNewProduct: Boolean,
        isOpeningStock: Boolean,
        quantity: Int,
        purchasePrice: Double,
        gstPercent: Double,
        supplierName: String?,
        supplierPhone: String?,
        supplierGst: String?,
        amountPaid: Double,
        paymentMode: String?,
        notes: String?,
        entryDate: Long,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (product.name.isBlank()) {
            onResult(false, "Product Name is required")
            return
        }
        if (product.modelNumber.isBlank()) {
            onResult(false, "Model Number is required")
            return
        }
        if (quantity <= 0) {
            onResult(false, "Quantity must be greater than zero")
            return
        }
        if (product.sellingPrice <= 0) {
            onResult(false, "Selling Price must be greater than zero")
            return
        }
        if (purchasePrice <= 0) {
            onResult(false, "Purchase Price must be greater than zero")
            return
        }

        viewModelScope.launch {
            try {
                // 1. Prepare Supplier (only if not opening stock)
                var supplier: SupplierEntity? = null
                if (!isOpeningStock && (!supplierPhone.isNullOrBlank() || !supplierName.isNullOrBlank())) {
                    val foundSupplier = if (!supplierPhone.isNullOrBlank()) supplierRepository.getSupplierByPhone(supplierPhone) else null
                    val supplierId = foundSupplier?.id ?: UUID.randomUUID().toString()
                    
                    supplier = if (foundSupplier == null) {
                        SupplierEntity(
                            id = supplierId,
                            name = supplierName ?: "Unknown",
                            phone = supplierPhone ?: "",
                            gstNumber = supplierGst
                        )
                    } else {
                        foundSupplier.copy(
                            name = supplierName ?: foundSupplier.name,
                            gstNumber = supplierGst ?: foundSupplier.gstNumber,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                }

                // 2. Prepare Stock Entry
                val entryId = UUID.randomUUID().toString()
                val entry = StockEntryEntity(
                    id = entryId,
                    productId = product.id,
                    quantity = quantity,
                    purchasePrice = purchasePrice,
                    gstPercent = gstPercent,
                    supplierId = if (isOpeningStock) null else supplier?.id,
                    amountPaid = if (isOpeningStock) 0.0 else amountPaid,
                    paymentMode = if (isOpeningStock) "Opening Stock" else paymentMode,
                    notes = if (isOpeningStock) "Opening Stock: ${notes ?: ""}" else notes,
                    entryDate = entryDate
                )

                // 3. Prepare Payment (only if not opening stock)
                var payment: PaymentEntity? = null
                if (!isOpeningStock && amountPaid > 0 && supplier != null) {
                    payment = PaymentEntity(
                        id = UUID.randomUUID().toString(),
                        supplierId = supplier.id,
                        amount = amountPaid,
                        paymentMode = paymentMode ?: "Cash",
                        type = PaymentType.PAID,
                        referenceId = entryId,
                        notes = "Stock Entry Payment"
                    )
                }

                // 4. Save everything in one transaction
                stockRepository.saveInventory(
                    product = product,
                    isNewProduct = isNewProduct,
                    entry = entry,
                    supplier = supplier,
                    payment = payment
                )

                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun deleteStockEntry(entryId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                stockRepository.deleteStockEntry(entryId)
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun getStockEntriesForProduct(productId: String) = stockRepository.getStockEntriesByProduct(productId)
    
    suspend fun getProductById(id: String) = productRepository.getProductById(id)
}

class InventoryViewModelFactory(
    private val stockRepository: StockRepository,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(stockRepository, productRepository, supplierRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
