package com.sahil.retailcore.data.repository

import androidx.room.withTransaction
import com.sahil.retailcore.data.local.dao.*
import com.sahil.retailcore.data.local.database.RetailDatabase
import com.sahil.retailcore.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.*

class SaleRepository(
    private val saleDao: SaleDao,
    private val productDao: ProductDao,
    private val stockMovementDao: StockMovementDao,
    private val customerDao: CustomerDao,
    private val paymentDao: PaymentDao,
    private val database: RetailDatabase
) {
    val allSales: Flow<List<SaleEntity>> = saleDao.getAllSales()

    suspend fun saveSale(
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        payments: List<PaymentEntity>
    ): String { 
        return database.withTransaction {
            val invoiceNumber = if (sale.invoiceNumber.isBlank()) {
                generateInvoiceNumber()
            } else {
                sale.invoiceNumber
            }

            val saleWithInvoice = sale.copy(invoiceNumber = invoiceNumber)
            saleDao.insertSale(saleWithInvoice)
            saleDao.insertSaleItems(items)
            
            if (sale.customerId != null) {
                val customer = customerDao.getCustomerById(sale.customerId)
                if (customer != null) {
                    val totalReceived = payments.sumOf { it.amount }
                    val newTotalSales = customer.totalSales + sale.totalAmount
                    val newTotalReceived = customer.totalReceived + totalReceived
                    val newOutstanding = customer.openingBalance + newTotalSales - newTotalReceived
                    
                    customerDao.insertCustomer(customer.copy(
                        totalSales = newTotalSales,
                        totalReceived = newTotalReceived,
                        outstandingBalance = newOutstanding,
                        lastPurchaseDate = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ))

                    payments.forEach { payment ->
                        if (payment.amount > 0) {
                            paymentDao.insertPayment(payment.copy(referenceId = sale.id))
                        }
                    }
                }
            }

            items.forEach { item ->
                val product = productDao.getProductById(item.productId)
                if (product != null) {
                    val previousStock = product.stockQuantity
                    val newStock = previousStock - item.quantity
                    
                    if (newStock < 0) throw Exception("Insufficient stock for ${product.name}")

                    productDao.updateProduct(product.copy(
                        stockQuantity = newStock,
                        updatedAt = System.currentTimeMillis()
                    ))
                    
                    stockMovementDao.insertStockMovement(StockMovementEntity(
                        id = UUID.randomUUID().toString(),
                        productId = item.productId,
                        type = StockMovementType.SALE,
                        quantity = item.quantity,
                        previousStock = previousStock,
                        newStock = newStock,
                        referenceId = sale.id,
                        notes = "Sale Transaction: $invoiceNumber"
                    ))
                }
            }
            invoiceNumber
        }
    }

    private suspend fun generateInvoiceNumber(): String {
        val key = "sales_invoice_count"
        val currentCount = database.settingsDao().getSettingByKey(key)?.value?.toIntOrNull() ?: 0
        val nextCount = currentCount + 1
        
        val setting = database.settingsDao().getSettingByKey(key)
        database.settingsDao().saveSetting(SettingsEntity(
            id = setting?.id ?: UUID.randomUUID().toString(),
            key = key,
            value = nextCount.toString()
        ))

        val year = Calendar.getInstance().get(Calendar.YEAR)
        return "SAL-$year-${String.format(Locale.getDefault(), "%06d", nextCount)}"
    }

    suspend fun updateInvoicePdfPath(saleId: String, path: String) {
        val sale = saleDao.getSaleById(saleId)
        if (sale != null) {
            saleDao.insertSale(sale.copy(invoicePdfPath = path))
        }
    }

    suspend fun getSaleWithItems(saleId: String): Pair<SaleEntity?, List<SaleItemEntity>> {
        val sale = saleDao.getSaleById(saleId)
        val items = saleDao.getSaleItems(saleId)
        return Pair(sale, items)
    }

    fun getSaleItemsByProduct(productId: String): Flow<List<SaleItemEntity>> {
        return saleDao.getSaleItemsByProduct(productId)
    }

    fun getSaleItemsInRange(start: Long, end: Long): Flow<List<SaleItemEntity>> {
        return saleDao.getSaleItemsInRange(start, end)
    }

    suspend fun recordReturn(saleReturn: SaleReturnEntity) {
        database.withTransaction {
            saleDao.insertSaleReturn(saleReturn)
            
            val product = productDao.getProductById(saleReturn.productId)
            if (product != null) {
                val previousStock = product.stockQuantity
                val newStock = previousStock + saleReturn.quantity
                productDao.updateProduct(product.copy(stockQuantity = newStock))
                
                stockMovementDao.insertStockMovement(StockMovementEntity(
                    id = UUID.randomUUID().toString(),
                    productId = saleReturn.productId,
                    type = "RETURN",
                    quantity = saleReturn.quantity,
                    previousStock = previousStock,
                    newStock = newStock,
                    referenceId = saleReturn.id,
                    notes = "Sale Return for Sale: ${saleReturn.saleId}"
                ))
            }
        }
    }

    suspend fun getSalePayments(saleId: String): List<PaymentEntity> {
        return paymentDao.getPaymentsByReference(saleId)
    }
}
