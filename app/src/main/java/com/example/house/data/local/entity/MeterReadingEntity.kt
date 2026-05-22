
package com.example.house.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meter_readings",
    foreignKeys = [ForeignKey(
        entity = RoomEntity::class,
        parentColumns = ["roomId"],
        childColumns = ["roomId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("roomId")]
)
data class MeterReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val recordId: Long = 0,
    val roomId: Long,
    val recordDate: String,        // "yyyy-MM-dd"
    val waterReading: Double,
    val electricReading: Double,
    val createdBy: String? = null
)
