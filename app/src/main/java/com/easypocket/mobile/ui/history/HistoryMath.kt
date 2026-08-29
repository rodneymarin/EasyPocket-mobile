package com.easypocket.mobile.ui.history

import com.easypocket.mobile.data.local.PurchaseHistoryEntity
import com.easypocket.mobile.data.local.PurchaseHistoryWithItems
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HistoryChartPoint(val date: LocalDate, val total: Double)

data class HistoryCategorySlice(
    val categoryId: String?,
    val name: String?,
    val total: Double,
)

object HistoryMath {

    private fun rangeStart(rangeDays: Int?, today: LocalDate): LocalDate? =
        rangeDays?.let { today.minusDays((it - 1).toLong()) }

    private fun toMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun isInRange(record: PurchaseHistoryEntity, rangeDays: Int?, today: LocalDate): Boolean {
        val start = rangeStart(rangeDays, today) ?: return true
        return record.date >= toMillis(start)
    }

    fun filterRecords(
        records: List<PurchaseHistoryEntity>,
        rangeDays: Int?,
        today: LocalDate,
    ): List<PurchaseHistoryEntity> =
        if (rangeDays == null) records else records.filter { isInRange(it, rangeDays, today) }

    fun dailyTotals(
        records: List<PurchaseHistoryEntity>,
        rangeDays: Int?,
        today: LocalDate,
    ): List<HistoryChartPoint> {
        val zone = ZoneId.systemDefault()
        val filtered = filterRecords(records, rangeDays, today)
        val byDay = filtered
            .groupBy { Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
            .mapValues { (_, dayRecords) -> dayRecords.sumOf { it.totalAmount } }
        val start = rangeStart(rangeDays, today)
        val days = if (start != null) {
            generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.toList()
        } else {
            byDay.keys.sorted()
        }
        return days.map { HistoryChartPoint(it, byDay[it] ?: 0.0) }
    }

    fun categoryTotals(
        records: List<PurchaseHistoryWithItems>,
        rangeDays: Int?,
        today: LocalDate,
    ): Map<String?, Double> =
        records.filter { isInRange(it.record, rangeDays, today) }
            .flatMap { it.items }
            .groupBy { it.categoryCode }
            .mapValues { (_, items) -> items.sumOf { it.totalPrice } }
}
