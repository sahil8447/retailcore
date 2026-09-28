package com.sahil.retailcore.data.repository

import com.sahil.retailcore.data.local.dao.SupplierDao
import com.sahil.retailcore.data.local.entity.SupplierEntity
import kotlinx.coroutines.flow.Flow

class SupplierRepository(private val supplierDao: SupplierDao) {
    val allSuppliers: Flow<List<SupplierEntity>> = supplierDao.getAllSuppliers()

    suspend fun insertSupplier(supplier: SupplierEntity) {
        supplierDao.insertSupplier(supplier)
    }

    suspend fun getSupplierById(id: String): SupplierEntity? {
        return supplierDao.getSupplierById(id)
    }

    suspend fun getSupplierByPhone(phone: String): SupplierEntity? {
        return supplierDao.getSupplierByPhone(phone)
    }

    suspend fun getSupplierByGst(gst: String): SupplierEntity? {
        return supplierDao.getSupplierByGst(gst)
    }

    suspend fun deleteSupplier(id: String) {
        supplierDao.softDeleteSupplier(id, System.currentTimeMillis())
    }
}
