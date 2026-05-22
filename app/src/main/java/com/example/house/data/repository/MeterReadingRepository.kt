
package com.example.house.data.repository

import android.content.ContentValues
import android.content.Context
import com.example.house.data.local.model.MeterReading

class MeterReadingRepository(context: Context) : BaseRepository(context) {

    fun getByRoom(roomId: Long): List<MeterReading> {
        val list = mutableListOf<MeterReading>()
        db.rawQuery("SELECT * FROM meter_readings WHERE roomId=? ORDER BY recordDate DESC",
            arrayOf(roomId.toString())).use { c ->
            while (c.moveToNext()) list.add(mapReading(c))
        }
        return list
    }

    fun getLatest(roomId: Long): MeterReading? {
        db.rawQuery("SELECT * FROM meter_readings WHERE roomId=? ORDER BY recordDate DESC LIMIT 1",
            arrayOf(roomId.toString())).use { c ->
            if (c.moveToFirst()) return mapReading(c)
        }
        return null
    }

    fun getAll(): List<MeterReading> {
        val list = mutableListOf<MeterReading>()
        db.rawQuery("SELECT * FROM meter_readings ORDER BY recordDate DESC", null).use { c ->
            while (c.moveToNext()) list.add(mapReading(c))
        }
        return list
    }

    fun insert(reading: MeterReading): Long {
        val cv = ContentValues().apply {
            put("roomId", reading.roomId)
            put("recordDate", reading.recordDate)
            put("waterReading", reading.waterReading)
            put("electricReading", reading.electricReading)
            reading.createdBy?.let { put("createdBy", it) }
        }
        return db.insert("meter_readings", null, cv)
    }

    private fun mapReading(c: android.database.Cursor): MeterReading = MeterReading(
        recordId = c.getLong("recordId"),
        roomId = c.getLong("roomId"),
        recordDate = c.getString("recordDate") ?: "",
        waterReading = c.getDouble("waterReading"),
        electricReading = c.getDouble("electricReading"),
        createdBy = c.getString("createdBy")
    )
}
