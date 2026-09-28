package com.sahil.retailcore.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_entries")
data class StockEntryEntity(
    @PrimaryKey
    val id: String,
    val productId: String,
    val quantity: Int,
    val purchasePrice: Double,
    val gstPercent: Double,
    val supplierId: String?,
    val amountPaid: Double = 0.0,
    val paymentMode: String? = null,
    val notes: String?,
    val entryDate: Long,
    
    // Common fields
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: Int = 0,
    val deleted: Boolean = false
)
