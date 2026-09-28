package com.sahil.retailcore.data.local.dao

import androidx.room.*
import com.sahil.retailcore.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE deleted = 0")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("UPDATE categories SET deleted = 1, syncStatus = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteCategory(id: String, timestamp: Long): Int
}
