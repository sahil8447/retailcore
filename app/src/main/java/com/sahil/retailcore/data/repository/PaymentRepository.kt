package com.sahil.retailcore.data.repository

import com.sahil.retailcore.data.local.dao.PaymentDao
import com.sahil.retailcore.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

class PaymentRepository(private val paymentDao: PaymentDao) {
    fun getPaymentsByCustomer(customerId: String): Flow<List<PaymentEntity>> =
        paymentDao.getPaymentsByCustomer(customerId)

    fun getPaymentsBySupplier(supplierId: String): Flow<List<PaymentEntity>> =
        paymentDao.getPaymentsBySupplier(supplierId)

    suspend fun insertPayment(payment: PaymentEntity) =
        paymentDao.insertPayment(payment)
}
