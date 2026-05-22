
package com.example.house.data.repository

import com.example.house.data.local.dao.RoomDao
import com.example.house.data.local.entity.RoomEntity
import com.example.house.data.local.entity.RoomStatus
import kotlinx.coroutines.flow.Flow

class RoomRepository(private val roomDao: RoomDao) {
    val allRooms: Flow<List<RoomEntity>> = roomDao.getAllRooms()

    fun getRoomsByStatus(status: RoomStatus): Flow<List<RoomEntity>> = roomDao.getRoomsByStatus(status)

    fun searchRooms(query: String): Flow<List<RoomEntity>> = roomDao.searchRooms(query)

    suspend fun getRoomById(id: Long): RoomEntity? = roomDao.getRoomById(id)

    suspend fun insertRoom(room: RoomEntity): Long = roomDao.insertRoom(room)

    suspend fun updateRoom(room: RoomEntity) = roomDao.updateRoom(room)

    suspend fun deleteRoom(room: RoomEntity) = roomDao.deleteRoom(room)

    suspend fun updateTenantAndStatus(roomId: Long, tenantId: Long?, status: RoomStatus) =
        roomDao.updateTenantAndStatus(roomId, tenantId, status)

    suspend fun updateLastReadings(
        roomId: Long, water: Double, electric: Double, date: String,
        wFee: Double, eFee: Double, total: Double
    ) = roomDao.updateLastReadings(roomId, water, electric, date, wFee, eFee, total)
}
