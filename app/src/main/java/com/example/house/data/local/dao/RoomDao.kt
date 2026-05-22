
package com.example.house.data.local.dao

import androidx.room.*
import com.example.house.data.local.entity.RoomEntity
import com.example.house.data.local.entity.RoomStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms ORDER BY roomCode ASC")
    fun getAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE roomId = :id")
    suspend fun getRoomById(id: Long): RoomEntity?

    @Query("SELECT * FROM rooms WHERE status = :status ORDER BY roomCode ASC")
    fun getRoomsByStatus(status: RoomStatus): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE roomCode LIKE '%' || :query || '%' OR roomName LIKE '%' || :query || '%'")
    fun searchRooms(query: String): Flow<List<RoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity): Long

    @Update
    suspend fun updateRoom(room: RoomEntity)

    @Delete
    suspend fun deleteRoom(room: RoomEntity)

    @Query("UPDATE rooms SET tenantId = :tenantId, status = :status WHERE roomId = :roomId")
    suspend fun updateTenantAndStatus(roomId: Long, tenantId: Long?, status: RoomStatus)

    @Query("UPDATE rooms SET waterMeterLast = :water, electricMeterLast = :electric, lastSettleDate = :date, lastWaterFee = :wFee, lastElectricFee = :eFee, lastTotalFee = :total WHERE roomId = :roomId")
    suspend fun updateLastReadings(roomId: Long, water: Double, electric: Double, date: String, wFee: Double, eFee: Double, total: Double)
}
