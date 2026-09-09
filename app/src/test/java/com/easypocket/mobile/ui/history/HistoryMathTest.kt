package com.easypocket.mobile.ui.history

import com.easypocket.mobile.data.local.PurchaseHistoryCategoryTotalEntity
import com.easypocket.mobile.data.local.PurchaseHistoryEntity
import com.easypocket.mobile.data.local.PurchaseHistoryItemEntity
import com.easypocket.mobile.data.local.PurchaseHistoryWithItems
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private fun item(historyId: String, categoryCode: String?, totalPrice: Double) = PurchaseHistoryItemEntity(
        historyId = historyId,
        productName = "P",
        storeName = null,
        quantity = 1.0,
        unitPrice = totalPrice,
        totalPrice = totalPrice,
        categoryCode = categoryCode,
        itemUid = "uid-$historyId-$categoryCode-$totalPrice",
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
    fun `dailyTotals aggregates long ranges into at most 7 equal buckets`() {
        val records = listOf(
            entity("2026-08-01", 10.0),
            entity("2026-08-15", 20.0),
            entity("2026-08-27", 40.0), // último día del rango de 30
            entity("2026-08-16", 5.0),
        )

        val points = HistoryMath.dailyTotals(records, 30, today)

        assertTrue(points.size <= 7)
        assertEquals(LocalDate.of(2026, 8, 27), points.last().date)
        assertEquals(75.0, points.sumOf { it.total }, 0.001) // conserva todos los totales
        assertEquals(40.0, points.last().total, 0.001)
        assertTrue(points.zipWithNext().all { (a, b) -> a.date < b.date }) // buckets ordenados
    }

    @Test
    fun `dailyTotals keeps every day for a 7 day range`() {
        val records = listOf(entity("2026-08-27", 10.0))

        val points = HistoryMath.dailyTotals(records, 7, today)

        assertEquals(7, points.size)
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

    private fun categoryTotal(historyId: String, categoryCode: String?, total: Double) =
        PurchaseHistoryCategoryTotalEntity(
            historyId = historyId,
            categoryCode = categoryCode,
            total = total,
        )

    @Test
    fun `categoryTotals groups stored category totals and falls back to null`() {
        val records = listOf(
            PurchaseHistoryWithItems(
                entity("2026-08-27", 30.0),
                listOf(
                    item("h1", "CAT1", 10.0),
                    item("h1", "CAT1", 5.0),
                    item("h1", null, 15.0),
                ),
                listOf(
                    categoryTotal("h1", "CAT1", 15.0),
                    categoryTotal("h1", null, 15.0),
                ),
            ),
            PurchaseHistoryWithItems(
                entity("2026-08-26", 20.0),
                listOf(item("h2", "CAT2", 20.0)),
                listOf(categoryTotal("h2", "CAT2", 20.0)),
            ),
        )

        val totals = HistoryMath.categoryTotals(records, 7, today)

        assertEquals(3, totals.size)
        assertEquals(15.0, totals["CAT1"]!!, 0.001)
        assertEquals(20.0, totals["CAT2"]!!, 0.001)
        assertEquals(15.0, totals[null]!!, 0.001)
    }

    @Test
    fun `categoryTotals includes the manual total of records without item prices`() {
        val records = listOf(
            PurchaseHistoryWithItems(
                entity("2026-08-27", 50.0),
                listOf(
                    item("h1", "CAT1", 0.0),
                    item("h1", "CAT1", 0.0),
                ),
                listOf(categoryTotal("h1", "CAT1", 50.0)),
            ),
        )

        val totals = HistoryMath.categoryTotals(records, 7, today)

        assertEquals(50.0, totals["CAT1"]!!, 0.001)
    }

    @Test
    fun `dailyTotals reflects purchases for a 365 day range`() {
        val start = LocalDate.of(2025, 8, 28) // inicio de la ventana de 365 días
        val records = listOf(
            entity(start.plusDays(50).toString(), 100.0),
            entity(start.plusDays(200).toString(), 300.0),
            entity("2026-08-27", 50.0),
        )

        val points = HistoryMath.dailyTotals(records, 365, today)

        assertEquals(450.0, points.sumOf { it.total }, 0.001) // conserva todos los totales
    }

    @Test
    fun `categoryTotals respects range filter`() {
        val records = listOf(
            PurchaseHistoryWithItems(
                entity("2026-08-27", 30.0),
                listOf(item("h1", "CAT1", 30.0)),
                listOf(categoryTotal("h1", "CAT1", 30.0)),
            ),
            PurchaseHistoryWithItems(
                entity("2026-08-01", 50.0),
                listOf(item("h2", "CAT2", 50.0)),
                listOf(categoryTotal("h2", "CAT2", 50.0)),
            ),
        )

        val inRange = HistoryMath.categoryTotals(records, 7, today)
        assertEquals(mapOf<String?, Double>("CAT1" to 30.0), inRange)

        val all = HistoryMath.categoryTotals(records, null, today)
        assertEquals(2, all.size)
    }
}
