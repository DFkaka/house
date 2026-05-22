
package com.example.house.di

import android.content.Context
import com.example.house.data.local.db.AppDatabase
import com.example.house.data.repository.MeterReadingRepository
import com.example.house.data.repository.RoomRepository
import com.example.house.data.repository.SettlementRepository
import com.example.house.data.repository.TenantRepository

class AppContainer(context: Context) {
    private val db = AppDatabase.getInstance(context)

    val roomRepository = RoomRepository(db.roomDao())
    val tenantRepository = TenantRepository(db.tenantDao())
    val meterReadingRepository = MeterReadingRepository(db.meterReadingDao())
    val settlementRepository = SettlementRepository(
        db.settlementDao(), db.utilityRateDao()
    )
}
