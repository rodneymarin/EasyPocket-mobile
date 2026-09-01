package com.easypocket.mobile.data.repository

import androidx.room.withTransaction
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.PriceDao
import com.easypocket.mobile.data.local.ProductDao
import com.easypocket.mobile.data.local.PurchaseHistoryDao
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

    suspend fun delete(id: String) = historyDao.deleteById(id)

    suspend fun archiveCompleted(listId: String, date: Long = System.currentTimeMillis()) {
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
                    categoryCode = products[item.productId]?.categoryId,
                    itemUid = UUID.randomUUID().toString(),
                )
            }
            val history = PurchaseHistoryEntity(
                id = historyId,
                listTitle = relation.list.title,
                listIcon = relation.list.icon,
                date = date,
                totalAmount = historyItems.sumOf { it.totalPrice },
                itemCount = historyItems.size,
            )
            historyDao.insertHistory(history)
            historyDao.insertItems(historyItems)
            listDao.removeCompleted(listId)
        }
    }
}
