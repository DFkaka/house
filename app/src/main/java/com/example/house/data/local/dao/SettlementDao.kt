
package com.example.house.data.local.dao

import androidx.room.*
import com.example.house.data.local.entity.SettlementEntity
import com.example.house.data.local.entity.SettleStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlements WHERE roomId = :roomId ORDER BY settleDate DESC")
    fun getSettlementsByRoom(roomId: Long): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE roomId = :roomId ORDER BY settleDate DESC LIMIT 1")
    suspend fun getLatestSettlement(roomId: Long): SettlementEntity?

    @Query("SELECT * FROM settlements ORDER BY settleDate DESC")
    fun getAllSettlements(): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE status = :status ORDER BY settleDate DESC")
    fun getSettlementsByStatus(status: SettleStatus): Flow<List<SettlementEntity>>

    @Query("SELECT SUM(totalFee) FROM settlements WHERE status = 'UNPAID'")
    fun getTotalUnpaid(): Flow<Double?>

    @Query("SELECT SUM(totalFee) FROM settlements WHERE status = 'UNPAID' AND roomId = :roomId")
    suspend fun getRoomUnpaidTotal(roomId: Long): Double?

    @Query("SELECT COUNT(*) FROM settlements WHERE status = 'UNPAID' AND roomId = :roomId")
    suspend fun getRoomUnpaidCount(roomId: Long): Int

    @Query("SELECT SUM(totalFee) FROM settlements WHERE settleDate BETWEEN :start AND :end")
    suspend fun getTotalBetween(start: String, end: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity): Long

    @Update
    suspend fun updateSettlement(settlement: SettlementEntity)

    @Delete
    suspend fun deleteSettlement(settlement: SettlementEntity)
}
