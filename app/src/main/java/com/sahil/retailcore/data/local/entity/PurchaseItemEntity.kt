package com.sahil.retailcore.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchase_items")
data class PurchaseItemEntity(
    @PrimaryKey
    val id: String,
    val purchaseId: String,
    val productId: String,
    val quantity: Int,
    val purchasePriceWithoutGST: Double,
    val gstPercent: Double,
    val gstAmount: Double,
    val purchasePriceWithGST: Double,
    val lineTotal: Double,
    
    // Common fields
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: Int = 0,
    val deleted: Boolean = false
)
