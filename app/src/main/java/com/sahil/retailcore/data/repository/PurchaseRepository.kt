package com.sahil.retailcore.data.repository

import androidx.room.withTransaction
import com.sahil.retailcore.data.local.dao.*
import com.sahil.retailcore.data.local.database.RetailDatabase
import com.sahil.retailcore.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class PurchaseRepository(
    private val purchaseDao: PurchaseDao,
    private val purchaseItemDao: PurchaseItemDao,
    private val stockMovementDao: StockMovementDao,
    private val productDao: ProductDao,
    private val supplierDao: SupplierDao,
    private val paymentDao: PaymentDao,
    private val database: RetailDatabase
) {
    val allPurchases: Flow<List<PurchaseEntity>> = purchaseDao.getAllPurchases()

    suspend fun savePurchase(
        purchase: PurchaseEntity, 
        items: List<PurchaseItemEntity>,
        amountPaid: Double,
        paymentMode: String
    ): String {
        return database.withTransaction {
            val invoiceNumber = if (purchase.invoiceNumber.isBlank()) {
                generateInvoiceNumber()
            } else {
                purchase.invoiceNumber
            }

            val purchaseWithInvoice = purchase.copy(invoiceNumber = invoiceNumber)

            // 1. Save Purchase
            purchaseDao.insertPurchase(purchaseWithInvoice)
            
            // ... (rest of logic)
            // 2. Save Purchase Items
            purchaseItemDao.insertPurchaseItems(items)
            
            // 3. Update Supplier Balance and Payment History
            val supplier = supplierDao.getSupplierById(purchase.supplierId)
            if (supplier != null) {
                val newTotalPurchases = supplier.totalPurchases + purchase.grandTotal
                val newTotalPaid = supplier.totalPaid + amountPaid
                val newOutstanding = newTotalPurchases - newTotalPaid
                
                supplierDao.insertSupplier(supplier.copy(
                    totalPurchases = newTotalPurchases,
                    totalPaid = newTotalPaid,
                    outstandingBalance = newOutstanding,
                    lastPurchaseDate = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ))

                if (amountPaid > 0) {
                    paymentDao.insertPayment(PaymentEntity(
                        id = UUID.randomUUID().toString(),
                        supplierId = purchase.supplierId,
                        amount = amountPaid,
                        paymentMode = paymentMode,
                        type = PaymentType.PAID,
                        referenceId = purchase.id,
                        notes = "Purchase Payment"
                    ))
                }
            }

            // 4. Update Product Stock and Create StockMovement
            items.forEach { item ->
                val product = productDao.getProductById(item.productId)
                if (product != null) {
                    val previousStock = product.stockQuantity
                    val newStock = previousStock + item.quantity
                    
                    productDao.updateProduct(product.copy(
                        stockQuantity = newStock,
                        updatedAt = System.currentTimeMillis()
                    ))
                    
                    stockMovementDao.insertStockMovement(StockMovementEntity(
                        id = UUID.randomUUID().toString(),
                        productId = item.productId,
                        type = StockMovementType.PURCHASE,
                        quantity = item.quantity,
                        previousStock = previousStock,
                        newStock = newStock,
                        referenceId = purchase.id,
                        notes = "Purchase Invoice: ${purchase.invoiceNumber}"
                    ))
                }
            }
            invoiceNumber
        }
    }

    private suspend fun generateInvoiceNumber(): String {
        val key = "purchase_invoice_count"
        val currentCount = database.settingsDao().getSettingByKey(key)?.value?.toIntOrNull() ?: 0
        val nextCount = currentCount + 1
        
        val setting = database.settingsDao().getSettingByKey(key)
        database.settingsDao().saveSetting(SettingsEntity(
            id = setting?.id ?: UUID.randomUUID().toString(),
            key = key,
            value = nextCount.toString()
        ))

        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return "PUR-$year-${String.format("%06d", nextCount)}"
    }

    suspend fun updateInvoicePdfPath(purchaseId: String, path: String) {
        val purchase = purchaseDao.getPurchaseById(purchaseId)
        if (purchase != null) {
            purchaseDao.insertPurchase(purchase.copy(invoicePdfPath = path))
        }
    }

    suspend fun getPurchaseWithItems(purchaseId: String): Pair<PurchaseEntity?, List<PurchaseItemEntity>> {
        val purchase = purchaseDao.getPurchaseById(purchaseId)
        val items = purchaseItemDao.getItemsByPurchaseId(purchaseId)
        return Pair(purchase, items)
    }
}
