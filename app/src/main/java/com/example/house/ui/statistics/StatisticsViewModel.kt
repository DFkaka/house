
package com.example.house.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.house.data.local.entity.SettleStatus
import com.example.house.data.repository.SettlementRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class StatisticsState(
    val totalUnpaid: Double = 0.0,
    val unpaidCount: Int = 0,
    val monthlyTotal: Double = 0.0,
    val quarterlyTotal: Double = 0.0,
    val yearlyTotal: Double = 0.0,
    val isLoading: Boolean = true
)

class StatisticsViewModel(
    private val settlementRepo: SettlementRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatisticsState())
    val state: StateFlow<StatisticsState> = _state.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            settlementRepo.getTotalUnpaid().collect { unpaid ->
                _state.value = _state.value.copy(
                    totalUnpaid = unpaid ?: 0.0
                )
            }
        }
        viewModelScope.launch {
            settlementRepo.getSettlementsByStatus(SettleStatus.UNPAID).collect { list ->
                _state.value = _state.value.copy(unpaidCount = list.size)
            }
        }

        viewModelScope.launch {
            val today = LocalDate.now()
            val fmt = DateTimeFormatter.ISO_LOCAL_DATE

            // This month
            val monthStart = today.withDayOfMonth(1).format(fmt)
            val monthlyTotal = settlementRepo.getTotalBetween(monthStart, today.format(fmt)) ?: 0.0

            // This quarter
            val quarterMonth = ((today.monthValue - 1) / 3) * 3 + 1
            val quarterStart = LocalDate.of(today.year, quarterMonth, 1).format(fmt)
            val quarterlyTotal = settlementRepo.getTotalBetween(quarterStart, today.format(fmt)) ?: 0.0

            // This year
            val yearStart = LocalDate.of(today.year, 1, 1).format(fmt)
            val yearlyTotal = settlementRepo.getTotalBetween(yearStart, today.format(fmt)) ?: 0.0

            _state.value = _state.value.copy(
                monthlyTotal = monthlyTotal,
                quarterlyTotal = quarterlyTotal,
                yearlyTotal = yearlyTotal,
                isLoading = false
            )
        }
    }

    class Factory(
        private val settlementRepo: SettlementRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatisticsViewModel(settlementRepo) as T
        }
    }
}
