
package com.example.house.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.house.data.repository.SettlementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    init { load() }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val totalUnpaid = settlementRepo.getTotalUnpaid()
            val unpaid = settlementRepo.getUnpaid()

            val today = LocalDate.now()
            val fmt = DateTimeFormatter.ISO_LOCAL_DATE
            val monthStart = today.withDayOfMonth(1).format(fmt)
            val quarterMonth = ((today.monthValue - 1) / 3) * 3 + 1
            val quarterStart = LocalDate.of(today.year, quarterMonth, 1).format(fmt)
            val yearStart = LocalDate.of(today.year, 1, 1).format(fmt)

            _state.value = StatisticsState(
                totalUnpaid = totalUnpaid,
                unpaidCount = unpaid.size,
                monthlyTotal = settlementRepo.getTotalBetween(monthStart, today.format(fmt)),
                quarterlyTotal = settlementRepo.getTotalBetween(quarterStart, today.format(fmt)),
                yearlyTotal = settlementRepo.getTotalBetween(yearStart, today.format(fmt)),
                isLoading = false
            )
        }
    }

    class Factory(private val repo: SettlementRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = StatisticsViewModel(repo) as T
    }
}
