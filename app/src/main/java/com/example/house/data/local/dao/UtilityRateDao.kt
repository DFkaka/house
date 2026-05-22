
package com.example.house.data.local.dao

import androidx.room.*
import com.example.house.data.local.entity.RateType
import com.example.house.data.local.entity.UtilityRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UtilityRateDao {
    @Query("SELECT * FROM utility_rates ORDER BY effectiveDate DESC")
    fun getAllRates(): Flow<List<UtilityRateEntity>>

    @Query("SELECT * FROM utility_rates WHERE rateType = :type AND effectiveDate <= :date ORDER BY effectiveDate DESC LIMIT 1")
    suspend fun getEffectiveRate(type: RateType, date: String): UtilityRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: UtilityRateEntity): Long

    @Update
    suspend fun updateRate(rate: UtilityRateEntity)

    @Delete
    suspend fun deleteRate(rate: UtilityRateEntity)
}
