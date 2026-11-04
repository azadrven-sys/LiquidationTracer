package com.example.util

import com.example.data.model.LiquidationRecord
import com.example.data.model.UnitItem

/**
 * Utility functions for correlating liquidation records with live unit prices
 * and evaluating target price eligibility.
 */
object MarketUtils {

    /**
     * Matches a LiquidationRecord with the corresponding UnitItem from the units list
     * based on assetSymbol and unit name.
     */
    fun findMatchingUnit(record: LiquidationRecord, units: List<UnitItem>): UnitItem? {
        val cleanSymbol = record.assetSymbol.trim().uppercase()
        val cleanUnit = record.unit.trim().uppercase()

        // 1. Exact match by unit name (e.g. "BTC" == "BTC")
        units.firstOrNull { it.name.trim().equals(cleanSymbol, ignoreCase = true) }?.let { return it }

        // 2. Exact match by unit symbol (e.g. "BTC/USDT" == "BTC/USDT")
        units.firstOrNull { it.symbol.trim().equals(cleanSymbol, ignoreCase = true) }?.let { return it }

        // 3. Asset symbol contains unit name (e.g. "BTC/USDT" contains "BTC")
        units.firstOrNull { it.name.isNotBlank() && cleanSymbol.contains(it.name.trim(), ignoreCase = true) }?.let { return it }

        // 4. Unit symbol contains asset symbol (e.g. "BTC/USDT" contains "BTC")
        units.firstOrNull { cleanSymbol.isNotBlank() && it.symbol.trim().contains(cleanSymbol, ignoreCase = true) }?.let { return it }

        // 5. Match by unit field if not USDT or YOK
        if (cleanUnit.isNotBlank() && cleanUnit != "USDT" && cleanUnit != "YOK") {
            units.firstOrNull { it.name.trim().equals(cleanUnit, ignoreCase = true) }?.let { return it }
        }

        // 6. Default unit fallback
        return units.firstOrNull { it.isDefault } ?: units.firstOrNull()
    }

    /**
     * Checks if an active liquidation record has reached or exceeded its target selling price.
     * Güncel fiyat >= hedef fiyat olduğunda pozisyon satılmaya elverişlidir.
     */
    fun isEligibleForSale(record: LiquidationRecord, currentPrice: Double?): Boolean {
        if (record.isSold) return false
        if (currentPrice == null || currentPrice <= 0.0) return false
        return currentPrice >= record.targetPrice()
    }

    /**
     * Returns remaining percentage to reach target price if currentPrice < targetPrice.
     * E.g. target 1010, current 1000 -> 1.0%
     */
    fun distanceToTargetPercent(record: LiquidationRecord, currentPrice: Double?): Double? {
        if (currentPrice == null || currentPrice <= 0.0) return null
        val target = record.targetPrice()
        if (currentPrice >= target) return 0.0
        return ((target - currentPrice) / currentPrice) * 100.0
    }

    /**
     * Returns percentage difference between current price and buy price.
     */
    fun currentGainPercent(record: LiquidationRecord, currentPrice: Double?): Double? {
        if (currentPrice == null || currentPrice <= 0.0 || record.buyPrice <= 0.0) return null
        return ((currentPrice - record.buyPrice) / record.buyPrice) * 100.0
    }

    /**
     * Returns live unrealized PnL (profit/loss) based on current market price.
     */
    fun liveUnrealizedPnl(record: LiquidationRecord, currentPrice: Double?): Double? {
        if (currentPrice == null || currentPrice <= 0.0) return null
        return (currentPrice - record.buyPrice) * record.amount
    }
}
