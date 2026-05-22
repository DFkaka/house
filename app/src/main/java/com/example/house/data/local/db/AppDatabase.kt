
package com.example.house.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.house.data.local.dao.*
import com.example.house.data.local.entity.*

@Database(
    entities = [
        RoomEntity::class,
        TenantEntity::class,
        MeterReadingEntity::class,
        SettlementEntity::class,
        UtilityRateEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roomDao(): RoomDao
    abstract fun tenantDao(): TenantDao
    abstract fun meterReadingDao(): MeterReadingDao
    abstract fun settlementDao(): SettlementDao
    abstract fun utilityRateDao(): UtilityRateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "house_utility.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
