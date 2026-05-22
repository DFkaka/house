
package com.example.house.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "house.db"
        private const val DB_VERSION = 1

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE rooms (
                roomId INTEGER PRIMARY KEY AUTOINCREMENT,
                roomCode TEXT NOT NULL UNIQUE,
                roomName TEXT DEFAULT '',
                area REAL,
                status TEXT DEFAULT 'VACANT',
                tenantId INTEGER,
                waterMeterLast REAL DEFAULT 0,
                electricMeterLast REAL DEFAULT 0,
                lastSettleDate TEXT,
                lastWaterFee REAL DEFAULT 0,
                lastElectricFee REAL DEFAULT 0,
                lastTotalFee REAL DEFAULT 0
            )
        """)

        db.execSQL("""
            CREATE TABLE tenants (
                tenantId INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT NOT NULL,
                idCard TEXT,
                roomId INTEGER NOT NULL,
                checkInDate TEXT NOT NULL,
                checkOutDate TEXT,
                notes TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE meter_readings (
                recordId INTEGER PRIMARY KEY AUTOINCREMENT,
                roomId INTEGER NOT NULL,
                recordDate TEXT NOT NULL,
                waterReading REAL NOT NULL,
                electricReading REAL NOT NULL,
                createdBy TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE settlements (
                settleId INTEGER PRIMARY KEY AUTOINCREMENT,
                roomId INTEGER NOT NULL,
                tenantId INTEGER NOT NULL,
                startDate TEXT,
                endDate TEXT,
                waterStart REAL DEFAULT 0,
                waterEnd REAL DEFAULT 0,
                waterUsage REAL DEFAULT 0,
                waterUnitPrice REAL DEFAULT 0,
                waterFixedAmount REAL,
                waterFee REAL DEFAULT 0,
                electricStart REAL DEFAULT 0,
                electricEnd REAL DEFAULT 0,
                electricUsage REAL DEFAULT 0,
                electricUnitPrice REAL DEFAULT 0,
                electricFee REAL DEFAULT 0,
                totalFee REAL DEFAULT 0,
                settleDate TEXT,
                status TEXT DEFAULT 'UNPAID'
            )
        """)

        db.execSQL("""
            CREATE TABLE utility_rates (
                rateId INTEGER PRIMARY KEY AUTOINCREMENT,
                rateType TEXT NOT NULL,
                unitPrice REAL NOT NULL,
                unit TEXT NOT NULL,
                effectiveDate TEXT NOT NULL,
                remark TEXT
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle future migrations
    }
}
