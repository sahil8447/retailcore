package com.sahil.retailcore.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sahil.retailcore.data.local.dao.*
import com.sahil.retailcore.data.local.entity.*

@Database(
    entities = [
        ProductEntity::class,
        CategoryEntity::class,
        SupplierEntity::class,
        CustomerEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        ExpenseEntity::class,
        SettingsEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        StockMovementEntity::class,
        StockEntryEntity::class,
        PaymentEntity::class,
        SaleReturnEntity::class
    ],
    version = 17,
    exportSchema = false
)
abstract class RetailDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun supplierDao(): SupplierDao
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun settingsDao(): SettingsDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun purchaseItemDao(): PurchaseItemDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun stockEntryDao(): StockEntryDao
    abstract fun paymentDao(): PaymentDao
}
