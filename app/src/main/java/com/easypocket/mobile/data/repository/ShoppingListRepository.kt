package com.easypocket.mobile.data.repository

import com.easypocket.mobile.data.local.ShoppingListDao
import com.easypocket.mobile.data.local.ShoppingListEntity
import com.easypocket.mobile.data.local.ShoppingListItemEntity
import com.easypocket.mobile.data.local.ShoppingListWithItems
import com.easypocket.mobile.domain.Alphabet
import com.easypocket.mobile.domain.ListIcon
import com.easypocket.mobile.domain.ShoppingList
import com.easypocket.mobile.domain.ShoppingListItem
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class ShoppingListRepository @Inject constructor(private val listDao: ShoppingListDao) {

    private fun withItems(relation: ShoppingListWithItems) =
        ShoppingList(
            relation.list.id,
            relation.list.title,
            relation.list.icon,
            relation.items.map { ShoppingListItem(it.id, it.productId, it.quantity, it.storeId, it.done, it.pinned) },
        )

    suspend fun getAll(): List<ShoppingList> = listDao.getAll().first().map(::withItems).sortedWith(Alphabet.comparator { it.title })

    suspend fun getById(id: String): ShoppingList? = listDao.getById(id)?.let(::withItems)

    suspend fun create(title: String, icon: String = ListIcon.DEFAULT): ShoppingList {
        val list = ShoppingList(UUID.randomUUID().toString(), title, icon.ifBlank { ListIcon.DEFAULT })
        listDao.insert(ShoppingListEntity(list.id, list.title, list.icon))
        return list
    }

    suspend fun delete(id: String) = listDao.deleteById(id)

    suspend fun rename(id: String, title: String, icon: String) =
        listDao.updateTitleAndIcon(id, title, icon)

    suspend fun addItem(listId: String, productId: String, storeId: String?, quantity: Double): Long =
        listDao.insertItem(
            ShoppingListItemEntity(shoppingListId = listId, productId = productId, storeId = storeId, quantity = quantity)
        )

    suspend fun toggleItemDone(itemId: Long, done: Boolean) = listDao.toggleItemDone(itemId, done)

    suspend fun updateItem(item: ShoppingListItem) =
        listDao.updateItemFields(item.id, item.productId, item.storeId, item.quantity, item.done, item.pinned)

    suspend fun removeItems(ids: List<Long>) = listDao.removeItems(ids)

    suspend fun moveItems(ids: List<Long>, toListId: String) = listDao.moveItems(ids, toListId)

    suspend fun pinItems(ids: List<Long>, pinned: Boolean) = listDao.pinItems(ids, pinned)

    suspend fun uncheckAll(listId: String) = listDao.uncheckAll(listId)

    suspend fun removeCompleted(listId: String) = listDao.removeCompleted(listId)
}