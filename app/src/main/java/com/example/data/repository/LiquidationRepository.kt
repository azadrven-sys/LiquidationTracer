package com.example.data.repository

import com.example.data.api.PriceApiService
import com.example.data.local.ConfigDao
import com.example.data.local.LiquidationDao
import com.example.data.local.UnitDao
import com.example.data.model.AppConfig
import com.example.data.model.LiquidationRecord
import com.example.data.model.UnitItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class LiquidationRepository(
    private val liquidationDao: LiquidationDao,
    private val unitDao: UnitDao,
    private val configDao: ConfigDao,
    private val priceApiService: PriceApiService = PriceApiService()
) {
    val allRecords: Flow<List<LiquidationRecord>> = liquidationDao.getAllRecords()
    val activeRecords: Flow<List<LiquidationRecord>> = liquidationDao.getActiveRecords()
    val soldRecords: Flow<List<LiquidationRecord>> = liquidationDao.getSoldRecords()
    val allUnits: Flow<List<UnitItem>> = unitDao.getAllUnits()
    val appConfig: Flow<AppConfig?> = configDao.getConfig().map { cfg ->
        cfg?.let {
            if (it.currencySymbol.contains("$")) {
                it.copy(currencySymbol = it.currencySymbol.replace("$", "").trim())
            } else {
                it
            }
        }
    }

    suspend fun insertRecord(record: LiquidationRecord): Long {
        return liquidationDao.insertRecord(record)
    }

    suspend fun updateRecord(record: LiquidationRecord) {
        liquidationDao.updateRecord(record)
    }

    suspend fun deleteRecord(record: LiquidationRecord) {
        liquidationDao.deleteRecord(record)
    }

    suspend fun markAsSold(id: Long, salePrice: Double) {
        val record = liquidationDao.getRecordById(id) ?: return
        val pnl = (salePrice - record.buyPrice) * record.amount
        val now = System.currentTimeMillis()
        liquidationDao.markAsSold(id, salePrice, pnl, now)
    }

    suspend fun reopenRecord(id: Long) {
        liquidationDao.reopenRecord(id)
    }

    // Units management
    suspend fun insertUnit(unit: UnitItem) {
        unitDao.insertUnit(unit)
    }

    suspend fun deleteUnit(unit: UnitItem) {
        unitDao.deleteUnit(unit)
    }

    suspend fun setDefaultUnit(id: Long) {
        unitDao.setDefaultUnit(id)
    }

    suspend fun updateUnitManualPrice(id: Long, price: Double) {
        unitDao.updatePrice(id, price, System.currentTimeMillis())
    }

    /**
     * API yoluyla tüm birimlerin güncel fiyatlarını otomatik güncelle
     */
    suspend fun refreshPricesFromApi(): Int {
        val marketPrices = priceApiService.fetchMarketPrices()
        if (marketPrices.isEmpty()) return 0

        val existingUnits = unitDao.getAllUnitsSync()
        var updatedCount = 0
        val now = System.currentTimeMillis()

        for (unit in existingUnits) {
            val cleanKey = unit.name.trim().uppercase()
            val apiPrice = marketPrices[cleanKey]
                ?: marketPrices["$cleanKey/USDT"]
                ?: marketPrices[unit.symbol.trim().uppercase()]

            if (apiPrice != null && apiPrice > 0.0) {
                unitDao.updatePrice(unit.id, apiPrice, now)
                updatedCount++
            }
        }
        return updatedCount
    }

    // Config
    suspend fun updateConfig(config: AppConfig) {
        configDao.updateConfig(config.copy(currencySymbol = config.currencySymbol.replace("$", "").trim()))
    }

    /**
     * JSON yedeğinden kullanıcı verilerini (kayıtlar, birimler, ayarlar) veri tabanına geri yükler.
     */
    suspend fun restoreAllData(
        records: List<LiquidationRecord>?,
        units: List<UnitItem>?,
        config: AppConfig?
    ) {
        if (records != null) {
            liquidationDao.deleteAllRecords()
            liquidationDao.insertAll(records)
        }
        if (units != null && units.isNotEmpty()) {
            unitDao.deleteAllUnits()
            val hasDefault = units.any { it.isDefault }
            val unitsToInsert = if (!hasDefault) {
                units.mapIndexed { index, u -> if (index == 0) u.copy(isDefault = true) else u }
            } else {
                units
            }
            unitDao.insertAll(unitsToInsert)
        }
        if (config != null) {
            configDao.updateConfig(config.copy(currencySymbol = config.currencySymbol.replace("$", "").trim()))
        }
    }

    suspend fun ensureDefaultDataSeeded() {
        try {
            // Örnek sistem birimlerini sil (yeni kurulumlarda test birimleri eklenmez)
            unitDao.deleteSystemUnits()

            val existingConfig = configDao.getConfigSync()
            if (existingConfig == null) {
                configDao.insertConfig(
                    AppConfig(
                        id = 1,
                        defaultTargetPercentage = 1.0,
                        defaultUnit = "",
                        defaultAmount = 1.0,
                        currencySymbol = ""
                    )
                )
            } else if (existingConfig.currencySymbol.contains("$")) {
                configDao.updateConfig(
                    existingConfig.copy(currencySymbol = existingConfig.currencySymbol.replace("$", "").trim())
                )
            }

            // Önceden eklenmiş örnek test verilerini sil (yeni kurulumlarda test verisi eklenmez)
            val sampleNotes = listOf(
                "Direnç kırılımı sonrası",
                "Orta vadeli takip",
                "Destek seviyesinden alım",
                "Haftalık birikim",
                "Hedef fiyata ulaşıldı ve satıldı"
            )
            liquidationDao.deleteRecordsByNotes(sampleNotes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
