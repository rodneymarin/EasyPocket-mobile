package com.easypocket.mobile.data.repository

import androidx.room.withTransaction
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.PriceDao
import com.easypocket.mobile.data.local.PriceEntity
import com.easypocket.mobile.data.local.StoreDao
import com.easypocket.mobile.data.local.StoreEntity
import com.easypocket.mobile.domain.Alphabet
import com.easypocket.mobile.domain.Store
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// A store delete also takes its prices down and clears the store on the
// shopping list items that pointed at it; this snapshot restores all of it.
data class DeletedStores(
    val stores: List<StoreEntity>,
    val prices: List<PriceEntity>,
    val itemIdsByStore: Map<String, List<Long>>,
)

@Singleton
class StoreRepository @Inject constructor(
    private val db: EasyPocketDatabase,
    private val storeDao: StoreDao,
    private val priceDao: PriceDao,
) {

    suspend fun getAll(): List<Store> = storeDao.getAll().first().map { it.toDomain() }.sortedWith(Alphabet.comparator { it.description })

    fun observeAll(): Flow<List<Store>> =
        storeDao.getAll().map { stores -> stores.map { it.toDomain() }.sortedWith(Alphabet.comparator { it.description }) }

    suspend fun create(description: String): Store {
        val store = Store(UUID.randomUUID().toString(), description, 0)
        storeDao.insert(StoreEntity(store.id, store.description, store.color))
        return store
    }

    suspend fun update(store: Store) = storeDao.update(StoreEntity(store.id, store.description, store.color))

    suspend fun deleteAll(ids: List<String>): DeletedStores? {
        if (ids.isEmpty()) return null
        return db.withTransaction {
            val stores = storeDao.getAll().first().filter { it.id in ids }
            if (stores.isEmpty()) return@withTransaction null
            val items = db.listDao().getItemsByStoreIds(ids)
            val snapshot = DeletedStores(
                stores = stores,
                prices = priceDao.getAll().first().filter { it.storeId in ids },
                itemIdsByStore = items.filter { it.storeId in ids }
                    .groupBy({ it.storeId!! }, { it.id }),
            )
            storeDao.deleteByIds(ids)
            snapshot
        }
    }

    suspend fun restore(deletion: DeletedStores) {
        if (deletion.stores.isEmpty()) return
        db.withTransaction {
            deletion.stores.forEach { storeDao.insert(it) }
            priceDao.insertAll(deletion.prices)
            deletion.itemIdsByStore.forEach { (storeId, ids) ->
                db.listDao().setItemsStore(ids, storeId)
            }
        }
    }

    private fun StoreEntity.toDomain() = Store(id, description, color)
}
