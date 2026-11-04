package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "units")
data class UnitItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                    // Varlık / Sembol adı (Örn: BTC, ETH, SOL, AVAX, XAU)
    val symbol: String,                  // Gösterim sembolü (Örn: BTC/USDT, ₿, ₺)
    val currentPrice: Double = 0.0,      // Güncel Piyasa Fiyatı
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false,
    val isSystem: Boolean = false
)
