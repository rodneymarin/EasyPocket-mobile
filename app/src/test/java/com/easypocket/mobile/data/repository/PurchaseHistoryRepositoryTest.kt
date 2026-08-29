package com.easypocket.mobile.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.easypocket.mobile.data.local.EasyPocketDatabase
import com.easypocket.mobile.data.local.PriceEntity
import com.easypocket.mobile.data.local.ProductEntity
import com.easypocket.mobile.data.local.ShoppingListEntity
import com.easypocket.mobile.data.local.ShoppingListItemEntity
import com.easypocket.mobile.data.local.StoreEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PurchaseHistoryRepositoryTest {
    private lateinit var db: EasyPocketDatabase
    private lateinit var repo: PurchaseHistoryRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = PurchaseHistoryRepository(
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
    fun `archiveCompleted snapshots done items and keeps pending`() = runTest {
        db.storeDao().insert(StoreEntity("s1", "Store 1", 0))
        db.productDao().insert(ProductEntity("p1", "Milk", "u"))
        db.productDao().insert(ProductEntity("p2", "Bread", "u"))
        db.priceDao().insertAll(listOf(PriceEntity("p1", "s1", 10.0)))
        db.listDao().insert(ShoppingListEntity("l1", "Weekly", "🛒"))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = "s1", quantity = 2.0, done = true))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p2", storeId = null, quantity = 1.0, done = true))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 3.0, done = false))

        repo.archiveCompleted("l1", date = 1756400000000L)

        val records = repo.observeAll().first()
        assertEquals(1, records.size)
        val record = records.first()
        assertEquals("Weekly", record.record.listTitle)
        assertEquals("🛒", record.record.listIcon)
        assertEquals(1756400000000L, record.record.date)
        assertEquals(2, record.record.itemCount)
        assertEquals(20.0, record.record.totalAmount, 0.001)
        val byName = record.items.associateBy { it.productName }
        val milk = byName.getValue("Milk")
        assertEquals(10.0, milk.unitPrice, 0.001)
        assertEquals(20.0, milk.totalPrice, 0.001)
        assertEquals("Store 1", milk.storeName)
        val noStore = byName.getValue("Bread")
        assertEquals(0.0, noStore.unitPrice, 0.001)
        assertEquals(null, noStore.storeName)

        val remaining = db.listDao().getById("l1")!!.items
        assertEquals(1, remaining.size)
        assertTrue(!remaining.first().done)
    }

    @Test
    fun `archiveCompleted records the category code of each item`() = runTest {
        db.productDao().insert(ProductEntity("p1", "Milk", "lt", categoryId = "ABC123"))
        db.productDao().insert(ProductEntity("p2", "Bread", "u", categoryId = null))
        db.listDao().insert(ShoppingListEntity("l1", "Weekly", "🛒"))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 1.0, done = true))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p2", storeId = null, quantity = 1.0, done = true))

        repo.archiveCompleted("l1")

        val items = repo.observeAll().first().first().items.associateBy { it.productName }
        assertEquals("ABC123", items.getValue("Milk").categoryCode)
        assertEquals(null, items.getValue("Bread").categoryCode)
    }

    @Test
    fun `archiveCompleted without done items does nothing`() = runTest {
        db.productDao().insert(ProductEntity("p1", "Milk", "u"))
        db.listDao().insert(ShoppingListEntity("l1", "Weekly", "$"))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 1.0, done = false))

        repo.archiveCompleted("l1")

        assertTrue(repo.observeAll().first().isEmpty())
        assertEquals(1, db.listDao().getById("l1")!!.items.size)
    }
}
