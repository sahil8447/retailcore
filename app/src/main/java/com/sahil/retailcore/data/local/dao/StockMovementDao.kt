package com.sahil.retailcore.data.local.dao

import androidx.room.*
import com.sahil.retailcore.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity): Long

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY createdAt DESC")
    fun getMovementsByProduct(productId: String): Flow<List<StockMovementEntity>>
}
