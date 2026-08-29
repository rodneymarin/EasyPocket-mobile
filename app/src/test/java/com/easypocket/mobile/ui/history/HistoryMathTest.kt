package com.easypocket.mobile.ui.history

import com.easypocket.mobile.data.local.PurchaseHistoryEntity
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryMathTest {
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 8, 27)

    private fun entity(date: String, total: Double) = PurchaseHistoryEntity(
        id = date + total.toString(),
        listTitle = "L",
        listIcon = "$",
        date = LocalDate.parse(date).atStartOfDay(zone).plusHours(12).toInstant().toEpochMilli(),
        totalAmount = total,
        itemCount = 1,
    )

    @Test
    fun `dailyTotals fills every day in range with zeros`() {
        val records = listOf(entity("2026-08-27", 50.0), entity("2026-08-27", 20.0), entity("2026-08-25", 30.0))

        val points = HistoryMath.dailyTotals(records, 7, today)

        assertEquals(7, points.size)
        assertEquals(LocalDate.of(2026, 8, 21), points.first().date)
        assertEquals(LocalDate.of(2026, 8, 27), points.last().date)
        assertEquals(30.0, points[4].total, 0.001) // 2026-08-25
        assertEquals(70.0, points[6].total, 0.001) // 2026-08-27
        assertEquals(0.0, points[0].total, 0.001)
    }

    @Test
    fun `dailyTotals with null range returns only days with purchases`() {
        val records = listOf(entity("2026-08-27", 50.0), entity("2026-08-01", 30.0))

        val points = HistoryMath.dailyTotals(records, null, today)

        assertEquals(2, points.size)
        assertEquals(LocalDate.of(2026, 8, 1), points.first().date)
        assertEquals(30.0, points.first().total, 0.001)
        assertEquals(50.0, points.last().total, 0.001)
    }

    @Test
    fun `filterRecords excludes records older than range`() {
        val records = listOf(entity("2026-08-27", 50.0), entity("2026-08-01", 30.0))

        val filtered = HistoryMath.filterRecords(records, 7, today)

        assertEquals(1, filtered.size)
        assertEquals(50.0, filtered.first().totalAmount, 0.001)
    }

    @Test
    fun `isInRange with null range includes everything`() {
        val record = entity("2020-01-01", 1.0)
        assertEquals(true, HistoryMath.isInRange(record, null, today))
        assertEquals(false, HistoryMath.isInRange(record, 7, today))
    }
}
