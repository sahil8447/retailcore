package com.sahil.retailcore.data.local.dao

import androidx.room.*
import com.sahil.retailcore.data.local.entity.StockEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockEntryDao {
    @Query("SELECT * FROM stock_entries WHERE deleted = 0 ORDER BY entryDate DESC")
    fun getAllStockEntries(): Flow<List<StockEntryEntity>>

    @Query("SELECT * FROM stock_entries WHERE productId = :productId AND deleted = 0 ORDER BY entryDate DESC")
    fun getStockEntriesByProduct(productId: String): Flow<List<StockEntryEntity>>

    @Query("SELECT * FROM stock_entries WHERE id = :id AND deleted = 0")
    suspend fun getStockEntryById(id: String): StockEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockEntry(entry: StockEntryEntity): Long

    @Update
    suspend fun updateStockEntry(entry: StockEntryEntity): Int

    @Query("UPDATE stock_entries SET deleted = 1, syncStatus = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteStockEntry(id: String, timestamp: Long): Int
}
