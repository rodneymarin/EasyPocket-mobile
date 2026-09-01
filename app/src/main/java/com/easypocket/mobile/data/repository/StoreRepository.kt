package com.easypocket.mobile.data.repository

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

@Singleton
class StoreRepository @Inject constructor(private val storeDao: StoreDao) {

    suspend fun getAll(): List<Store> = storeDao.getAll().first().map { it.toDomain() }.sortedWith(Alphabet.comparator { it.description })

    fun observeAll(): Flow<List<Store>> =
        storeDao.getAll().map { stores -> stores.map { it.toDomain() }.sortedWith(Alphabet.comparator { it.description }) }

    suspend fun create(description: String): Store {
        val store = Store(UUID.randomUUID().toString(), description, 0)
        storeDao.insert(StoreEntity(store.id, store.description, store.color))
        return store
    }

    suspend fun update(store: Store) = storeDao.update(StoreEntity(store.id, store.description, store.color))

    suspend fun deleteAll(ids: List<String>) = storeDao.deleteByIds(ids)

    private fun StoreEntity.toDomain() = Store(id, description, color)
}