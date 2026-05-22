
package com.example.house.data.local.model

data class MeterReading(
    val recordId: Long = 0,
    val roomId: Long,
    val recordDate: String,
    val waterReading: Double,
    val electricReading: Double,
    val createdBy: String? = null
)
