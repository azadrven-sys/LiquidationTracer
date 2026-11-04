package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "liquidation_records")
data class LiquidationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assetSymbol: String,           // Varlık / Sembol
    val buyPrice: Double,              // Alış Fiyatı
    val amount: Double,                // Miktar
    val targetPercentage: Double = 1.0,// Hedef Satış Yüzdesi (%1, %2 vb.)
    val notes: String = "",            // Notlar
    val unit: String = "USDT",         // Birim (varsayılandan gelir)
    val isSold: Boolean = false,       // Satıldı mı?
    val salePrice: Double? = null,     // Satış Fiyatı
    val salePnl: Double? = null,       // Gerçekleşen Kâr/Zarar
    val saleDate: Long? = null,        // Satış Zamanı
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null        // Düzenlenme Zamanı
) {
    /**
     * Toplam Alış Maliyeti = Alış Fiyatı * Miktar
     */
    fun totalCost(): Double {
        return buyPrice * amount
    }

    /**
     * Hedef Satış Fiyatı = Alış Fiyatı * (1 + targetPercentage / 100)
     * Örn: Alış 1000, Hedef %1 -> 1000 * (1 + 0.01) = 1010
     */
    fun targetPrice(): Double {
        return buyPrice * (1.0 + (targetPercentage / 100.0))
    }

    /**
     * Hedef Net Kâr = (Hedef Fiyat - Alış Fiyatı) * Miktar
     */
    fun targetProfit(): Double {
        return (targetPrice() - buyPrice) * amount
    }

    /**
     * Satış yapıldıysa gerçekleşen kâr/zarar
     */
    fun calculatePnl(exitPrice: Double): Double {
        return (exitPrice - buyPrice) * amount
    }

    /**
     * Satış kâr/zarar yüzdesi
     */
    fun calculatePnlPercent(exitPrice: Double): Double {
        if (buyPrice <= 0.0) return 0.0
        return ((exitPrice - buyPrice) / buyPrice) * 100.0
    }
}
