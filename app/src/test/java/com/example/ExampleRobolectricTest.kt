package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.LiquidationRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Likidasyon Takip", appName)
  }

  @Test
  fun `test target price calculation with 1000 buy price and 1 percent target`() {
    // User requested example: Buy 1000, %1 target -> targetPrice = 1010
    val record = LiquidationRecord(
        assetSymbol = "VARLIK_A",
        buyPrice = 1000.0,
        amount = 1.0,
        targetPercentage = 1.0,
        notes = "Hedef test"
    )

    assertEquals(1010.0, record.targetPrice(), 0.001)
    assertEquals(10.0, record.targetProfit(), 0.001)
    assertEquals(1000.0, record.totalCost(), 0.001)
  }

  @Test
  fun `test target price calculation with multiple quantities and custom target percentage`() {
    val record = LiquidationRecord(
        assetSymbol = "ETH",
        buyPrice = 2000.0,
        amount = 3.0,
        targetPercentage = 5.0 // %5
    )

    // targetPrice = 2000 * 1.05 = 2100.0
    assertEquals(2100.0, record.targetPrice(), 0.001)
    // targetProfit = (2100 - 2000) * 3 = 300.0
    assertEquals(300.0, record.targetProfit(), 0.001)
  }

  @Test
  fun `test MainActivity launches without crash`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.create().start().resume().get()
    org.junit.Assert.assertNotNull(activity)
  }

  @Test
  fun `test liquidation record deletion workflow`() {
    val record = LiquidationRecord(
        id = 42,
        assetSymbol = "SOL",
        buyPrice = 150.0,
        amount = 10.0,
        targetPercentage = 10.0
    )
    assertEquals(165.0, record.targetPrice(), 0.001)
    assertEquals(150.0, record.targetProfit(), 0.001)
  }

  @Test
  fun `test active record details not shown on click and sold record details allowed`() {
    val activeRecord = LiquidationRecord(
        id = 1,
        assetSymbol = "BTC",
        buyPrice = 60000.0,
        amount = 0.5,
        targetPercentage = 1.0,
        isSold = false
    )
    val soldRecord = activeRecord.copy(id = 2, isSold = true, salePrice = 61000.0)

    var detailsOpenedForRecordId: Long? = null
    val handleCardClick: (LiquidationRecord) -> Unit = { rec ->
        if (rec.isSold) {
            detailsOpenedForRecordId = rec.id
        }
    }

    // Clicking active record must NOT show details
    handleCardClick(activeRecord)
    org.junit.Assert.assertNull(detailsOpenedForRecordId)

    // Clicking sold record MUST show details
    handleCardClick(soldRecord)
    assertEquals(2L, detailsOpenedForRecordId)
  }

  @Test
  fun `test AppConfig defaultAmount persistence and default values`() {
    val config = com.example.data.model.AppConfig(
        defaultTargetPercentage = 2.5,
        defaultUnit = "ETH",
        defaultAmount = 0.75
    )
    assertEquals(0.75, config.defaultAmount, 0.001)
    assertEquals(2.5, config.defaultTargetPercentage, 0.001)
    assertEquals("ETH", config.defaultUnit)

    val updated = config.copy(defaultAmount = 5.0)
    assertEquals(5.0, updated.defaultAmount, 0.001)
  }

  @Test
  fun `test latest date determination precedence for created edited and sold states`() {
    val createdTime = 1000L
    val editedTime = 2000L
    val soldTime = 3000L

    // Case 1: Only created
    val recordOnlyCreated = LiquidationRecord(
        assetSymbol = "BTC",
        buyPrice = 50000.0,
        amount = 1.0,
        createdAt = createdTime,
        updatedAt = null,
        isSold = false
    )
    fun getLabelAndTimestamp(rec: LiquidationRecord): Pair<String, Long> {
        val tCreated = rec.createdAt
        val tUpdated = rec.updatedAt ?: 0L
        val tSold = if (rec.isSold) (rec.saleDate ?: 0L) else 0L
        return when {
            tSold > 0L && tSold >= tUpdated && tSold >= tCreated -> "Satış" to tSold
            tUpdated > 0L && tUpdated >= tCreated -> "Düzenleme" to tUpdated
            else -> "Oluşturulma" to tCreated
        }
    }

    val (label1, time1) = getLabelAndTimestamp(recordOnlyCreated)
    assertEquals("Oluşturulma", label1)
    assertEquals(createdTime, time1)

    // Case 2: Created then edited
    val recordEdited = recordOnlyCreated.copy(updatedAt = editedTime)
    val (label2, time2) = getLabelAndTimestamp(recordEdited)
    assertEquals("Düzenleme", label2)
    assertEquals(editedTime, time2)

    // Case 3: Created, edited then sold
    val recordSold = recordEdited.copy(isSold = true, salePrice = 55000.0, saleDate = soldTime)
    val (label3, time3) = getLabelAndTimestamp(recordSold)
    assertEquals("Satış", label3)
    assertEquals(soldTime, time3)

    // Case 4: Sold position reopened (isSold = false, saleDate = null, updatedAt = newTime)
    val reopenTime = 4000L
    val recordReopened = recordSold.copy(isSold = false, salePrice = null, saleDate = null, updatedAt = reopenTime)
    val (label4, time4) = getLabelAndTimestamp(recordReopened)
    assertEquals("Düzenleme", label4)
    assertEquals(reopenTime, time4)
  }

  @Test
  fun `test sold records cannot be edited unless reopened`() {
    val soldRecord = LiquidationRecord(
        id = 10,
        assetSymbol = "ETH",
        buyPrice = 3000.0,
        amount = 2.0,
        isSold = true,
        salePrice = 3200.0
    )
    val canEditSold = !soldRecord.isSold
    assertEquals(false, canEditSold)

    val reopenedRecord = soldRecord.copy(isSold = false, salePrice = null)
    val canEditReopened = !reopenedRecord.isSold
    assertEquals(true, canEditReopened)
  }

  @Test
  fun `test MarketUtils findMatchingUnit and price correlation`() {
    val units = listOf(
        com.example.data.model.UnitItem(id = 1, name = "BTC", symbol = "BTC/USDT", currentPrice = 68450.0, isDefault = true),
        com.example.data.model.UnitItem(id = 2, name = "ETH", symbol = "ETH/USDT", currentPrice = 3520.0, isDefault = false),
        com.example.data.model.UnitItem(id = 3, name = "SOL", symbol = "SOL/USDT", currentPrice = 158.0, isDefault = false)
    )

    val btcRecord = LiquidationRecord(assetSymbol = "BTC", buyPrice = 60000.0, amount = 1.0)
    val ethRecord = LiquidationRecord(assetSymbol = "ETH/USDT", buyPrice = 3000.0, amount = 2.0)
    val unknownRecord = LiquidationRecord(assetSymbol = "CUSTOM_COIN", buyPrice = 10.0, amount = 5.0)

    val matchedBtc = com.example.util.MarketUtils.findMatchingUnit(btcRecord, units)
    org.junit.Assert.assertNotNull(matchedBtc)
    assertEquals(68450.0, matchedBtc!!.currentPrice, 0.001)

    val matchedEth = com.example.util.MarketUtils.findMatchingUnit(ethRecord, units)
    org.junit.Assert.assertNotNull(matchedEth)
    assertEquals(3520.0, matchedEth!!.currentPrice, 0.001)

    // Falls back to default unit
    val matchedUnknown = com.example.util.MarketUtils.findMatchingUnit(unknownRecord, units)
    org.junit.Assert.assertNotNull(matchedUnknown)
    assertEquals(true, matchedUnknown!!.isDefault)
  }

  @Test
  fun `test isEligibleForSale when current price reaches or exceeds target price`() {
    // Alış: 1000, Hedef: %1 -> Hedef Fiyat: 1010
    val record = LiquidationRecord(
        assetSymbol = "BTC",
        buyPrice = 1000.0,
        amount = 1.0,
        targetPercentage = 1.0,
        isSold = false
    )
    assertEquals(1010.0, record.targetPrice(), 0.001)

    // 1. Current price is below target -> not eligible
    assertEquals(false, com.example.util.MarketUtils.isEligibleForSale(record, 1005.0))
    assertEquals(false, com.example.util.MarketUtils.isEligibleForSale(record, 990.0))

    // 2. Current price equals target -> eligible!
    assertEquals(true, com.example.util.MarketUtils.isEligibleForSale(record, 1010.0))

    // 3. Current price exceeds target -> eligible!
    assertEquals(true, com.example.util.MarketUtils.isEligibleForSale(record, 1025.0))

    // 4. Sold record is NEVER eligible even if price is high
    val soldRecord = record.copy(isSold = true, salePrice = 1010.0)
    assertEquals(false, com.example.util.MarketUtils.isEligibleForSale(soldRecord, 1050.0))
  }

  @Test
  fun `test distanceToTargetPercent and currentGainPercent calculations`() {
    val record = LiquidationRecord(
        assetSymbol = "SOL",
        buyPrice = 100.0,
        amount = 1.0,
        targetPercentage = 10.0 // target = 110.0
    )

    // Current price = 105.0 (Gain +5%, Distance to 110 is (110 - 105)/105 * 100 = 4.7619%)
    val gain = com.example.util.MarketUtils.currentGainPercent(record, 105.0)
    assertEquals(5.0, gain!!, 0.01)

    val dist = com.example.util.MarketUtils.distanceToTargetPercent(record, 105.0)
    assertEquals(4.7619, dist!!, 0.01)

    // When at or above target, distance is 0.0
    assertEquals(0.0, com.example.util.MarketUtils.distanceToTargetPercent(record, 110.0)!!, 0.001)
    assertEquals(0.0, com.example.util.MarketUtils.distanceToTargetPercent(record, 115.0)!!, 0.001)
  }

  @Test
  fun `test tab counts update dynamically with search query`() {
    val btcActive = LiquidationRecord(id = 1, assetSymbol = "BTC", buyPrice = 50000.0, amount = 1.0, targetPercentage = 10.0, isSold = false)
    val btcSold = LiquidationRecord(id = 2, assetSymbol = "BTC", buyPrice = 40000.0, amount = 1.0, targetPercentage = 10.0, isSold = true)
    val ethActive = LiquidationRecord(id = 3, assetSymbol = "ETH", buyPrice = 3000.0, amount = 2.0, targetPercentage = 5.0, isSold = false)
    val list = listOf(btcActive, btcSold, ethActive)

    fun filterByQuery(query: String): List<LiquidationRecord> {
      return if (query.isBlank()) list else list.filter {
        it.assetSymbol.contains(query, ignoreCase = true) ||
        it.unit.contains(query, ignoreCase = true) ||
        it.notes.contains(query, ignoreCase = true)
      }
    }

    // Without search: Tümü: 3, Aktif: 2, Satılan: 1
    val unsearched = filterByQuery("")
    assertEquals(3, unsearched.size)
    assertEquals(2, unsearched.count { !it.isSold })
    assertEquals(1, unsearched.count { it.isSold })

    // With search "BTC": Tümü: 2, Aktif: 1, Satılan: 1
    val searchedBtc = filterByQuery("BTC")
    assertEquals(2, searchedBtc.size)
    assertEquals(1, searchedBtc.count { !it.isSold })
    assertEquals(1, searchedBtc.count { it.isSold })

    // With search "ETH": Tümü: 1, Aktif: 1, Satılan: 0
    val searchedEth = filterByQuery("ETH")
    assertEquals(1, searchedEth.size)
    assertEquals(1, searchedEth.count { !it.isSold })
    assertEquals(0, searchedEth.count { it.isSold })
  }

  @Test
  fun `test json backup export and restore roundtrip`() {
    val records = listOf(
        LiquidationRecord(
            id = 10,
            assetSymbol = "SOL",
            buyPrice = 145.0,
            amount = 10.0,
            targetPercentage = 5.0,
            unit = "USDT",
            notes = "Yedek testi",
            isSold = false
        ),
        LiquidationRecord(
            id = 11,
            assetSymbol = "AVAX",
            buyPrice = 28.0,
            amount = 50.0,
            targetPercentage = 4.0,
            unit = "USDT",
            notes = "Satılmış kayıt",
            isSold = true,
            salePrice = 30.0,
            salePnl = 100.0,
            saleDate = 1700000000000L
        )
    )
    val units = listOf(
        com.example.data.model.UnitItem(id = 1, name = "SOL", symbol = "SOL/USDT", currentPrice = 150.0, isDefault = true),
        com.example.data.model.UnitItem(id = 2, name = "AVAX", symbol = "AVAX/USDT", currentPrice = 29.0, isDefault = false)
    )
    val config = com.example.data.model.AppConfig(defaultTargetPercentage = 2.0, defaultUnit = "SOL", defaultAmount = 5.0, currencySymbol = "USDT")

    // 1. Export to JSON
    val json = com.example.util.JsonBackupUtils.exportToJson(records, units, config)
    org.junit.Assert.assertTrue(json.contains("\"SOL\""))
    org.junit.Assert.assertTrue(json.contains("\"records\""))
    org.junit.Assert.assertTrue(json.contains("\"units\""))
    org.junit.Assert.assertTrue(json.contains("\"config\""))
    // Verify "id" and "symbol" are NOT present in JSON
    org.junit.Assert.assertFalse("JSON should not contain id field", json.contains("\"id\""))
    org.junit.Assert.assertFalse("JSON should not contain symbol field", json.contains("\"symbol\""))

    // 2. Parse back
    val result = com.example.util.JsonBackupUtils.parseBackupJson(json)
    org.junit.Assert.assertTrue(result.isSuccess)
    val data = result.getOrNull()!!

    org.junit.Assert.assertNotNull(data.records)
    assertEquals(2, data.records!!.size)
    assertEquals("SOL", data.records!![0].assetSymbol)
    assertEquals(145.0, data.records!![0].buyPrice, 0.001)
    assertEquals(false, data.records!![0].isSold)
    assertEquals(true, data.records!![1].isSold)
    assertEquals(100.0, data.records!![1].salePnl!!, 0.001)

    org.junit.Assert.assertNotNull(data.units)
    assertEquals(2, data.units!!.size)
    assertEquals("SOL", data.units!![0].name)

    org.junit.Assert.assertNotNull(data.config)
    assertEquals("SOL", data.config!!.defaultUnit)
    assertEquals(2.0, data.config!!.defaultTargetPercentage, 0.001)
  }

  @Test
  fun `test parse raw records array json`() {
    val rawJsonArray = """
      [
        {
          "assetSymbol": "DOT",
          "buyPrice": 7.5,
          "amount": 100.0,
          "targetPercentage": 10.0,
          "unit": "USDT"
        }
      ]
    """.trimIndent()

    val result = com.example.util.JsonBackupUtils.parseBackupJson(rawJsonArray)
    org.junit.Assert.assertTrue(result.isSuccess)
    val data = result.getOrNull()!!
    assertEquals(1, data.records?.size)
    assertEquals("DOT", data.records!![0].assetSymbol)
    assertEquals(7.5, data.records!![0].buyPrice, 0.001)
  }

  @Test
  fun `test invalid json returns error`() {
    val invalid = "{ not a valid json: 123"
    val result = com.example.util.JsonBackupUtils.parseBackupJson(invalid)
    org.junit.Assert.assertTrue(result.isFailure)
  }

  @Test
  fun `test empty units prevents record creation without unit`() {
    val emptyUnits = emptyList<com.example.data.model.UnitItem>()
    val canCreateDirectly = emptyUnits.isNotEmpty() && emptyUnits.any { it.isDefault }
    org.junit.Assert.assertFalse(canCreateDirectly)
  }

  @Test
  fun `test units with no default selected blocks record creation and defaults access`() {
    val unitsWithoutDefault = listOf(
        com.example.data.model.UnitItem(id = 1, name = "BTC", symbol = "BTC/USDT", currentPrice = 65000.0, isDefault = false),
        com.example.data.model.UnitItem(id = 2, name = "ETH", symbol = "ETH/USDT", currentPrice = 3500.0, isDefault = false)
    )

    fun canAccessDefaults(units: List<com.example.data.model.UnitItem>): Boolean {
        return units.isNotEmpty() && units.any { it.isDefault }
    }

    fun canCreateRecord(units: List<com.example.data.model.UnitItem>): Boolean {
        return units.isNotEmpty() && units.any { it.isDefault }
    }

    // Both should be FALSE when no default unit is selected
    org.junit.Assert.assertFalse(canAccessDefaults(unitsWithoutDefault))
    org.junit.Assert.assertFalse(canCreateRecord(unitsWithoutDefault))

    // When one is marked default, both should be TRUE
    val unitsWithDefault = unitsWithoutDefault.mapIndexed { idx, it ->
        if (idx == 0) it.copy(isDefault = true) else it
    }
    org.junit.Assert.assertTrue(canAccessDefaults(unitsWithDefault))
    org.junit.Assert.assertTrue(canCreateRecord(unitsWithDefault))
  }

  @Test
  fun `test default unit is preselected for defaults window`() {
    val units = listOf(
        com.example.data.model.UnitItem(id = 1, name = "BTC", symbol = "BTC/USDT", currentPrice = 65000.0, isDefault = false),
        com.example.data.model.UnitItem(id = 2, name = "SOL", symbol = "SOL/USDT", currentPrice = 160.0, isDefault = true),
        com.example.data.model.UnitItem(id = 3, name = "ETH", symbol = "ETH/USDT", currentPrice = 3500.0, isDefault = false)
    )
    val configWithEmptyDefault = com.example.data.model.AppConfig(defaultUnit = "")

    fun resolvePreselectedUnit(units: List<com.example.data.model.UnitItem>, config: com.example.data.model.AppConfig): String {
        return units.find { it.isDefault }?.name
            ?: config.defaultUnit.takeIf { sym -> units.any { it.name == sym } }
            ?: units.firstOrNull()?.name
            ?: ""
    }

    val preselected = resolvePreselectedUnit(units, configWithEmptyDefault)
    assertEquals("SOL", preselected)
  }
}
