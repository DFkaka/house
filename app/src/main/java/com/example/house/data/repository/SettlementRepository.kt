
package com.example.house.data.repository

import com.example.house.data.local.dao.SettlementDao
import com.example.house.data.local.dao.UtilityRateDao
import com.example.house.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class SettlementRepository(
    private val settlementDao: SettlementDao,
    private val utilityRateDao: UtilityRateDao
) {
    fun getSettlementsByRoom(roomId: Long): Flow<List<SettlementEntity>> =
        settlementDao.getSettlementsByRoom(roomId)

    suspend fun getLatestSettlement(roomId: Long): SettlementEntity? =
        settlementDao.getLatestSettlement(roomId)

    fun getAllSettlements(): Flow<List<SettlementEntity>> = settlementDao.getAllSettlements()

    fun getSettlementsByStatus(status: SettleStatus): Flow<List<SettlementEntity>> =
        settlementDao.getSettlementsByStatus(status)

    fun getTotalUnpaid(): Flow<Double?> = settlementDao.getTotalUnpaid()

    suspend fun getRoomUnpaidTotal(roomId: Long): Double? = settlementDao.getRoomUnpaidTotal(roomId)

    suspend fun getRoomUnpaidCount(roomId: Long): Int = settlementDao.getRoomUnpaidCount(roomId)

    suspend fun getTotalBetween(start: String, end: String): Double? =
        settlementDao.getTotalBetween(start, end)

    suspend fun insertSettlement(settlement: SettlementEntity): Long =
        settlementDao.insertSettlement(settlement)

    suspend fun updateSettlement(settlement: SettlementEntity) =
        settlementDao.updateSettlement(settlement)

    suspend fun deleteSettlement(settlement: SettlementEntity) =
        settlementDao.deleteSettlement(settlement)

    // Business: get effective rates
    suspend fun getEffectiveRate(type: RateType, date: String): UtilityRateEntity? =
        utilityRateDao.getEffectiveRate(type, date)

    suspend fun insertRate(rate: UtilityRateEntity): Long = utilityRateDao.insertRate(rate)

    suspend fun updateRate(rate: UtilityRateEntity) = utilityRateDao.updateRate(rate)

    suspend fun deleteRate(rate: UtilityRateEntity) = utilityRateDao.deleteRate(rate)
}
