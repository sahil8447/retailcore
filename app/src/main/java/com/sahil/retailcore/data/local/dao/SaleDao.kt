package com.sahil.retailcore.data.local.dao

import androidx.room.*
import com.sahil.retailcore.data.local.entity.SaleEntity
import com.sahil.retailcore.data.local.entity.SaleItemEntity
import com.sahil.retailcore.data.local.entity.SaleReturnEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE deleted = 0")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>): List<Long>

    @Query("SELECT * FROM sales WHERE id = :saleId AND deleted = 0")
    suspend fun getSaleById(saleId: String): SaleEntity?

    @Transaction
    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItems(saleId: String): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items WHERE productId = :productId ORDER BY createdAt DESC")
    fun getSaleItemsByProduct(productId: String): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sale_items WHERE createdAt BETWEEN :start AND :end AND deleted = 0")
    fun getSaleItemsInRange(start: Long, end: Long): Flow<List<SaleItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleReturn(saleReturn: SaleReturnEntity): Long

    @Query("SELECT * FROM sale_returns WHERE saleId = :saleId")
    suspend fun getReturnsBySale(saleId: String): List<SaleReturnEntity>
}
