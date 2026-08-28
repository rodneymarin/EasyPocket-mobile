package com.easypocket.mobile.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Query("SELECT * FROM stores ORDER BY description COLLATE NOCASE")
    fun getAll(): Flow<List<StoreEntity>>

    @Insert
    suspend fun insert(store: StoreEntity)

    @Update
    suspend fun update(store: StoreEntity)

    @Query("DELETE FROM stores WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY product_name COLLATE NOCASE")
    fun getAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE LOWER(product_name) = LOWER(:name) LIMIT 1")
    suspend fun getByName(name: String): ProductEntity?

    @Insert
    suspend fun insert(product: ProductEntity)

    @Update
    suspend fun update(product: ProductEntity)

    @Query("DELETE FROM products WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}

@Dao
interface PriceDao {
    @Query("SELECT * FROM product_prices")
    fun getAll(): Flow<List<PriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prices: List<PriceEntity>)

    @Query("DELETE FROM product_prices WHERE product_id = :productId")
    suspend fun deleteForProduct(productId: String)

    @Query("DELETE FROM product_prices WHERE store_id IN (:storeIds)")
    suspend fun deleteForStores(storeIds: List<String>)
}

@Dao
interface ShoppingListDao {
    @Transaction
    @Query("SELECT * FROM shopping_lists ORDER BY title COLLATE NOCASE")
    fun getAll(): Flow<List<ShoppingListWithItems>>

    @Transaction
    @Query("SELECT * FROM shopping_lists WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ShoppingListWithItems?

    @Insert
    suspend fun insert(list: ShoppingListEntity)

    @Query("UPDATE shopping_lists SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: String, title: String)

    @Query("DELETE FROM shopping_lists WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert
    suspend fun insertItem(item: ShoppingListItemEntity): Long

    @Update
    suspend fun updateItem(item: ShoppingListItemEntity)

    @Query("UPDATE shopping_list_items SET product_id = :productId, store_id = :storeId, quantity = :quantity, done = :done, pinned = :pinned WHERE id = :id")
    suspend fun updateItemFields(id: Long, productId: String, storeId: String?, quantity: Double, done: Boolean, pinned: Boolean)

    @Query("UPDATE shopping_list_items SET done = :done WHERE id = :id")
    suspend fun toggleItemDone(id: Long, done: Boolean)

    @Query("DELETE FROM shopping_list_items WHERE id IN (:ids)")
    suspend fun removeItems(ids: List<Long>)

    @Query("UPDATE shopping_list_items SET shopping_list_id = :toListId WHERE id IN (:ids)")
    suspend fun moveItems(ids: List<Long>, toListId: String)

    @Query("UPDATE shopping_list_items SET pinned = :pinned WHERE id IN (:ids)")
    suspend fun pinItems(ids: List<Long>, pinned: Boolean)

    @Query("UPDATE shopping_list_items SET pinned = 0 WHERE shopping_list_id = :listId")
    suspend fun unpinAll(listId: String)

    @Query("DELETE FROM shopping_list_items WHERE shopping_list_id = :listId AND done = 1")
    suspend fun removeCompleted(listId: String)

    @Query("UPDATE shopping_list_items SET done = 0 WHERE shopping_list_id = :listId AND done = 1")
    suspend fun uncheckAll(listId: String)
}
