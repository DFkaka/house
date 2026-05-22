
package com.example.house.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey(autoGenerate = true)
    val roomId: Long = 0,
    val roomCode: String,          // 房间编号 (101, A01)
    val roomName: String = "",     // 房间名称
    val area: Double? = null,      // 面积
    val status: RoomStatus = RoomStatus.VACANT,
    val tenantId: Long? = null,    // 当前租客ID
    val waterMeterLast: Double = 0.0,
    val electricMeterLast: Double = 0.0,
    val lastSettleDate: String? = null,  // "yyyy-MM-dd"
    val lastWaterFee: Double = 0.0,
    val lastElectricFee: Double = 0.0,
    val lastTotalFee: Double = 0.0
)

enum class RoomStatus { VACANT, OCCUPIED, MAINTENANCE }
