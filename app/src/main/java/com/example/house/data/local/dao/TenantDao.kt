
package com.example.house.data.local.dao

import androidx.room.*
import com.example.house.data.local.entity.TenantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TenantDao {
    @Query("SELECT * FROM tenants ORDER BY checkInDate DESC")
    fun getAllTenants(): Flow<List<TenantEntity>>

    @Query("SELECT * FROM tenants WHERE tenantId = :id")
    suspend fun getTenantById(id: Long): TenantEntity?

    @Query("SELECT * FROM tenants WHERE roomId = :roomId AND checkOutDate IS NULL LIMIT 1")
    suspend fun getActiveTenantByRoom(roomId: Long): TenantEntity?

    @Query("SELECT * FROM tenants WHERE roomId = :roomId ORDER BY checkInDate DESC")
    fun getTenantsByRoom(roomId: Long): Flow<List<TenantEntity>>

    @Query("SELECT * FROM tenants WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'")
    fun searchTenants(query: String): Flow<List<TenantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenant(tenant: TenantEntity): Long

    @Update
    suspend fun updateTenant(tenant: TenantEntity)

    @Delete
    suspend fun deleteTenant(tenant: TenantEntity)

    @Query("UPDATE tenants SET checkOutDate = :date WHERE tenantId = :tenantId")
    suspend fun checkoutTenant(tenantId: Long, date: String)
}
