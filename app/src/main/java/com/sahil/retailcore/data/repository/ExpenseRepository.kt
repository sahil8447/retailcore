package com.sahil.retailcore.data.repository

import com.sahil.retailcore.data.local.dao.ExpenseDao
import com.sahil.retailcore.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    suspend fun getExpenseById(id: String): ExpenseEntity? {
        return expenseDao.getExpenseById(id)
    }

    suspend fun insertExpense(expense: ExpenseEntity) {
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(id: String) {
        expenseDao.softDeleteExpense(id, System.currentTimeMillis())
    }
}
