package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LiquidationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface LiquidationDao {
    @Query("SELECT * FROM liquidation_records ORDER BY isSold ASC, COALESCE(updatedAt, createdAt) DESC")
    fun getAllRecords(): Flow<List<LiquidationRecord>>

    @Query("SELECT * FROM liquidation_records WHERE isSold = 0 ORDER BY COALESCE(updatedAt, createdAt) DESC")
    fun getActiveRecords(): Flow<List<LiquidationRecord>>

    @Query("SELECT * FROM liquidation_records WHERE isSold = 1 ORDER BY COALESCE(updatedAt, createdAt) DESC")
    fun getSoldRecords(): Flow<List<LiquidationRecord>>

    @Query("SELECT * FROM liquidation_records WHERE id = :id")
    suspend fun getRecordById(id: Long): LiquidationRecord?

    @Query("SELECT COUNT(*) FROM liquidation_records")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: LiquidationRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<LiquidationRecord>)

    @Update
    suspend fun updateRecord(record: LiquidationRecord)

    @Delete
    suspend fun deleteRecord(record: LiquidationRecord)

    @Query("UPDATE liquidation_records SET isSold = 1, salePrice = :salePrice, salePnl = :pnl, saleDate = :saleDate WHERE id = :id")
    suspend fun markAsSold(id: Long, salePrice: Double, pnl: Double, saleDate: Long)

    @Query("UPDATE liquidation_records SET isSold = 0, salePrice = NULL, salePnl = NULL, saleDate = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun reopenRecord(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM liquidation_records")
    suspend fun deleteAllRecords()

    @Query("DELETE FROM liquidation_records WHERE notes IN (:notes)")
    suspend fun deleteRecordsByNotes(notes: List<String>)

    @Query("SELECT * FROM liquidation_records")
    suspend fun getAllRecordsSync(): List<LiquidationRecord>
}
