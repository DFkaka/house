
package com.example.house.data.local.model

data class UtilityRate(
    val rateId: Long = 0,
    val rateType: String,              // ELECTRIC_UNIT / WATER_UNIT / WATER_FIXED
    val unitPrice: Double,
    val unit: String,
    val effectiveDate: String,
    val remark: String? = null
)
