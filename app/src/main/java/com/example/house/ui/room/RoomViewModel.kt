
package com.example.house.ui.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.house.data.local.model.Room
import com.example.house.data.local.model.Tenant
import com.example.house.data.repository.RoomRepository
import com.example.house.data.repository.TenantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RoomListState(
    val rooms: List<RoomWithTenant> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = ""
)

data class RoomWithTenant(
    val room: Room,
    val tenantName: String = ""
)

class RoomViewModel(
    private val roomRepo: RoomRepository,
    private val tenantRepo: TenantRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RoomListState())
    val state: StateFlow<RoomListState> = _state.asStateFlow()

    init {
        loadRooms()
    }

    fun loadRooms(query: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val rooms = if (query.isNotBlank()) roomRepo.search(query) else roomRepo.getAll()
            val items = rooms.map { room ->
                val tenant = room.tenantId?.let { tenantRepo.getById(it) }
                RoomWithTenant(room, tenant?.name ?: "")
            }
            _state.value = RoomListState(rooms = items, isLoading = false, searchQuery = query)
        }
    }

    fun onSearch(query: String) {
        _state.value = _state.value.copy(searchQuery = query, isLoading = true)
        loadRooms(query)
    }

    fun addRoom(room: Room, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                roomRepo.insert(room)
                loadRooms()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun deleteRoom(roomId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                roomRepo.delete(roomId)
                loadRooms()
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
