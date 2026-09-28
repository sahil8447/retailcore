package com.sahil.retailcore.data.local.dao

import androidx.room.*
import com.sahil.retailcore.data.local.entity.SupplierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE deleted = 0")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id AND deleted = 0")
    suspend fun getSupplierById(id: String): SupplierEntity?

    @Query("SELECT * FROM suppliers WHERE phone = :phone AND deleted = 0 LIMIT 1")
    suspend fun getSupplierByPhone(phone: String): SupplierEntity?

    @Query("SELECT * FROM suppliers WHERE gstNumber = :gstNumber AND deleted = 0 LIMIT 1")
    suspend fun getSupplierByGst(gstNumber: String): SupplierEntity?

    @Query("SELECT * FROM suppliers WHERE name = :name AND deleted = 0 LIMIT 1")
    suspend fun getSupplierByName(name: String): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Query("UPDATE suppliers SET deleted = 1, syncStatus = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteSupplier(id: String, timestamp: Long): Int
}
