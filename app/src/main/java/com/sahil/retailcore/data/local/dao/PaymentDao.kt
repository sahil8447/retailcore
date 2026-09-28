package com.sahil.retailcore.data.local.dao

import androidx.room.*
import com.sahil.retailcore.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE customerId = :customerId AND deleted = 0 ORDER BY paymentDate DESC")
    fun getPaymentsByCustomer(customerId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE supplierId = :supplierId AND deleted = 0 ORDER BY paymentDate DESC")
    fun getPaymentsBySupplier(supplierId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE referenceId = :referenceId AND deleted = 0")
    suspend fun getPaymentsByReference(referenceId: String): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Query("UPDATE payments SET deleted = 1 WHERE id = :id")
    suspend fun softDeletePayment(id: String): Int
}
