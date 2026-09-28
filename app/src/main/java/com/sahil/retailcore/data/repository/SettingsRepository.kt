package com.sahil.retailcore.data.repository

import com.sahil.retailcore.data.local.dao.SettingsDao
import com.sahil.retailcore.data.local.entity.SettingsEntity
import java.util.UUID

class SettingsRepository(private val settingsDao: SettingsDao) {

    suspend fun getSetting(key: String): String? {
        return settingsDao.getSettingByKey(key)?.value
    }

    suspend fun saveSetting(key: String, value: String) {
        val existing = settingsDao.getSettingByKey(key)
        val setting = SettingsEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            key = key,
            value = value,
            updatedAt = System.currentTimeMillis()
        )
        settingsDao.saveSetting(setting)
    }

    suspend fun getNextInvoiceNumber(prefix: String, counterKey: String): String {
        val currentCount = getSetting(counterKey)?.toIntOrNull() ?: 0
        val nextCount = currentCount + 1
        saveSetting(counterKey, nextCount.toString())
        
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return "$prefix-$year-${String.format("%06d", nextCount)}"
    }
}
