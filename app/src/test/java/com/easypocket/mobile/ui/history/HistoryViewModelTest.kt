package com.easypocket.mobile.ui.history

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.PurchaseHistoryEntity
import com.easypocket.mobile.data.local.PurchaseHistoryWithItems
import com.easypocket.mobile.data.repository.PurchaseHistoryRepository
import com.easypocket.mobile.util.MainDispatcherRule
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: EasyPocketDatabase

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    private fun createVm(): HistoryViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries().build()
        val repo = PurchaseHistoryRepository(
            db = db,
            historyDao = db.purchaseHistoryDao(),
            listDao = db.listDao(),
            productDao = db.productDao(),
            priceDao = db.priceDao(),
            storeDao = db.storeDao(),
        )
        return HistoryViewModel(repo)
    }

    private fun entity(date: String, total: Double) = PurchaseHistoryEntity(
        id = date + total.toString(),
        listTitle = "L",
        listIcon = "$",
        date = LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).plusHours(12)
            .toInstant().toEpochMilli(),
        totalAmount = total,
        itemCount = 1,
    )

    @Test
    fun `setRange updates filter state`() = runTest {
        val vm = createVm()
        advanceUntilIdle()
        assertEquals(7, vm.uiState.value.rangeDays)
        vm.setRange(30)
        assertEquals(30, vm.uiState.value.rangeDays)
        vm.setRange(null)
        assertEquals(null, vm.uiState.value.rangeDays)
    }

    @Test
    fun `uiState filters records by range and computes chart points`() {
        val records = listOf(
            PurchaseHistoryWithItems(entity("2026-08-27", 50.0), emptyList()),
            PurchaseHistoryWithItems(entity("2026-08-01", 30.0), emptyList()),
        )
        val today = LocalDate.of(2026, 8, 27)
        val state = HistoryUiState(records = records, rangeDays = 7, isLoading = false)

        assertEquals(1, state.filteredRecordsOf(today).size)
        assertEquals(7, state.chartPointsOf(today, 7).size)
        assertEquals(50.0, state.chartPointsOf(today, 7).last().total, 0.001)
        assertEquals(2, state.chartPointsOf(today, null).size)
        assertEquals(50.0, state.grandTotalOf(today), 0.001)
    }
}
