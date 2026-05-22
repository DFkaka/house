
package com.example.house.ui.settlement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.house.data.local.entity.*
import com.example.house.data.repository.MeterReadingRepository
import com.example.house.data.repository.RoomRepository
import com.example.house.data.repository.SettlementRepository
import com.example.house.data.repository.TenantRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class SettlementState(
    val rooms: List<RoomEntity> = emptyList(),
    val selectedRoom: RoomEntity? = null,
    val activeTenant: TenantEntity? = null,
    val previousSettlement: SettlementEntity? = null,
    val settlements: List<SettlementEntity> = emptyList(),
    val waterMode: WaterMode = WaterMode.BY_METER,
    // Input fields
    val currentWaterReading: String = "",
    val currentElectricReading: String = "",
    val waterUnitPrice: String = "3.50",
    val electricUnitPrice: String = "0.65",
    val fixedWaterAmount: String = "30.00",
    // Calculated
    val waterUsage: Double = 0.0,
    val electricUsage: Double = 0.0,
    val waterFee: Double = 0.0,
    val electricFee: Double = 0.0,
    val totalFee: Double = 0.0,
    val monthsBetween: Int = 1,
    val isLoading: Boolean = true,
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
        viewModelScope.launch {
            roomRepo.allRooms.collect { rooms ->
                _state.value = _state.value.copy(rooms = rooms, isLoading = false)
            }
        }
    }

    fun selectRoom(roomId: Long) {
        viewModelScope.launch {
            val room = roomRepo.getRoomById(roomId)
            val tenant = tenantRepo.getActiveTenantByRoom(roomId)
            val prevSettlement = settlementRepo.getLatestSettlement(roomId)
            val settlements = settlementRepo.getSettlementsByRoom(roomId)

            // Pre-fill current readings from latest meter reading
            val latestReading = meterRepo.getLatestReading(roomId)
            val waterReading = latestReading?.waterReading?.toString() ?: ""
            val electricReading = latestReading?.electricReading?.toString() ?: ""

            _state.value = _state.value.copy(
                selectedRoom = room,
                activeTenant = tenant,
                previousSettlement = prevSettlement,
                currentWaterReading = waterReading,
                currentElectricReading = electricReading,
                showResult = false
            )

            settlements.collect { list ->
                _state.value = _state.value.copy(settlements = list)
            }
        }
    }

    fun setWaterMode(mode: WaterMode) {
        _state.value = _state.value.copy(waterMode = mode, showResult = false)
    }

    fun updateWaterReading(value: String) {
        _state.value = _state.value.copy(currentWaterReading = value, showResult = false)
    }

    fun updateElectricReading(value: String) {
        _state.value = _state.value.copy(currentElectricReading = value, showResult = false)
    }

    fun updateWaterUnitPrice(value: String) {
        _state.value = _state.value.copy(waterUnitPrice = value, showResult = false)
    }

    fun updateElectricUnitPrice(value: String) {
        _state.value = _state.value.copy(electricUnitPrice = value, showResult = false)
    }

    fun updateFixedWaterAmount(value: String) {
        _state.value = _state.value.copy(fixedWaterAmount = value, showResult = false)
    }

    fun calculate() {
        val s = _state.value
        val prev = s.previousSettlement
        val room = s.selectedRoom ?: return

        val currWater = s.currentWaterReading.toDoubleOrNull() ?: return
        val currElectric = s.currentElectricReading.toDoubleOrNull() ?: return

        // Electric calculation
        val prevElectric = room.electricMeterLast
        val electricUsage = currElectric - prevElectric
        val ePrice = s.electricUnitPrice.toDoubleOrNull() ?: 0.65
        val electricFee = electricUsage * ePrice

        // Water calculation
        val prevWater = room.waterMeterLast
        val waterUsage = currWater - prevWater
        val wPrice = s.waterUnitPrice.toDoubleOrNull() ?: 3.50
        val waterFee: Double
        val months: Int

        if (s.waterMode == WaterMode.FIXED) {
            // Calculate months between
            months = if (room.lastSettleDate != null) {
                val lastDate = LocalDate.parse(room.lastSettleDate)
                val now = LocalDate.now()
                ChronoUnit.MONTHS.between(lastDate, now).toInt().coerceAtLeast(1)
            } else 1
            val fixedAmount = s.fixedWaterAmount.toDoubleOrNull() ?: 30.00
            waterFee = fixedAmount * months
        } else {
            months = 0
            waterFee = waterUsage * wPrice
        }

        val totalFee = waterFee + electricFee

        _state.value = _state.value.copy(
            waterUsage = waterUsage,
            electricUsage = electricUsage,
            waterFee = waterFee,
            electricFee = electricFee,
            totalFee = totalFee,
            monthsBetween = months,
            showResult = true
        )
    }

    fun confirmSettlement(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val s = _state.value
                val room = s.selectedRoom ?: return@launch
                val tenant = s.activeTenant ?: return@launch
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

                val currWater = s.currentWaterReading.toDoubleOrNull() ?: 0.0
                val currElectric = s.currentElectricReading.toDoubleOrNull() ?: 0.0

                val settlement = SettlementEntity(
                    roomId = room.roomId,
                    tenantId = tenant.tenantId,
                    startDate = room.lastSettleDate,
                    endDate = today,
                    waterStart = room.waterMeterLast,
                    waterEnd = currWater,
                    waterUsage = s.waterUsage,
                    waterUnitPrice = s.waterUnitPrice.toDoubleOrNull() ?: 0.0,
                    waterFixedAmount = if (s.waterMode == WaterMode.FIXED)
                        s.fixedWaterAmount.toDoubleOrNull() else null,
                    waterFee = s.waterFee,
                    electricStart = room.electricMeterLast,
                    electricEnd = currElectric,
                    electricUsage = s.electricUsage,
                    electricUnitPrice = s.electricUnitPrice.toDoubleOrNull() ?: 0.0,
                    electricFee = s.electricFee,
                    totalFee = s.totalFee,
                    settleDate = today,
                    status = SettleStatus.PAID
                )
                settlementRepo.insertSettlement(settlement)

                // Update room last readings
                roomRepo.updateLastReadings(
                    room.roomId, currWater, currElectric, today,
                    s.waterFee, s.electricFee, s.totalFee
                )

                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    class Factory(
        private val settlementRepo: SettlementRepository,
        private val roomRepo: RoomRepository,
        private val tenantRepo: TenantRepository,
        private val meterRepo: MeterReadingRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettlementViewModel(settlementRepo, roomRepo, tenantRepo, meterRepo) as T
        }
    }
}
