package com.sahil.retailcore.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index(value = ["modelNumber"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val sku: String?, // Barcode
    val name: String,
    val modelNumber: String, // Required and Unique
    val brand: String?,
    val categoryId: String,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val stockQuantity: Int,
    val minimumStock: Int,
    val location: String? = null,
    val notes: String? = null,
    val hsnCode: String? = null,
    val warrantyMonths: Int = 0,
    val productImage: String? = null,
    val gstPercent: Double = 18.0,
    
    // Common fields
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: Int = 0,
    val deleted: Boolean = false
)
