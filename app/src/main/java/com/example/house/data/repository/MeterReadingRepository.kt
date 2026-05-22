
package com.example.house.data.repository

import com.example.house.data.local.dao.MeterReadingDao
import com.example.house.data.local.entity.MeterReadingEntity
import kotlinx.coroutines.flow.Flow

class MeterReadingRepository(private val meterReadingDao: MeterReadingDao) {
    fun getReadingsByRoom(roomId: Long): Flow<List<MeterReadingEntity>> =
        meterReadingDao.getReadingsByRoom(roomId)

    suspend fun getLatestReading(roomId: Long): MeterReadingEntity? =
        meterReadingDao.getLatestReading(roomId)

    suspend fun getReadingsByRoomList(roomId: Long): List<MeterReadingEntity> =
        meterReadingDao.getReadingsByRoomList(roomId)

    fun getAllReadings(): Flow<List<MeterReadingEntity>> = meterReadingDao.getAllReadings()

    suspend fun insertReading(reading: MeterReadingEntity): Long =
        meterReadingDao.insertReading(reading)

    suspend fun insertReadings(readings: List<MeterReadingEntity>): List<Long> =
        meterReadingDao.insertReadings(readings)

    suspend fun deleteReading(reading: MeterReadingEntity) =
        meterReadingDao.deleteReading(reading)
}
