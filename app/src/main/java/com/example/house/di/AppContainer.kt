
package com.example.house.di

import android.content.Context
import com.example.house.data.repository.MeterReadingRepository
import com.example.house.data.repository.RoomRepository
import com.example.house.data.repository.SettlementRepository
import com.example.house.data.repository.TenantRepository

class AppContainer(context: Context) {
    val roomRepository = RoomRepository(context)
    val tenantRepository = TenantRepository(context)
    val meterReadingRepository = MeterReadingRepository(context)
    val settlementRepository = SettlementRepository(context)
}
