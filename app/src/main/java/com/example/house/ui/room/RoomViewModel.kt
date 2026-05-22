
package com.example.house.ui.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.house.data.local.entity.RoomEntity
import com.example.house.data.local.entity.RoomStatus
import com.example.house.data.repository.RoomRepository
import com.example.house.data.repository.TenantRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RoomListState(
    val rooms: List<RoomWithTenant> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val statusFilter: RoomStatus? = null
)

data class RoomWithTenant(
    val room: RoomEntity,
    val tenantName: String = ""
)

class RoomViewModel(
    private val roomRepo: RoomRepository,
    private val tenantRepo: TenantRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RoomListState())
    val state: StateFlow<RoomListState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _statusFilter = MutableStateFlow<RoomStatus?>(null)

    init {
        loadRooms()
    }

    private fun loadRooms() {
        viewModelScope.launch {
            combine(
                _searchQuery, _statusFilter
            ) { query, filter -> Pair(query, filter) }
                .flatMapLatest { (query, filter) ->
                    val flow = when {
                        query.isNotBlank() -> roomRepo.searchRooms(query)
                        filter != null -> roomRepo.getRoomsByStatus(filter)
                        else -> roomRepo.allRooms
                    }
                    flow.map { rooms ->
                        rooms.map { room ->
                            val tenant = if (room.tenantId != null)
                                tenantRepo.getTenantById(room.tenantId)
                            else null
                            RoomWithTenant(room, tenant?.name ?: "")
                        }
                    }
                }
                .collect { items ->
                    _state.value = _state.value.copy(rooms = items, isLoading = false)
                }
        }
    }

    fun onSearch(query: String) {
        _searchQuery.value = query
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun onFilterStatus(status: RoomStatus?) {
        _statusFilter.value = status
        _state.value = _state.value.copy(statusFilter = status)
    }

    fun addRoom(room: RoomEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                roomRepo.insertRoom(room)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun updateRoom(room: RoomEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                roomRepo.updateRoom(room)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun deleteRoom(room: RoomEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                roomRepo.deleteRoom(room)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    class Factory(
        private val roomRepo: RoomRepository,
        private val tenantRepo: TenantRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RoomViewModel(roomRepo, tenantRepo) as T
        }
    }
}
