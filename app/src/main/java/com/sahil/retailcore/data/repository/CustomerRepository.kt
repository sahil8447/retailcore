package com.sahil.retailcore.data.repository

import com.sahil.retailcore.data.local.dao.CustomerDao
import com.sahil.retailcore.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

class CustomerRepository(private val customerDao: CustomerDao) {
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    suspend fun insertCustomer(customer: CustomerEntity) {
        customerDao.insertCustomer(customer)
    }

    suspend fun getCustomerById(id: String): CustomerEntity? {
        return customerDao.getCustomerById(id)
    }

    suspend fun getCustomerByPhone(phone: String): CustomerEntity? {
        return customerDao.getCustomerByPhone(phone)
    }

    suspend fun getCustomerByGst(gstNumber: String): CustomerEntity? {
        return customerDao.getCustomerByGst(gstNumber)
    }

    suspend fun deleteCustomer(id: String) {
        customerDao.softDeleteCustomer(id, System.currentTimeMillis())
    }
}
