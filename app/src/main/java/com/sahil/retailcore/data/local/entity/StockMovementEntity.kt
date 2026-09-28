package com.sahil.retailcore.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey
    val id: String,
    val productId: String,
    val type: String, // PURCHASE, SALE, ADJUSTMENT
    val quantity: Int,
    val previousStock: Int,
    val newStock: Int,
    val referenceId: String, // purchaseId or saleId
    val notes: String?,
    val createdAt: Long = System.currentTimeMillis(),
    
    // Additional tracking for sync
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: Int = 0,
    val deleted: Boolean = false
)

object StockMovementType {
    const val PURCHASE = "PURCHASE"
    const val SALE = "SALE"
    const val ADJUSTMENT = "ADJUSTMENT"
    const val OPENING_STOCK = "OPENING_STOCK"
}
