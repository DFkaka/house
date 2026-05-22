
package com.example.house.ui.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.house.data.local.model.Tenant
import com.example.house.data.repository.RoomRepository
import com.example.house.data.repository.TenantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TenantListState(
    val tenants: List<Tenant> = emptyList(),
    val isLoading: Boolean = true
)

class TenantViewModel(
    private val tenantRepo: TenantRepository,
    private val roomRepo: RoomRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TenantListState())
    val state: StateFlow<TenantListState> = _state.asStateFlow()

    init { loadTenants() }

    fun loadTenants(query: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val tenants = if (query.isNotBlank()) tenantRepo.search(query) else tenantRepo.getAll()
            _state.value = TenantListState(tenants = tenants, isLoading = false)
        }
    }

    fun addTenant(tenant: Tenant, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tenantRepo.insert(tenant)
                roomRepo.updateTenantAndStatus(tenant.roomId, tenant.tenantId, "OCCUPIED")
                loadTenants()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun checkoutTenant(tenant: Tenant, checkoutDate: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tenantRepo.checkout(tenant.tenantId, checkoutDate)
                roomRepo.updateTenantAndStatus(tenant.roomId, null, "VACANT")
                loadTenants()
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
