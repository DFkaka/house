
package com.example.house.ui.settlement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.house.data.local.model.*
import com.example.house.data.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class SettlementState(
    val rooms: List<Room> = emptyList(),
    val selectedRoom: Room? = null,
    val activeTenant: Tenant? = null,
    val previousSettlement: Settlement? = null,
    val settlements: List<Settlement> = emptyList(),
    val waterMode: WaterMode = WaterMode.BY_METER,
    val currentWaterReading: String = "",
    val currentElectricReading: String = "",
    val waterUnitPrice: String = "3.50",
    val electricUnitPrice: String = "0.65",
    val fixedWaterAmount: String = "30.00",
    val waterUsage: Double = 0.0,
    val electricUsage: Double = 0.0,
    val waterFee: Double = 0.0,
    val electricFee: Double = 0.0,
    val totalFee: Double = 0.0,
    val monthsBetween: Int = 1,
    val showResult: Boolean = false
)

enum class WaterMode { BY_METER, FIXED }

class SettlementViewModel(
    private val settlementRepo: SettlementRepository,
    private val roomRepo: RoomRepository,
    private val tenantRepo: TenantRepository,
    private val meterRepo: MeterReadingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettlementState())
    val state: StateFlow<SettlementState> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val rooms = roomRepo.getAll()
            _state.value = _state.value.copy(rooms = rooms)
        }
    }

    fun selectRoom(roomId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val room = roomRepo.getById(roomId)
            val tenant = tenantRepo.getActiveByRoom(roomId)
            val prev = settlementRepo.getLatest(roomId)
            val settlements = settlementRepo.getByRoom(roomId)
            val latestReading = meterRepo.getLatest(roomId)

            _state.value = _state.value.copy(
                selectedRoom = room,
                activeTenant = tenant,
                previousSettlement = prev,
                settlements = settlements,
                currentWaterReading = latestReading?.waterReading?.toString() ?: "",
                currentElectricReading = latestReading?.electricReading?.toString() ?: "",
                showResult = false
            )
        }
    }

    fun setWaterMode(mode: WaterMode) { _state.value = _state.value.copy(waterMode = mode, showResult = false) }
    fun updateWaterReading(v: String) { _state.value = _state.value.copy(currentWaterReading = v, showResult = false) }
    fun updateElectricReading(v: String) { _state.value = _state.value.copy(currentElectricReading = v, showResult = false) }
    fun updateWaterUnitPrice(v: String) { _state.value = _state.value.copy(waterUnitPrice = v, showResult = false) }
    fun updateElectricUnitPrice(v: String) { _state.value = _state.value.copy(electricUnitPrice = v, showResult = false) }
    fun updateFixedWaterAmount(v: String) { _state.value = _state.value.copy(fixedWaterAmount = v, showResult = false) }

    fun calculate() {
        val s = _state.value
        val room = s.selectedRoom ?: return
        val currWater = s.currentWaterReading.toDoubleOrNull() ?: return
        val currElectric = s.currentElectricReading.toDoubleOrNull() ?: return

        val electricUsage = currElectric - room.electricMeterLast
        val ePrice = s.electricUnitPrice.toDoubleOrNull() ?: 0.65
        val electricFee = electricUsage * ePrice

        val waterUsage = currWater - room.waterMeterLast
        val wPrice = s.waterUnitPrice.toDoubleOrNull() ?: 3.50
        val waterFee: Double
        val months: Int

        if (s.waterMode == WaterMode.FIXED) {
            months = if (room.lastSettleDate != null) {
                ChronoUnit.MONTHS.between(LocalDate.parse(room.lastSettleDate), LocalDate.now()).toInt().coerceAtLeast(1)
            } else 1
            waterFee = (s.fixedWaterAmount.toDoubleOrNull() ?: 30.00) * months
        } else {
            months = 0
            waterFee = waterUsage * wPrice
        }

        _state.value = _state.value.copy(
            waterUsage = waterUsage, electricUsage = electricUsage,
            waterFee = waterFee, electricFee = electricFee,
            totalFee = waterFee + electricFee, monthsBetween = months, showResult = true
        )
    }

    fun confirmSettlement(onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val s = _state.value
                val room = s.selectedRoom ?: return@launch
                val tenant = s.activeTenant ?: return@launch
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val currWater = s.currentWaterReading.toDoubleOrNull() ?: 0.0
                val currElectric = s.currentElectricReading.toDoubleOrNull() ?: 0.0

                settlementRepo.insert(Settlement(
                    roomId = room.roomId, tenantId = tenant.tenantId,
                    startDate = room.lastSettleDate, endDate = today,
                    waterStart = room.waterMeterLast, waterEnd = currWater,
                    waterUsage = s.waterUsage,
                    waterUnitPrice = s.waterUnitPrice.toDoubleOrNull() ?: 0.0,
                    waterFixedAmount = if (s.waterMode == WaterMode.FIXED) s.fixedWaterAmount.toDoubleOrNull() else null,
                    waterFee = s.waterFee,
                    electricStart = room.electricMeterLast, electricEnd = currElectric,
                    electricUsage = s.electricUsage,
                    electricUnitPrice = s.electricUnitPrice.toDoubleOrNull() ?: 0.0,
                    electricFee = s.electricFee,
                    totalFee = s.totalFee, settleDate = today, status = "PAID"
                ))

                roomRepo.updateLastReadings(room.roomId, currWater, currElectric, today, s.waterFee, s.electricFee, s.totalFee)

                // Refresh
                selectRoom(room.roomId)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    class Factory(
        private val sr: SettlementRepository,
        private val rr: RoomRepository,
        private val tr: TenantRepository,
        private val mr: MeterReadingRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettlementViewModel(sr, rr, tr, mr) as T
        }
    }
}
