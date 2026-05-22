
package com.example.house.data.repository

import android.content.ContentValues
import android.content.Context
import com.example.house.data.local.model.Tenant

class TenantRepository(context: Context) : BaseRepository(context) {

    fun getAll(): List<Tenant> {
        val list = mutableListOf<Tenant>()
        db.rawQuery("SELECT * FROM tenants ORDER BY checkInDate DESC", null).use { c ->
            while (c.moveToNext()) list.add(mapTenant(c))
        }
        return list
    }

    fun getById(id: Long): Tenant? {
        db.rawQuery("SELECT * FROM tenants WHERE tenantId=?", arrayOf(id.toString())).use { c ->
            if (c.moveToFirst()) return mapTenant(c)
        }
        return null
    }

    fun getActiveByRoom(roomId: Long): Tenant? {
        db.rawQuery("SELECT * FROM tenants WHERE roomId=? AND checkOutDate IS NULL LIMIT 1",
            arrayOf(roomId.toString())).use { c ->
            if (c.moveToFirst()) return mapTenant(c)
        }
        return null
    }

    fun getByRoom(roomId: Long): List<Tenant> {
        val list = mutableListOf<Tenant>()
        db.rawQuery("SELECT * FROM tenants WHERE roomId=? ORDER BY checkInDate DESC",
            arrayOf(roomId.toString())).use { c ->
            while (c.moveToNext()) list.add(mapTenant(c))
        }
        return list
    }

    fun search(query: String): List<Tenant> {
        val list = mutableListOf<Tenant>()
        db.rawQuery("SELECT * FROM tenants WHERE name LIKE ? OR phone LIKE ? ORDER BY checkInDate DESC",
            arrayOf("%$query%", "%$query%")).use { c ->
            while (c.moveToNext()) list.add(mapTenant(c))
        }
        return list
    }

    fun insert(tenant: Tenant): Long {
        val cv = ContentValues().apply {
            put("name", tenant.name)
            put("phone", tenant.phone)
            tenant.idCard?.let { put("idCard", it) }
            put("roomId", tenant.roomId)
            put("checkInDate", tenant.checkInDate)
        }
        return db.insert("tenants", null, cv)
    }

    fun checkout(tenantId: Long, date: String) {
        val cv = ContentValues().apply { put("checkOutDate", date) }
        db.update("tenants", cv, "tenantId=?", arrayOf(tenantId.toString()))
    }

    private fun mapTenant(c: android.database.Cursor): Tenant = Tenant(
        tenantId = c.getLong("tenantId"),
        name = c.getString("name") ?: "",
        phone = c.getString("phone") ?: "",
        idCard = c.getString("idCard"),
        roomId = c.getLong("roomId"),
        checkInDate = c.getString("checkInDate") ?: "",
        checkOutDate = c.getString("checkOutDate"),
        notes = c.getString("notes")
    )
}
