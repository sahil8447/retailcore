package com.sahil.retailcore.data.local.dao

import androidx.room.*
import com.sahil.retailcore.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE deleted = 0")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id AND deleted = 0")
    suspend fun getProductById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity): Int

    @Query("UPDATE products SET deleted = 1, syncStatus = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteProduct(id: String, timestamp: Long): Int

    @Query("SELECT * FROM products WHERE syncStatus = 0")
    suspend fun getUnsyncedProducts(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE deleted = 0 AND (name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR modelNumber LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%')")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE modelNumber = :modelNumber AND deleted = 0 LIMIT 1")
    suspend fun getProductByModelNumber(modelNumber: String): ProductEntity?

    @Query("SELECT * FROM products WHERE sku = :barcode AND deleted = 0 LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?
}
