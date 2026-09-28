package com.sahil.retailcore.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey
    val id: String,
    val invoiceNumber: String,
    val customerId: String?,
    val totalAmount: Double,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val paymentMethod: String,
    val status: String, // e.g., Completed, Pending, Cancelled
    val invoicePdfPath: String? = null,
    
    // Common fields
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: Int = 0,
    val deleted: Boolean = false
)
