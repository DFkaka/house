
package com.example.house.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "utility_rates")
data class UtilityRateEntity(
    @PrimaryKey(autoGenerate = true)
    val rateId: Long = 0,
    val rateType: RateType,
    val unitPrice: Double,
    val unit: String,              // 元/度, 元/吨, 元/月
    val effectiveDate: String,     // "yyyy-MM-dd"
    val remark: String? = null
)

enum class RateType { ELECTRIC_UNIT, WATER_UNIT, WATER_FIXED }
