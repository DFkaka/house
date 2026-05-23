package com.example.house.data.local.model

data class Tenant(
    val tenantId: Long = 0,
    val name: String,
    val phone: String,
    val idCard: String? = null,
    val roomId: Long,
    val checkInDate: String,
    val checkOutDate: String? = null,
    val initialWaterReading: Double = 0.0,
    val initialElectricReading: Double = 0.0,
    val notes: String? = null
)
