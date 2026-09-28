package com.sahil.retailcore.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String, // Made mandatory for matching
    val contactName: String? = null,
    val email: String? = null,
    val address: String? = null,
    val gstNumber: String? = null,
    val totalPurchases: Double = 0.0,
    val totalPaid: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val lastPurchaseDate: Long? = null,
    
    // Common fields
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: Int = 0,
    val deleted: Boolean = false
)
