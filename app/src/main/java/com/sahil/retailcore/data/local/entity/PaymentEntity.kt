package com.sahil.retailcore.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey
    val id: String,
    val customerId: String? = null,
    val supplierId: String? = null,
    val amount: Double,
    val paymentMode: String, // Cash, UPI, Card, etc.
    val type: String, // RECEIVED, PAID
    val referenceId: String? = null, // Sale ID or Purchase ID
    val notes: String? = null,
    val paymentDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false
)

object PaymentType {
    const val RECEIVED = "RECEIVED"
    const val PAID = "PAID"
}
