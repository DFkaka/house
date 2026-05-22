
package com.example.house.data.repository

import com.example.house.data.local.dao.TenantDao
import com.example.house.data.local.entity.TenantEntity
import kotlinx.coroutines.flow.Flow

class TenantRepository(private val tenantDao: TenantDao) {
    val allTenants: Flow<List<TenantEntity>> = tenantDao.getAllTenants()

    suspend fun getTenantById(id: Long): TenantEntity? = tenantDao.getTenantById(id)

    suspend fun getActiveTenantByRoom(roomId: Long): TenantEntity? = tenantDao.getActiveTenantByRoom(roomId)

    fun getTenantsByRoom(roomId: Long): Flow<List<TenantEntity>> = tenantDao.getTenantsByRoom(roomId)

    fun searchTenants(query: String): Flow<List<TenantEntity>> = tenantDao.searchTenants(query)

    suspend fun insertTenant(tenant: TenantEntity): Long = tenantDao.insertTenant(tenant)

    suspend fun updateTenant(tenant: TenantEntity) = tenantDao.updateTenant(tenant)

    suspend fun deleteTenant(tenant: TenantEntity) = tenantDao.deleteTenant(tenant)

    suspend fun checkoutTenant(tenantId: Long, date: String) = tenantDao.checkoutTenant(tenantId, date)
}
