
package com.example.house.data.local.model

data class Room(
    val roomId: Long = 0,
    val roomCode: String,
    val roomName: String = "",
    val area: Double? = null,
    val status: String = "VACANT",      // VACANT / OCCUPIED / MAINTENANCE
    val tenantId: Long? = null,
    val waterMeterLast: Double = 0.0,
    val electricMeterLast: Double = 0.0,
    val lastSettleDate: String? = null,
    val lastWaterFee: Double = 0.0,
    val lastElectricFee: Double = 0.0,
    val lastTotalFee: Double = 0.0
)
