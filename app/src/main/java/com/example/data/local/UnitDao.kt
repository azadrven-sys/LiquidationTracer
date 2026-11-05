package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UnitItem
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitDao {
    @Query("SELECT * FROM units ORDER BY isDefault DESC, name ASC")
    fun getAllUnits(): Flow<List<UnitItem>>

    @Query("SELECT * FROM units")
    suspend fun getAllUnitsSync(): List<UnitItem>

    @Query("SELECT COUNT(*) FROM units")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: UnitItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(units: List<UnitItem>)

    @Update
    suspend fun updateUnit(unit: UnitItem)

    @Delete
    suspend fun deleteUnit(unit: UnitItem)

    @Query("UPDATE units SET isDefault = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setDefaultUnit(id: Long)

    @Query("UPDATE units SET currentPrice = :price, lastUpdated = :timestamp WHERE id = :id")
    suspend fun updatePrice(id: Long, price: Double, timestamp: Long)

    @Query("UPDATE units SET currentPrice = :price, lastUpdated = :timestamp WHERE UPPER(name) = UPPER(:name)")
    suspend fun updatePriceByName(name: String, price: Double, timestamp: Long)

    @Query("DELETE FROM units")
    suspend fun deleteAllUnits()

    @Query("DELETE FROM units WHERE isSystem = 1")
    suspend fun deleteSystemUnits()
}
