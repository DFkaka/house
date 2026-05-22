
package com.example.house.data.repository

import android.content.ContentValues
import android.content.Context
import com.example.house.data.local.model.Room

class RoomRepository(context: Context) : BaseRepository(context) {

    fun getAll(): List<Room> {
        val list = mutableListOf<Room>()
        db.rawQuery("SELECT * FROM rooms ORDER BY roomCode ASC", null).use { c ->
            while (c.moveToNext()) list.add(mapRoom(c))
        }
        return list
    }

    fun getById(id: Long): Room? {
        db.rawQuery("SELECT * FROM rooms WHERE roomId=?", arrayOf(id.toString())).use { c ->
            if (c.moveToFirst()) return mapRoom(c)
        }
        return null
    }

    fun search(query: String): List<Room> {
        val list = mutableListOf<Room>()
        db.rawQuery("SELECT * FROM rooms WHERE roomCode LIKE ? OR roomName LIKE ? ORDER BY roomCode ASC",
            arrayOf("%$query%", "%$query%")).use { c ->
            while (c.moveToNext()) list.add(mapRoom(c))
        }
        return list
    }

    fun insert(room: Room): Long {
        val cv = ContentValues().apply {
            put("roomCode", room.roomCode)
            put("roomName", room.roomName)
            room.area?.let { put("area", it) }
            put("status", room.status)
        }
        return db.insert("rooms", null, cv)
    }

    fun update(room: Room) {
        val cv = ContentValues().apply {
            put("roomCode", room.roomCode)
            put("roomName", room.roomName)
            room.area?.let { put("area", it) }
            put("status", room.status)
            room.tenantId?.let { put("tenantId", it) } ?: putNull("tenantId")
        }
        db.update("rooms", cv, "roomId=?", arrayOf(room.roomId.toString()))
    }

    fun delete(roomId: Long) {
        db.delete("rooms", "roomId=?", arrayOf(roomId.toString()))
    }

    fun updateTenantAndStatus(roomId: Long, tenantId: Long?, status: String) {
        val cv = ContentValues().apply {
            put("status", status)
            tenantId?.let { put("tenantId", it) } ?: putNull("tenantId")
        }
        db.update("rooms", cv, "roomId=?", arrayOf(roomId.toString()))
    }

    fun updateLastReadings(roomId: Long, water: Double, electric: Double, date: String,
                            wFee: Double, eFee: Double, total: Double) {
        val cv = ContentValues().apply {
            put("waterMeterLast", water)
            put("electricMeterLast", electric)
            put("lastSettleDate", date)
            put("lastWaterFee", wFee)
            put("lastElectricFee", eFee)
            put("lastTotalFee", total)
        }
        db.update("rooms", cv, "roomId=?", arrayOf(roomId.toString()))
    }

    private fun mapRoom(c: android.database.Cursor): Room = Room(
        roomId = c.getLong("roomId"),
        roomCode = c.getString("roomCode") ?: "",
        roomName = c.getString("roomName") ?: "",
        area = c.getDoubleOrNull("area"),
        status = c.getString("status") ?: "VACANT",
        tenantId = c.getLongOrNull("tenantId"),
        waterMeterLast = c.getDouble("waterMeterLast"),
        electricMeterLast = c.getDouble("electricMeterLast"),
        lastSettleDate = c.getString("lastSettleDate"),
        lastWaterFee = c.getDouble("lastWaterFee"),
        lastElectricFee = c.getDouble("lastElectricFee"),
        lastTotalFee = c.getDouble("lastTotalFee")
    )
}
