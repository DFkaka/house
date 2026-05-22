
package com.example.house.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "settlements",
    foreignKeys = [
        ForeignKey(entity = RoomEntity::class, parentColumns = ["roomId"], childColumns = ["roomId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TenantEntity::class, parentColumns = ["tenantId"], childColumns = ["tenantId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("roomId"), Index("tenantId")]
)
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true)
    val settleId: Long = 0,
    val roomId: Long,
    val tenantId: Long,
    val startDate: String? = null,
    val endDate: String? = null,
    val waterStart: Double = 0.0,
    val waterEnd: Double = 0.0,
    val waterUsage: Double = 0.0,
    val waterUnitPrice: Double = 0.0,
    val waterFixedAmount: Double? = null,
    val waterFee: Double = 0.0,
    val electricStart: Double = 0.0,
    val electricEnd: Double = 0.0,
    val electricUsage: Double = 0.0,
    val electricUnitPrice: Double = 0.0,
    val electricFee: Double = 0.0,
    val totalFee: Double = 0.0,
    val settleDate: String? = null,
    val status: SettleStatus = SettleStatus.UNPAID
)

enum class SettleStatus { PAID, UNPAID }
