package com.sahil.retailcore.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String, // Made mandatory for matching
    val altPhone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val gstNumber: String? = null,
    val openingBalance: Double = 0.0,
    val openingBalanceDate: Long = System.currentTimeMillis(),
    val totalSales: Double = 0.0,
    val totalReceived: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val lastPurchaseDate: Long? = null,
    val notes: String? = null,
    
    // Common fields
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: Int = 0,
    val deleted: Boolean = false
)
