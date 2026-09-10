package com.easypocket.mobile.data.repository

import androidx.room.withTransaction
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.PriceDao
import com.easypocket.mobile.data.local.ProductDao
import com.easypocket.mobile.data.local.PurchaseHistoryDao
import com.easypocket.mobile.data.local.PurchaseHistoryCategoryTotalEntity
import com.easypocket.mobile.data.local.PurchaseHistoryEntity
import com.easypocket.mobile.data.local.PurchaseHistoryItemEntity
import com.easypocket.mobile.data.local.PurchaseHistoryWithItems
import com.easypocket.mobile.data.local.ShoppingListDao
import com.easypocket.mobile.data.local.StoreDao
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Singleton
class PurchaseHistoryRepository @Inject constructor(
    private val db: EasyPocketDatabase,
    private val historyDao: PurchaseHistoryDao,
    private val listDao: ShoppingListDao,
    private val productDao: ProductDao,
    private val priceDao: PriceDao,
    private val storeDao: StoreDao,
) {

    fun observeAll(): Flow<List<PurchaseHistoryWithItems>> = historyDao.observeAll()

    suspend fun delete(id: String): PurchaseHistoryWithItems? {
        val record = historyDao.observeAll().first().firstOrNull { it.record.id == id }
            ?: return null
        historyDao.deleteById(id)
        return record
    }

    suspend fun restore(record: PurchaseHistoryWithItems) {
        db.withTransaction {
            historyDao.insertHistory(record.record)
            historyDao.insertItems(record.items)
            if (record.categoryTotals.isNotEmpty()) historyDao.insertCategoryTotals(record.categoryTotals)
        }
    }

    suspend fun archiveCompleted(
        listId: String,
        date: Long = System.currentTimeMillis(),
        manualTotal: Double? = null,
    ) {
        db.withTransaction {
            val relation = listDao.getById(listId) ?: return@withTransaction
            val doneItems = relation.items.filter { it.done }
            if (doneItems.isEmpty()) return@withTransaction

            val products = productDao.getAll().first().associateBy { it.id }
            val pricesByProduct = priceDao.getAll().first().groupBy { it.productId }
            val stores = storeDao.getAll().first().associateBy { it.id }

            val historyId = UUID.randomUUID().toString()
            val historyItems = doneItems.map { item ->
                val unitPrice = item.storeId
                    ?.let { sid ->
                        pricesByProduct[item.productId].orEmpty().firstOrNull { it.storeId == sid }?.value
                    }
                    ?: 0.0
                PurchaseHistoryItemEntity(
                    historyId = historyId,
                    productName = products[item.productId]?.productName ?: "",
                    storeName = item.storeId?.let { stores[it]?.description },
                    quantity = item.quantity,
                    unitPrice = unitPrice,
                    totalPrice = unitPrice * item.quantity,
                    categoryCode = item.categoryId,
                    itemUid = UUID.randomUUID().toString(),
                )
            }
            val itemsTotal = historyItems.sumOf { it.totalPrice }
            val totalAmount = if (itemsTotal == 0.0 && manualTotal != null && manualTotal > 0.0) manualTotal else itemsTotal
            val categoryTotals = categoryTotalRows(historyId, historyItems, itemsTotal, totalAmount)
            val history = PurchaseHistoryEntity(
                id = historyId,
                listTitle = relation.list.title,
                listIcon = relation.list.icon,
                date = date,
                totalAmount = totalAmount,
                itemCount = historyItems.size,
            )
            historyDao.insertHistory(history)
            historyDao.insertItems(historyItems)
            if (categoryTotals.isNotEmpty()) historyDao.insertCategoryTotals(categoryTotals)
            listDao.removeCompleted(listId)
        }
    }

    // Per-category totals that feed the category charts. With item prices the
    // totals are the sum of the item totals per category. When the items have
    // no prices and the purchase was archived with a manual total, the whole
    // amount goes to the single category shared by the items (a list with a
    // fixed category), or to "no category" when they mix categories.
    private fun categoryTotalRows(
        historyId: String,
        items: List<PurchaseHistoryItemEntity>,
        itemsTotal: Double,
        recordedTotal: Double,
    ): List<PurchaseHistoryCategoryTotalEntity> {
        if (recordedTotal <= 0.0) return emptyList()
        if (itemsTotal > 0.0) {
            return items.groupBy { it.categoryCode }.mapNotNull { (code, group) ->
                val total = group.sumOf { it.totalPrice }
                if (total > 0.0) {
                    PurchaseHistoryCategoryTotalEntity(historyId = historyId, categoryCode = code, total = total)
                } else {
                    null
                }
            }
        }
        val sharedCategory = items.map { it.categoryCode }.distinct().singleOrNull()
        return listOf(
            PurchaseHistoryCategoryTotalEntity(historyId = historyId, categoryCode = sharedCategory, total = recordedTotal)
        )
    }
}
