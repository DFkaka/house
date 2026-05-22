
package com.example.house.ui.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.house.data.local.entity.RoomStatus
import com.example.house.data.local.entity.TenantEntity
import com.example.house.data.repository.RoomRepository
import com.example.house.data.repository.TenantRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TenantListState(
    val tenants: List<TenantEntity> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = ""
)

class TenantViewModel(
    private val tenantRepo: TenantRepository,
    private val roomRepo: RoomRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TenantListState())
    val state: StateFlow<TenantListState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _searchQuery
                .flatMapLatest { query ->
                    if (query.isNotBlank()) tenantRepo.searchTenants(query)
                    else tenantRepo.allTenants
                }
                .collect { tenants ->
                    _state.value = TenantListState(tenants = tenants, isLoading = false)
                }
        }
    }

    fun onSearch(query: String) {
        _searchQuery.value = query
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun addTenant(tenant: TenantEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                tenantRepo.insertTenant(tenant)
                // Update room status to OCCUPIED
                roomRepo.updateTenantAndStatus(tenant.roomId, tenant.tenantId, RoomStatus.OCCUPIED)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun checkoutTenant(tenant: TenantEntity, checkoutDate: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                tenantRepo.checkoutTenant(tenant.tenantId, checkoutDate)
                roomRepo.updateTenantAndStatus(tenant.roomId, null, RoomStatus.VACANT)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    class Factory(
        private val tenantRepo: TenantRepository,
        private val roomRepo: RoomRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TenantViewModel(tenantRepo, roomRepo) as T
        }
    }
}
