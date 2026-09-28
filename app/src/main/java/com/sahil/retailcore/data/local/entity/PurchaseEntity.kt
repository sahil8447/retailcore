package com.sahil.retailcore.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey
    val id: String,
    val supplierId: String,
    val invoiceNumber: String,
    val invoiceDate: Long,
    val paymentStatus: String, // e.g., Paid, Pending, Partial
    val notes: String?,
    val subtotal: Double,
    val totalGST: Double,
    val grandTotal: Double,
    val invoicePdfPath: String? = null,
    
    // Common fields
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: Int = 0,
    val deleted: Boolean = false
)
