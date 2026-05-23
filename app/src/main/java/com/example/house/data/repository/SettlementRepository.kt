
package com.example.house.data.repository

import android.content.ContentValues
import android.content.Context
import com.example.house.data.local.model.Settlement
import com.example.house.data.local.model.UtilityRate

class SettlementRepository(context: Context) : BaseRepository(context) {

    fun getByRoom(roomId: Long): List<Settlement> {
        val list = mutableListOf<Settlement>()
        db.rawQuery("SELECT * FROM settlements WHERE roomId=? ORDER BY settleDate DESC",
            arrayOf(roomId.toString())).use { c ->
            while (c.moveToNext()) list.add(mapSettlement(c))
        }
        return list
    }

    fun query(roomId: Long?, startDate: String?, endDate: String?): List<Settlement> {
        val list = mutableListOf<Settlement>()
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()
        roomId?.let { where.add("roomId=?"); args.add(it.toString()) }
        startDate?.let { where.add("settleDate>=?"); args.add(it) }
        endDate?.let { where.add("settleDate<=?"); args.add(it) }
        val whereClause = if (where.isNotEmpty()) "WHERE " + where.joinToString(" AND ") else ""
        db.rawQuery("SELECT * FROM settlements $whereClause ORDER BY settleDate DESC",
            args.toTypedArray()).use { c ->
            while (c.moveToNext()) list.add(mapSettlement(c))
        }
        return list
    }

    fun getLatest(roomId: Long): Settlement? {
        db.rawQuery("SELECT * FROM settlements WHERE roomId=? ORDER BY settleDate DESC LIMIT 1",
            arrayOf(roomId.toString())).use { c ->
            if (c.moveToFirst()) return mapSettlement(c)
        }
        return null
    }

    fun getAll(): List<Settlement> {
        val list = mutableListOf<Settlement>()
        db.rawQuery("SELECT * FROM settlements ORDER BY settleDate DESC", null).use { c ->
            while (c.moveToNext()) list.add(mapSettlement(c))
        }
        return list
    }

    fun getUnpaid(): List<Settlement> {
        val list = mutableListOf<Settlement>()
        db.rawQuery("SELECT * FROM settlements WHERE status='UNPAID' ORDER BY settleDate DESC", null).use { c ->
            while (c.moveToNext()) list.add(mapSettlement(c))
        }
        return list
    }

    fun getTotalUnpaid(): Double {
        db.rawQuery("SELECT SUM(totalFee) FROM settlements WHERE status='UNPAID'", null).use { c ->
            if (c.moveToFirst()) return c.getDouble(0)
        }
        return 0.0
    }

    fun getRoomUnpaidTotal(roomId: Long): Double {
        db.rawQuery("SELECT SUM(totalFee) FROM settlements WHERE status='UNPAID' AND roomId=?",
            arrayOf(roomId.toString())).use { c ->
            if (c.moveToFirst()) return c.getDouble(0)
        }
        return 0.0
    }

    fun getRoomUnpaidCount(roomId: Long): Int {
        db.rawQuery("SELECT COUNT(*) FROM settlements WHERE status='UNPAID' AND roomId=?",
            arrayOf(roomId.toString())).use { c ->
            if (c.moveToFirst()) return c.getInt(0)
        }
        return 0
    }

    fun getTotalBetween(start: String, end: String): Double {
        db.rawQuery("SELECT SUM(totalFee) FROM settlements WHERE settleDate BETWEEN ? AND ?",
            arrayOf(start, end)).use { c ->
            if (c.moveToFirst()) return c.getDouble(0)
        }
        return 0.0
    }

    fun insert(settlement: Settlement): Long {
        val cv = ContentValues().apply {
            put("roomId", settlement.roomId)
            put("tenantId", settlement.tenantId)
            put("startDate", settlement.startDate)
            put("endDate", settlement.endDate)
            put("waterStart", settlement.waterStart)
            put("waterEnd", settlement.waterEnd)
            put("waterUsage", settlement.waterUsage)
            put("waterUnitPrice", settlement.waterUnitPrice)
            putIfNotNull("waterFixedAmount", settlement.waterFixedAmount)
            put("waterFee", settlement.waterFee)
            put("electricStart", settlement.electricStart)
            put("electricEnd", settlement.electricEnd)
            put("electricUsage", settlement.electricUsage)
            put("electricUnitPrice", settlement.electricUnitPrice)
            put("electricFee", settlement.electricFee)
            put("totalFee", settlement.totalFee)
            put("settleDate", settlement.settleDate)
            put("status", settlement.status)
        }
        return db.insert("settlements", null, cv)
    }

    // Utility rates
    fun getEffectiveRate(type: String, date: String): UtilityRate? {
        db.rawQuery("SELECT * FROM utility_rates WHERE rateType=? AND effectiveDate<=? ORDER BY effectiveDate DESC LIMIT 1",
            arrayOf(type, date)).use { c ->
            if (c.moveToFirst()) return mapRate(c)
        }
        return null
    }

    fun insertRate(rate: UtilityRate): Long {
        val cv = ContentValues().apply {
            put("rateType", rate.rateType)
            put("unitPrice", rate.unitPrice)
            put("unit", rate.unit)
            put("effectiveDate", rate.effectiveDate)
            rate.remark?.let { put("remark", it) }
        }
        return db.insert("utility_rates", null, cv)
    }

    private fun mapSettlement(c: android.database.Cursor): Settlement = Settlement(
        settleId = c.getLong("settleId"),
        roomId = c.getLong("roomId"),
        tenantId = c.getLong("tenantId"),
        startDate = c.getString("startDate"),
        endDate = c.getString("endDate"),
        waterStart = c.getDouble("waterStart"),
        waterEnd = c.getDouble("waterEnd"),
        waterUsage = c.getDouble("waterUsage"),
        waterUnitPrice = c.getDouble("waterUnitPrice"),
        waterFixedAmount = c.getDoubleOrNull("waterFixedAmount"),
        waterFee = c.getDouble("waterFee"),
        electricStart = c.getDouble("electricStart"),
        electricEnd = c.getDouble("electricEnd"),
        electricUsage = c.getDouble("electricUsage"),
        electricUnitPrice = c.getDouble("electricUnitPrice"),
        electricFee = c.getDouble("electricFee"),
        totalFee = c.getDouble("totalFee"),
        settleDate = c.getString("settleDate"),
        status = c.getString("status") ?: "UNPAID"
    )

    private fun mapRate(c: android.database.Cursor): UtilityRate = UtilityRate(
        rateId = c.getLong("rateId"),
        rateType = c.getString("rateType") ?: "",
        unitPrice = c.getDouble("unitPrice"),
        unit = c.getString("unit") ?: "",
        effectiveDate = c.getString("effectiveDate") ?: "",
        remark = c.getString("remark")
    )
}
