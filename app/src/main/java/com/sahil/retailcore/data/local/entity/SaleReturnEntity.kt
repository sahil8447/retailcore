package com.sahil.retailcore.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sale_returns")
data class SaleReturnEntity(
    @PrimaryKey
    val id: String,
    val saleId: String,
    val productId: String,
    val quantity: Int,
    val refundAmount: Double,
    val reason: String?,
    val returnDate: Long = System.currentTimeMillis(),
    
    // Common fields
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: Int = 0,
    val deleted: Boolean = false
)
