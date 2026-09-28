package com.sahil.retailcore.data.repository

import com.sahil.retailcore.data.local.dao.ProductDao
import com.sahil.retailcore.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

    suspend fun getProductById(id: String): ProductEntity? {
        return productDao.getProductById(id)
    }

    suspend fun getProductByModelNumber(modelNumber: String): ProductEntity? {
        return productDao.getProductByModelNumber(modelNumber)
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        return productDao.getProductByBarcode(barcode)
    }

    suspend fun insertProduct(product: ProductEntity) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(id: String) {
        productDao.softDeleteProduct(id, System.currentTimeMillis())
    }

    fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return productDao.searchProducts(query)
    }
}
