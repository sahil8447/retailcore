package com.sahil.retailcore.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey
    val id: String,
    val saleId: String,
    val productId: String,
    val quantity: Int,
    val unitPrice: Double, // Selling price after discount
    val purchasePrice: Double = 0.0, // Purchase price at time of sale for profit calculation
    val discount: Double = 0.0,
    val gstPercent: Double = 18.0,
    val subtotal: Double, // Total for this item (Qty * unitPrice)
    
    // Common fields
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: Int = 0,
    val deleted: Boolean = false
)
