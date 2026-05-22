
package com.example.house.ui.meter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.house.data.local.model.MeterReading
import com.example.house.data.local.model.Room
import com.example.house.data.repository.MeterReadingRepository
import com.example.house.data.repository.RoomRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class MeterReadingState(
    val readings: List<MeterReading> = emptyList(),
    val rooms: List<Room> = emptyList(),
    val isLoading: Boolean = true
)

class MeterReadingViewModel(
    private val meterRepo: MeterReadingRepository,
    private val roomRepo: RoomRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MeterReadingState())
    val state: StateFlow<MeterReadingState> = _state.asStateFlow()

    init { load() }

    fun load(roomId: Long? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val rooms = roomRepo.getAll()
            val readings = if (roomId != null) meterRepo.getByRoom(roomId) else meterRepo.getAll()
            _state.value = MeterReadingState(readings = readings, rooms = rooms, isLoading = false)
        }
    }

    fun addReading(roomId: Long, water: Double, electric: Double, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                meterRepo.insert(MeterReading(roomId = roomId, recordDate = today, waterReading = water, electricReading = electric))
                load()
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
