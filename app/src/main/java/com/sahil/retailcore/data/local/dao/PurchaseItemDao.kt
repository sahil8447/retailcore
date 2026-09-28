package com.sahil.retailcore.data.local.dao

import androidx.room.*
import com.sahil.retailcore.data.local.entity.PurchaseItemEntity

@Dao
interface PurchaseItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItemEntity>): List<Long>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId AND deleted = 0")
    suspend fun getItemsByPurchaseId(purchaseId: String): List<PurchaseItemEntity>
}
