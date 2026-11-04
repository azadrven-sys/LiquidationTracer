package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey
    val id: Int = 1,
    // Varsayılanlar
    val defaultTargetPercentage: Double = 1.0, // Varsayılan hedef satış yüzdesi (%)
    val defaultUnit: String = "USDT",
    val defaultAmount: Double = 1.0,           // Varsayılan miktar
    
    // Yapılandırma
    val currencySymbol: String = ""
)
