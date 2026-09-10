package com.easypocket.mobile.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.ProductLastCategoryEntity
import com.easypocket.mobile.data.local.PurchaseHistoryCategoryTotalEntity
import com.easypocket.mobile.data.local.PurchaseHistoryEntity
import com.easypocket.mobile.data.local.PurchaseHistoryItemEntity
import com.easypocket.mobile.data.local.ShoppingListItemEntity
import com.easypocket.mobile.domain.Price
import com.easypocket.mobile.domain.UnitOfMeasurement
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UndoRestoreTest {
    private lateinit var db: EasyPocketDatabase
    private lateinit var stores: StoreRepository
    private lateinit var products: ProductRepository
    private lateinit var lists: ShoppingListRepository
    private lateinit var categories: CategoryRepository
    private lateinit var history: PurchaseHistoryRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries().build()
        stores = StoreRepository(db, db.storeDao(), db.priceDao())
        products = ProductRepository(db, db.productDao(), db.priceDao())
        lists = ShoppingListRepository(db, db.listDao())
        categories = CategoryRepository(db, db.categoryDao(), db.listDao(), db.productLastCategoryDao())
        history = PurchaseHistoryRepository(
            db = db,
            historyDao = db.purchaseHistoryDao(),
            listDao = db.listDao(),
            productDao = db.productDao(),
            priceDao = db.priceDao(),
            storeDao = db.storeDao(),
        )
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `product delete and restore brings back product prices last category and list items`() = runTest {
        val store = stores.create("S1")
        val category = categories.create("Verduras")
        val product = products.create("Milk", UnitOfMeasurement.LT, listOf(Price(store.id, 10.0)))
        val list = lists.create("Weekly")
        val itemId = lists.addItem(list.id, product.id, store.id, 2.0)
        db.productLastCategoryDao().upsert(ProductLastCategoryEntity(product.id, category.id))

        val snapshot = products.deleteAll(listOf(product.id))!!

        assertTrue(products.getAll().isEmpty())
        assertEquals(0, lists.getById(list.id)!!.items.size)
        assertNull(db.productLastCategoryDao().getByProductId(product.id))

        products.restore(snapshot)

        val restored = products.getByName("Milk")!!
        assertEquals(product.id, restored.id)
        assertEquals(listOf(Price(store.id, 10.0)), restored.prices)
        assertEquals(category.id, db.productLastCategoryDao().getByProductId(product.id)?.categoryId)
        val item = db.listDao().getById(list.id)!!.items.single()
        assertEquals(itemId, item.id)
        assertEquals(product.id, item.productId)
    }

    @Test
    fun `store delete and restore brings back store prices and item store links`() = runTest {
        val store = stores.create("S1")
        val product = products.create("Milk", UnitOfMeasurement.LT, listOf(Price(store.id, 10.0)))
        val list = lists.create("Weekly")
        val itemId = lists.addItem(list.id, product.id, store.id, 2.0)

        val snapshot = stores.deleteAll(listOf(store.id))!!

        assertTrue(stores.getAll().isEmpty())
        assertNull(db.listDao().getById(list.id)!!.items.single().storeId)

        stores.restore(snapshot)

        assertEquals(store.id, db.listDao().getById(list.id)!!.items.single { it.id == itemId }.storeId)
        val prices = db.priceDao().getAll().first().filter { it.productId == product.id }
        assertEquals(1, prices.size)
        assertEquals(store.id, prices.first().storeId)
    }

    @Test
    fun `category delete and restore brings back category and item list and last category links`() = runTest {
        val category = categories.create("Verduras")
        val product = products.create("Milk", UnitOfMeasurement.LT)
        val list = lists.create("Weekly")
        lists.setCategory(list.id, category.id)
        val itemId = lists.addItem(list.id, product.id, null, 2.0)
        db.listDao().setItemsCategoryByIds(listOf(itemId), category.id)
        db.productLastCategoryDao().upsert(ProductLastCategoryEntity(product.id, category.id))

        val snapshot = categories.deleteAll(listOf(category.id))!!

        assertTrue(categories.getAll().isEmpty())
        assertNull(db.listDao().getById(list.id)!!.items.single { it.id == itemId }.categoryId)
        assertNull(db.listDao().getById(list.id)?.list?.categoryId)

        categories.restore(snapshot)

        assertTrue(categories.getAll().isNotEmpty())
        assertEquals(category.id, db.listDao().getById(list.id)!!.items.single { it.id == itemId }.categoryId)
        assertEquals(category.id, db.productLastCategoryDao().getByProductId(product.id)?.categoryId)
    }

    @Test
    fun `list delete and restore brings back list and items with the same ids`() = runTest {
        val product = products.create("Milk", UnitOfMeasurement.LT)
        val list = lists.create("Weekly")
        val itemA = lists.addItem(list.id, product.id, null, 1.0)
        val itemB = lists.addItem(list.id, product.id, null, 2.0)

        val snapshot = lists.delete(list.id)!!

        assertTrue(lists.getAll().isEmpty())

        lists.restoreList(snapshot)

        val restored = lists.getById(list.id)!!
        assertEquals("Weekly", restored.title)
        val items = db.listDao().getById(list.id)!!.items.sortedBy { it.id }
        assertEquals(listOf(itemA, itemB), items.map { it.id })
        assertTrue(items.all { it.productId == product.id })
    }

    @Test
    fun `history delete and restore brings back record items and category totals`() = runTest {
        db.purchaseHistoryDao().insertHistory(
            com.easypocket.mobile.data.local.PurchaseHistoryEntity("h1", "Lista", "$", 0L, 20.0, 1)
        )
        db.purchaseHistoryDao().insertItems(
            listOf(PurchaseHistoryItemEntity(historyId = "h1", productName = "Milk", storeName = null, quantity = 2.0, unitPrice = 10.0, totalPrice = 20.0, itemUid = "u1"))
        )
        db.purchaseHistoryDao().insertCategoryTotals(
            listOf(PurchaseHistoryCategoryTotalEntity(historyId = "h1", categoryCode = null, total = 20.0))
        )

        val snapshot = history.delete("h1")!!
        assertTrue(db.purchaseHistoryDao().observeAll().first().isEmpty())

        history.restore(snapshot)

        val restored = db.purchaseHistoryDao().observeAll().first().single()
        assertEquals("h1", restored.record.id)
        assertEquals(1, restored.items.size)
        assertEquals("Milk", restored.items.first().productName)
        assertEquals(1, restored.categoryTotals.size)
        assertEquals(20.0, restored.categoryTotals.first().total, 0.001)
    }
}
