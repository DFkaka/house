
package com.example.house.ui.meter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.house.data.local.entity.MeterReadingEntity
import com.example.house.data.local.entity.RoomEntity
import com.example.house.data.repository.MeterReadingRepository
import com.example.house.data.repository.RoomRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class MeterReadingState(
    val readings: List<MeterReadingEntity> = emptyList(),
    val rooms: List<RoomEntity> = emptyList(),
    val selectedRoomId: Long? = null,
    val isLoading: Boolean = true
)

class MeterReadingViewModel(
    private val meterRepo: MeterReadingRepository,
    private val roomRepo: RoomRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MeterReadingState())
    val state: StateFlow<MeterReadingState> = _state.asStateFlow()

    private val _selectedRoomId = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            roomRepo.allRooms.collect { rooms ->
                _state.value = _state.value.copy(rooms = rooms)
            }
        }
        viewModelScope.launch {
            _selectedRoomId.flatMapLatest { roomId ->
                if (roomId != null) meterRepo.getReadingsByRoom(roomId)
                else meterRepo.getAllReadings()
            }.collect { readings ->
                _state.value = _state.value.copy(readings = readings, isLoading = false)
            }
        }
    }

    fun selectRoom(roomId: Long?) {
        _selectedRoomId.value = roomId
        _state.value = _state.value.copy(selectedRoomId = roomId)
    }

    fun addReading(roomId: Long, waterReading: Double, electricReading: Double, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val reading = MeterReadingEntity(
                    roomId = roomId,
                    recordDate = today,
                    waterReading = waterReading,
                    electricReading = electricReading
                )
                meterRepo.insertReading(reading)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun batchAddReadings(
        items: List<Triple<Long, Double, Double>>,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val readings = items.map { (roomId, water, electric) ->
                    MeterReadingEntity(
                        roomId = roomId,
                        recordDate = today,
                        waterReading = water,
                        electricReading = electric
                    )
                }
                meterRepo.insertReadings(readings)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    class Factory(
        private val meterRepo: MeterReadingRepository,
        private val roomRepo: RoomRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MeterReadingViewModel(meterRepo, roomRepo) as T
        }
    }
}
