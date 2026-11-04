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

    suspend fun ensureDefaultDataSeeded() {
        try {
            val unitCount = unitDao.getCount()
            if (unitCount == 0) {
                val defaultUnits = listOf(
                    UnitItem(name = "BTC", symbol = "BTC/USDT", currentPrice = 68450.0, isDefault = true, isSystem = true),
                    UnitItem(name = "ETH", symbol = "ETH/USDT", currentPrice = 3520.0, isDefault = false, isSystem = true),
                    UnitItem(name = "SOL", symbol = "SOL/USDT", currentPrice = 158.0, isDefault = false, isSystem = true),
                    UnitItem(name = "AVAX", symbol = "AVAX/USDT", currentPrice = 29.2, isDefault = false, isSystem = true),
                    UnitItem(name = "XRP", symbol = "XRP/USDT", currentPrice = 0.58, isDefault = false, isSystem = true),
                    UnitItem(name = "DOGE", symbol = "DOGE/USDT", currentPrice = 0.145, isDefault = false, isSystem = true),
                    UnitItem(name = "PAXG", symbol = "PAXG/USDT (Altın)", currentPrice = 2680.0, isDefault = false, isSystem = true),
                    UnitItem(name = "USD", symbol = "USD", currentPrice = 1.0, isDefault = false, isSystem = true),
                    UnitItem(name = "TRY", symbol = "₺", currentPrice = 0.029, isDefault = false, isSystem = true)
                )
                unitDao.insertAll(defaultUnits)
            } else {
                // Sanitize any existing units that had '$'
                val existingUnits = unitDao.getAllUnitsSync()
                existingUnits.forEach { u ->
                    if (u.symbol.contains("$")) {
                        unitDao.updateUnit(u.copy(symbol = u.symbol.replace("$", "").ifBlank { "USD" }))
                    }
                }
            }

            val existingConfig = configDao.getConfigSync()
            if (existingConfig == null) {
                configDao.insertConfig(
                    AppConfig(
                        id = 1,
                        defaultTargetPercentage = 1.0,
                        defaultUnit = "BTC",
                        defaultAmount = 1.0,
                        currencySymbol = ""
                    )
                )
            } else if (existingConfig.currencySymbol.contains("$")) {
                configDao.updateConfig(
                    existingConfig.copy(currencySymbol = existingConfig.currencySymbol.replace("$", "").trim())
                )
            }

            val recordCount = liquidationDao.getCount()
            if (recordCount == 0) {
                val now = System.currentTimeMillis()
                val sampleRecords = listOf(
                    LiquidationRecord(
                        assetSymbol = "BTC",
                        buyPrice = 64500.0,
                        amount = 0.5,
                        targetPercentage = 1.0,
                        unit = "USDT",
                        notes = "Direnç kırılımı sonrası",
                        createdAt = now - 3600000 * 24 * 3
                    ),
                    LiquidationRecord(
                        assetSymbol = "ETH",
                        buyPrice = 3450.0,
                        amount = 4.0,
                        targetPercentage = 2.0,
                        unit = "USDT",
                        notes = "Orta vadeli takip",
                        createdAt = now - 3600000 * 18
                    ),
                    LiquidationRecord(
                        assetSymbol = "SOL",
                        buyPrice = 145.0,
                        amount = 25.0,
                        targetPercentage = 3.0,
                        unit = "USDT",
                        notes = "Destek seviyesinden alım",
                        createdAt = now - 3600000 * 12
                    ),
                    LiquidationRecord(
                        assetSymbol = "AVAX",
                        buyPrice = 28.5,
                        amount = 150.0,
                        targetPercentage = 1.5,
                        unit = "USDT",
                        notes = "Haftalık birikim",
                        createdAt = now - 3600000 * 8
                    ),
                    LiquidationRecord(
                        assetSymbol = "XRP",
                        buyPrice = 0.55,
                        amount = 5000.0,
                        targetPercentage = 12.0,
                        unit = "USDT",
                        notes = "Hedef fiyata ulaşıldı ve satıldı",
                        createdAt = now - 3600000 * 24 * 7,
                        isSold = true,
                        salePrice = 0.62,
                        salePnl = 350.0,
                        saleDate = now - 3600000 * 5
                    )
                )
                liquidationDao.insertAll(sampleRecords)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
