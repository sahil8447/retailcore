package com.sahil.retailcore

import android.app.Application
import androidx.room.Room
import com.sahil.retailcore.data.local.database.RetailDatabase
import com.sahil.retailcore.data.repository.*
import com.sahil.retailcore.utils.BackupScheduler

class RetailApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BackupScheduler.scheduleDailyBackup(this)
    }

    val database by lazy {
        Room.databaseBuilder(
            this,
            RetailDatabase::class.java,
            "retail_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    val productRepository by lazy {
        ProductRepository(database.productDao())
    }

    val settingsRepository by lazy {
        SettingsRepository(database.settingsDao())
    }

    val supplierRepository by lazy {
        SupplierRepository(database.supplierDao())
    }

    val customerRepository by lazy {
        CustomerRepository(database.customerDao())
    }

    val expenseRepository by lazy {
        ExpenseRepository(database.expenseDao())
    }

    val paymentRepository by lazy {
        PaymentRepository(database.paymentDao())
    }

    val purchaseRepository by lazy {
        PurchaseRepository(
            database.purchaseDao(),
            database.purchaseItemDao(),
            database.stockMovementDao(),
            database.productDao(),
            database.supplierDao(),
            database.paymentDao(),
            database
        )
    }

    val saleRepository by lazy {
        SaleRepository(
            database.saleDao(),
            database.productDao(),
            database.stockMovementDao(),
            database.customerDao(),
            database.paymentDao(),
            database
        )
    }

    val stockRepository by lazy {
        StockRepository(
            database.stockEntryDao(),
            database.productDao(),
            database.stockMovementDao(),
            database.supplierDao(),
            database.paymentDao(),
            database
        )
    }

    fun closeDatabase() {
        if (database.isOpen) {
            database.close()
        }
    }
}
