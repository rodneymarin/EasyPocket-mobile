package com.easypocket.mobile.data.seed

import androidx.room.withTransaction
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.PriceEntity
import com.easypocket.mobile.data.local.ProductEntity
import com.easypocket.mobile.data.local.ShoppingListItemEntity
import com.easypocket.mobile.data.local.ShoppingListEntity
import com.easypocket.mobile.data.local.StoreEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class Seeder @Inject constructor(private val db: EasyPocketDatabase) {

    suspend fun seedIfEmpty() {
        if (db.storeDao().getAll().first().isEmpty()) {
            insertSeed()
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
