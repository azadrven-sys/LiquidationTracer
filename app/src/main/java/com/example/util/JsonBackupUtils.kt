package com.example.util

import com.example.data.model.AppConfig
import com.example.data.model.LiquidationRecord
import com.example.data.model.UnitItem
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val records: List<LiquidationRecord>? = null,
    val units: List<UnitItem>? = null,
    val config: AppConfig? = null
)

object JsonBackupUtils {

    /**
     * Kullanıcı verilerini (kayıtlar, birimler, yapılandırma) biçimlendirilmiş JSON dizesine dönüştürür.
     * Not: "id" ve "symbol" kullanılmaz; birim isimleri kendine özgüdür ve kayıtlar tarih-saate göre benzersizdir.
     */
    fun exportToJson(
        records: List<LiquidationRecord>,
        units: List<UnitItem>,
        config: AppConfig
    ): String {
        val root = JSONObject()

        // 1. Config
        val configObj = JSONObject().apply {
            put("currencySymbol", config.currencySymbol)
            put("defaultTargetPercentage", config.defaultTargetPercentage)
            put("defaultUnit", config.defaultUnit)
            put("defaultAmount", config.defaultAmount)
        }
        root.put("config", configObj)

        // 2. Units (id ve symbol yok)
        val unitsArray = JSONArray()
        for (u in units) {
            val uObj = JSONObject().apply {
                put("name", u.name)
                put("currentPrice", u.currentPrice)
                if (u.isDefault) put("isDefault", true)
            }
            unitsArray.put(uObj)
        }
        root.put("units", unitsArray)

        // 3. Records (id yok)
        val recordsArray = JSONArray()
        for (r in records) {
            val rObj = JSONObject().apply {
                put("assetSymbol", r.assetSymbol)
                put("buyPrice", r.buyPrice)
                put("amount", r.amount)
                put("targetPercentage", r.targetPercentage)
                put("unit", r.unit)
                if (r.notes.isNotBlank()) put("notes", r.notes)
                put("isSold", r.isSold)
                if (r.salePrice != null) put("salePrice", r.salePrice)
                if (r.salePnl != null) put("salePnl", r.salePnl)
                if (r.saleDate != null) put("saleDate", r.saleDate)
                put("createdAt", r.createdAt)
                if (r.updatedAt != null) put("updatedAt", r.updatedAt)
            }
            recordsArray.put(rObj)
        }
        root.put("records", recordsArray)

        return root.toString(2)
    }

    /**
     * JSON dizesini ayrıştırarak BackupData nesnesine dönüştürür.
     * Hem tam JSON nesnesini (config, units, records) hem de yalnızca kayıt dizisi [ ... ] formatını destekler.
     */
    fun parseBackupJson(jsonString: String): Result<BackupData> {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("JSON metni boş olamaz."))
        }

        return try {
            if (trimmed.startsWith("[")) {
                // Sadece kayıtlar dizisi verilmiş
                val array = JSONArray(trimmed)
                val records = parseRecordsArray(array)
                Result.success(BackupData(records = records))
            } else if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)

                val records = if (root.has("records")) {
                    parseRecordsArray(root.getJSONArray("records"))
                } else null

                val units = if (root.has("units")) {
                    parseUnitsArray(root.getJSONArray("units"))
                } else null

                val config = if (root.has("config")) {
                    parseConfigObject(root.getJSONObject("config"))
                } else null

                if (records == null && units == null && config == null) {
                    Result.failure(IllegalArgumentException("JSON geçerli 'records', 'units' veya 'config' alanı içermiyor."))
                } else {
                    Result.success(BackupData(records = records, units = units, config = config))
                }
            } else {
                Result.failure(IllegalArgumentException("Geçersiz JSON formatı: Süslü parantez '{' veya köşeli parantez '[' ile başlamalıdır."))
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("JSON ayrıştırma hatası: ${e.localizedMessage ?: e.message}"))
        }
    }

    private fun parseRecordsArray(array: JSONArray): List<LiquidationRecord> {
        val list = mutableListOf<LiquidationRecord>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val record = LiquidationRecord(
                id = 0L,
                assetSymbol = obj.optString("assetSymbol", obj.optString("symbol", "VARLIK")).trim().uppercase(),
                buyPrice = obj.optDouble("buyPrice", obj.optDouble("price", 0.0)),
                amount = obj.optDouble("amount", 1.0),
                targetPercentage = obj.optDouble("targetPercentage", 1.0),
                notes = obj.optString("notes", ""),
                unit = obj.optString("unit", "USDT"),
                isSold = obj.optBoolean("isSold", false),
                salePrice = if (!obj.isNull("salePrice")) obj.optDouble("salePrice") else null,
                salePnl = if (!obj.isNull("salePnl")) obj.optDouble("salePnl") else null,
                saleDate = if (!obj.isNull("saleDate")) obj.optLong("saleDate") else null,
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = if (!obj.isNull("updatedAt")) obj.optLong("updatedAt") else null
            )
            list.add(record)
        }
        return list
    }

    private fun parseUnitsArray(array: JSONArray): List<UnitItem> {
        val list = mutableListOf<UnitItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val name = obj.optString("name", "UNIT").trim().uppercase()
            val unit = UnitItem(
                id = 0L,
                name = name,
                symbol = name,
                currentPrice = obj.optDouble("currentPrice", 0.0),
                lastUpdated = System.currentTimeMillis(),
                isDefault = obj.optBoolean("isDefault", false),
                isSystem = false
            )
            list.add(unit)
        }
        return list
    }

    private fun parseConfigObject(obj: JSONObject): AppConfig {
        return AppConfig(
            id = 1,
            defaultTargetPercentage = obj.optDouble("defaultTargetPercentage", 1.0),
            defaultUnit = obj.optString("defaultUnit", "USDT"),
            defaultAmount = obj.optDouble("defaultAmount", 1.0),
            currencySymbol = FormatUtils.sanitizeCurrency(obj.optString("currencySymbol", ""))
        )
    }
}
