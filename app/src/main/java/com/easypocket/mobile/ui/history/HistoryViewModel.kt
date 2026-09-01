package com.easypocket.mobile.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easypocket.mobile.data.local.PurchaseHistoryEntity
import com.easypocket.mobile.data.local.PurchaseHistoryWithItems
import com.easypocket.mobile.data.repository.CategoryRepository
import com.easypocket.mobile.data.repository.PurchaseHistoryRepository
import com.easypocket.mobile.domain.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val records: List<PurchaseHistoryWithItems> = emptyList(),
    val categories: List<Category> = emptyList(),
    val rangeDays: Int? = 7,
    val isLoading: Boolean = true,
) {
    val filteredRecords: List<PurchaseHistoryWithItems>
        get() = filteredRecordsOf(LocalDate.now())

    val chartPoints: List<HistoryChartPoint>
        get() = chartPointsOf(LocalDate.now(), rangeDays)

    val grandTotal: Double
        get() = grandTotalOf(LocalDate.now())

    val categorySlices: List<HistoryCategorySlice>
        get() {
            val totals = HistoryMath.categoryTotals(records, rangeDays, LocalDate.now())
            return totals.entries
                .map { (code, total) ->
                    HistoryCategorySlice(
                        categoryId = code,
                        name = code?.let { c -> categories.firstOrNull { it.id == c }?.name },
                        total = total,
                    )
                }
                .sortedByDescending { it.total }
        }

    fun filteredRecordsOf(today: LocalDate): List<PurchaseHistoryWithItems> =
        records.filter { HistoryMath.isInRange(it.record, rangeDays, today) }

    fun chartPointsOf(today: LocalDate, rangeDays: Int?): List<HistoryChartPoint> =
        HistoryMath.dailyTotals(records.map { it.record }, rangeDays, today)

    fun grandTotalOf(today: LocalDate): Double =
        records.filter { HistoryMath.isInRange(it.record, this.rangeDays, today) }
            .sumOf { it.record.totalAmount }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: PurchaseHistoryRepository,
    categoriesRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            historyRepository.observeAll().collect { records ->
                _uiState.value = _uiState.value.copy(records = records, isLoading = false)
            }
        }
        viewModelScope.launch {
            categoriesRepository.observeAll().collect { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
            }
        }
    }

    fun setRange(days: Int?) {
        _uiState.value = _uiState.value.copy(rangeDays = days)
    }

    suspend fun deleteRecord(id: String) {
        historyRepository.delete(id)
    }
}
