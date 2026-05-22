
package com.example.house.data.local.dao

import androidx.room.*
import com.example.house.data.local.entity.MeterReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeterReadingDao {
    @Query("SELECT * FROM meter_readings WHERE roomId = :roomId ORDER BY recordDate DESC")
    fun getReadingsByRoom(roomId: Long): Flow<List<MeterReadingEntity>>

    @Query("SELECT * FROM meter_readings WHERE roomId = :roomId ORDER BY recordDate DESC LIMIT 1")
    suspend fun getLatestReading(roomId: Long): MeterReadingEntity?

    @Query("SELECT * FROM meter_readings WHERE roomId = :roomId ORDER BY recordDate ASC")
    suspend fun getReadingsByRoomList(roomId: Long): List<MeterReadingEntity>

    @Query("SELECT * FROM meter_readings ORDER BY recordDate DESC")
    fun getAllReadings(): Flow<List<MeterReadingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: MeterReadingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadings(readings: List<MeterReadingEntity>): List<Long>

    @Delete
    suspend fun deleteReading(reading: MeterReadingEntity)
}
