
package com.example.house.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tenants",
    foreignKeys = [ForeignKey(
        entity = RoomEntity::class,
        parentColumns = ["roomId"],
        childColumns = ["roomId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("roomId")]
)
data class TenantEntity(
    @PrimaryKey(autoGenerate = true)
    val tenantId: Long = 0,
    val name: String,
    val phone: String,
    val idCard: String? = null,
    val roomId: Long,
    val checkInDate: String,       // "yyyy-MM-dd"
    val checkOutDate: String? = null,
    val notes: String? = null
)
