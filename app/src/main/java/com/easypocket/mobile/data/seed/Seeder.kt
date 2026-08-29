package com.easypocket.mobile.data.seed

import androidx.room.withTransaction
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.PriceEntity
import com.easypocket.mobile.data.local.ProductEntity
import com.easypocket.mobile.data.local.PurchaseHistoryEntity
import com.easypocket.mobile.data.local.PurchaseHistoryItemEntity
import com.easypocket.mobile.data.local.ShoppingListItemEntity
import com.easypocket.mobile.data.local.ShoppingListEntity
import com.easypocket.mobile.data.local.StoreEntity
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.flow.first

@Singleton
class Seeder @Inject constructor(private val db: EasyPocketDatabase) {

    suspend fun seedIfEmpty() {
        if (db.storeDao().getAll().first().isEmpty()) {
            insertSeed()
        }
    }

    suspend fun seedFakeHistory() {
        db.withTransaction {
            db.openHelper.writableDatabase.execSQL("DELETE FROM purchase_history")
            val random = Random(42)
            val titles = listOf("Compras semanales", "Verdulería", "Almacén", "Limpieza", "Feriado")
            val icons = listOf("🛒", "🥬", "🧻", "$", "🎉")
            val products = listOf(
                "Yerba", "Leche", "Pan", "Huevos", "Fideos", "Arroz", "Detergente",
                "Manzanas", "Papel higiénico", "Queso", "Tomates", "Aceite",
            )
            val stores = listOf("Supermercado", "Verdulería", "Almacén", null)
            val today = LocalDate.now()
            val daysWithPurchases = (0 until 90).filter { random.nextInt(100) < 60 }
            daysWithPurchases.forEach { offset ->
                val purchasesToday = 1 + if (random.nextInt(100) < 25) 1 else 0
                repeat(purchasesToday) {
                    val historyId = UUID.randomUUID().toString()
                    val date = today.minusDays(offset.toLong())
                        .atTime(9 + random.nextInt(12), random.nextInt(60))
                        .atZone(ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                    val items = (1..(1 + random.nextInt(3))).map {
                        val unitPrice = (5..80).random(random).toDouble()
                        val quantity = random.nextDouble(1.0, 3.0)
                        PurchaseHistoryItemEntity(
                            historyId = historyId,
                            productName = products[random.nextInt(products.size)],
                            storeName = stores[random.nextInt(stores.size)],
                            quantity = quantity,
                            unitPrice = unitPrice,
                            totalPrice = unitPrice * quantity,
                        )
                    }
                    db.purchaseHistoryDao().insertHistory(
                        PurchaseHistoryEntity(
                            id = historyId,
                            listTitle = titles[random.nextInt(titles.size)],
                            listIcon = icons[random.nextInt(icons.size)],
                            date = date,
                            totalAmount = items.sumOf { it.totalPrice },
                            itemCount = items.size,
                        )
                    )
                    db.purchaseHistoryDao().insertItems(items)
                }
            }
        }
    }

    suspend fun resetToSeed() {
        db.clearAllTables()
        insertSeed()
    }

    private suspend fun insertSeed() {
        db.withTransaction {
            SeedData.stores.forEach { store ->
                db.storeDao().insert(StoreEntity(store.id, store.description, store.color))
            }
            SeedData.products.forEach { product ->
                db.productDao().insert(ProductEntity(product.id, product.productName, product.unitOfMeasurement.raw))
                db.priceDao().insertAll(product.prices.map { PriceEntity(product.id, it.storeId, it.value) })
            }
            SeedData.lists.forEach { list ->
                db.listDao().insert(ShoppingListEntity(list.id, list.title))
                list.items.forEach { item ->
                    db.listDao().insertItem(
                        ShoppingListItemEntity(
                            shoppingListId = list.id,
                            productId = item.productId,
                            storeId = item.storeId,
                            quantity = item.quantity,
                            done = item.done,
                            pinned = item.pinned,
                        )
                    )
                }
            }
        }
    }
}
