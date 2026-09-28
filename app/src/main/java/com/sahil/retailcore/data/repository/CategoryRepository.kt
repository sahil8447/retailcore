package com.sahil.retailcore.data.repository

import com.sahil.retailcore.data.local.dao.CategoryDao
import com.sahil.retailcore.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategory(id: String) {
        categoryDao.softDeleteCategory(id, System.currentTimeMillis())
    }
}
