package com.sahil.retailcore.data.local.dao

import androidx.room.*
import com.sahil.retailcore.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE deleted = 0")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id AND deleted = 0")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE phone = :phone AND deleted = 0 LIMIT 1")
    suspend fun getCustomerByPhone(phone: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE gstNumber = :gstNumber AND deleted = 0 LIMIT 1")
    suspend fun getCustomerByGst(gstNumber: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Query("UPDATE customers SET deleted = 1, syncStatus = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteCustomer(id: String, timestamp: Long): Int
}
