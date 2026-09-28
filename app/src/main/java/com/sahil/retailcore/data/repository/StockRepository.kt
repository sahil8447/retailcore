package com.sahil.retailcore.data.repository

import androidx.room.withTransaction
import com.sahil.retailcore.data.local.dao.*
import com.sahil.retailcore.data.local.database.RetailDatabase
import com.sahil.retailcore.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class StockRepository(
    private val stockEntryDao: StockEntryDao,
    private val productDao: ProductDao,
    private val stockMovementDao: StockMovementDao,
    private val supplierDao: SupplierDao,
    private val paymentDao: PaymentDao,
    private val database: RetailDatabase
) {
    val allStockEntries: Flow<List<StockEntryEntity>> = stockEntryDao.getAllStockEntries()

    fun getStockEntriesByProduct(productId: String): Flow<List<StockEntryEntity>> =
        stockEntryDao.getStockEntriesByProduct(productId)

    suspend fun saveInventory(
        product: ProductEntity,
        isNewProduct: Boolean,
        entry: StockEntryEntity,
        supplier: SupplierEntity?,
        payment: PaymentEntity?
    ) {
        database.withTransaction {
            // 1. Handle Supplier and update balances
            if (supplier != null) {
                val lineSubtotal = entry.purchasePrice * entry.quantity
                val lineTotal = lineSubtotal * (1 + entry.gstPercent / 100.0)
                
                val currentSupplier = supplierDao.getSupplierById(supplier.id) ?: supplier
                val newTotalPurchases = currentSupplier.totalPurchases + lineTotal
                val newTotalPaid = currentSupplier.totalPaid + entry.amountPaid
                val newOutstanding = newTotalPurchases - newTotalPaid
                
                supplierDao.insertSupplier(currentSupplier.copy(
                    totalPurchases = newTotalPurchases,
                    totalPaid = newTotalPaid,
                    outstandingBalance = newOutstanding,
                    lastPurchaseDate = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ))
            }

            // 2. Handle Product
            if (isNewProduct) {
                productDao.insertProduct(product.copy(stockQuantity = 0))
            } else {
                productDao.updateProduct(product)
            }

            // 3. Add Stock Entry (and update stock/movement)
            val type = if (entry.paymentMode == "Opening Stock") StockMovementType.OPENING_STOCK else StockMovementType.PURCHASE
            addStockEntryInternal(entry, type)

            // 4. Handle Payment
            if (payment != null) {
                paymentDao.insertPayment(payment)
            }
        }
    }

    suspend fun saveBulkInventory(
        products: List<ProductEntity>,
        entries: List<StockEntryEntity>,
        supplier: SupplierEntity?,
        totalAmountPaid: Double,
        paymentMode: String?
    ) {
        database.withTransaction {
            // 1. Handle Supplier
            if (supplier != null) {
                var totalPurchaseAmount = 0.0
                entries.forEach { entry ->
                    val lineTotal = (entry.purchasePrice * entry.quantity) * (1 + (entry.gstPercent / 100.0))
                    totalPurchaseAmount += lineTotal
                }

                val currentSupplier = supplierDao.getSupplierById(supplier.id) ?: supplier
                val newTotalPurchases = currentSupplier.totalPurchases + totalPurchaseAmount
                val newTotalPaid = currentSupplier.totalPaid + totalAmountPaid
                val newOutstanding = newTotalPurchases - newTotalPaid

                supplierDao.insertSupplier(currentSupplier.copy(
                    totalPurchases = newTotalPurchases,
                    totalPaid = newTotalPaid,
                    outstandingBalance = newOutstanding,
                    lastPurchaseDate = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ))

                // 2. Handle Payment (Single payment for bulk entry)
                if (totalAmountPaid > 0) {
                    val paymentId = UUID.randomUUID().toString()
                    val payment = PaymentEntity(
                        id = paymentId,
                        supplierId = supplier.id,
                        amount = totalAmountPaid,
                        paymentMode = paymentMode ?: "Cash",
                        type = PaymentType.PAID,
                        referenceId = entries.firstOrNull()?.id ?: "Bulk",
                        notes = "Bulk Purchase Payment"
                    )
                    paymentDao.insertPayment(payment)
                }
            }

            // 3. Handle Products and Entries
            products.zip(entries).forEach { (product, entry) ->
                productDao.insertProduct(product.copy(stockQuantity = 0))
                val type = if (entry.paymentMode == "Opening Stock") StockMovementType.OPENING_STOCK else StockMovementType.PURCHASE
                addStockEntryInternal(entry, type)
            }
        }
    }

    private suspend fun addStockEntryInternal(entry: StockEntryEntity, type: String = StockMovementType.PURCHASE) {
        val product = productDao.getProductById(entry.productId) ?: return
        val previousStock = product.stockQuantity
        val newStock = previousStock + entry.quantity

        // 1. Save Entry
        stockEntryDao.insertStockEntry(entry)

        // 2. Update Product Stock and Last Purchase Price
        productDao.updateProduct(product.copy(
            stockQuantity = newStock,
            purchasePrice = entry.purchasePrice,
            updatedAt = System.currentTimeMillis()
        ))

        // 3. Create Movement
        val movement = StockMovementEntity(
            id = UUID.randomUUID().toString(),
            productId = entry.productId,
            type = type,
            quantity = entry.quantity,
            previousStock = previousStock,
            newStock = newStock,
            referenceId = entry.id,
            notes = "Stock Entry: ${entry.notes ?: "Added"}"
        )
        stockMovementDao.insertStockMovement(movement)
    }

    suspend fun addStockEntry(entry: StockEntryEntity) {
        database.withTransaction {
            val type = if (entry.paymentMode == "Opening Stock") StockMovementType.OPENING_STOCK else StockMovementType.PURCHASE
            addStockEntryInternal(entry, type)
        }
    }

    suspend fun updateStockEntry(newEntry: StockEntryEntity) {
        database.withTransaction {
            val oldEntry = stockEntryDao.getStockEntryById(newEntry.id) ?: return@withTransaction
            val product = productDao.getProductById(newEntry.productId) ?: return@withTransaction
            
            val adjustedStock = product.stockQuantity - oldEntry.quantity + newEntry.quantity
            if (adjustedStock < 0) throw Exception("Negative stock error")

            stockEntryDao.updateStockEntry(newEntry)
            productDao.updateProduct(product.copy(stockQuantity = adjustedStock))
        }
    }

    suspend fun deleteStockEntry(entryId: String) {
        database.withTransaction {
            val entry = stockEntryDao.getStockEntryById(entryId) ?: return@withTransaction
            val product = productDao.getProductById(entry.productId) ?: return@withTransaction

            val newStock = product.stockQuantity - entry.quantity
            if (newStock < 0) throw Exception("Negative stock error")

            // Reverse financial impact if linked to supplier
            if (entry.supplierId != null) {
                val supplier = supplierDao.getSupplierById(entry.supplierId)
                if (supplier != null) {
                    val lineSubtotal = entry.purchasePrice * entry.quantity
                    val lineTotal = lineSubtotal * (1 + entry.gstPercent / 100.0)
                    
                    supplierDao.insertSupplier(supplier.copy(
                        totalPurchases = supplier.totalPurchases - lineTotal,
                        totalPaid = supplier.totalPaid - entry.amountPaid,
                        outstandingBalance = (supplier.totalPurchases - lineTotal) - (supplier.totalPaid - entry.amountPaid),
                        updatedAt = System.currentTimeMillis()
                    ))
                }
            }

            stockEntryDao.softDeleteStockEntry(entryId, System.currentTimeMillis())
            productDao.updateProduct(product.copy(stockQuantity = newStock))
        }
    }
}
