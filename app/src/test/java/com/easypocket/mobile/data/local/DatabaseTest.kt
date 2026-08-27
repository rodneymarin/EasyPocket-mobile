package com.easypocket.mobile.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
class DatabaseTest {
    private lateinit var db: EasyPocketDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EasyPocketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `delete store cascades prices and nulls item storeId`() = runTest {
        db.storeDao().insert(StoreEntity("s1", "Store 1", 0))
        db.productDao().insert(ProductEntity("p1", "Milk", "lt"))
        db.priceDao().insertAll(listOf(PriceEntity("p1", "s1", 10.0)))
        db.listDao().insert(ShoppingListEntity("l1", "Lista"))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = "s1", quantity = 1.0))

        db.storeDao().deleteByIds(listOf("s1"))

        assertTrue(db.priceDao().getAll().first().isEmpty())
        val item = db.listDao().getById("l1")!!.items.first()
        assertNull(item.storeId)
    }

    @Test
    fun `delete product cascades prices and items`() = runTest {
        db.storeDao().insert(StoreEntity("s1", "Store 1", 0))
        db.productDao().insert(ProductEntity("p1", "Milk", "lt"))
        db.priceDao().insertAll(listOf(PriceEntity("p1", "s1", 10.0)))
        db.listDao().insert(ShoppingListEntity("l1", "Lista"))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 1.0))

        db.productDao().deleteByIds(listOf("p1"))

        assertTrue(db.priceDao().getAll().first().isEmpty())
        assertTrue(db.listDao().getById("l1")!!.items.isEmpty())
    }

    @Test
    fun `delete list cascades items`() = runTest {
        db.productDao().insert(ProductEntity("p1", "Milk", "lt"))
        db.listDao().insert(ShoppingListEntity("l1", "Lista"))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 1.0))

        db.listDao().deleteById("l1")

        assertTrue(db.listDao().getAll().first().isEmpty())
    }

    @Test
    fun `getByName is case insensitive`() = runTest {
        db.productDao().insert(ProductEntity("p1", "Leche Entera", "lt"))
        assertEquals("p1", db.productDao().getByName("leche entera")!!.id)
        assertNull(db.productDao().getByName("leche"))
    }

    @Test
    fun `moveItems relocates items to another list`() = runTest {
        db.productDao().insert(ProductEntity("p1", "Milk", "lt"))
        db.listDao().insert(ShoppingListEntity("l1", "A"))
        db.listDao().insert(ShoppingListEntity("l2", "B"))
        val id = db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 1.0))

        db.listDao().moveItems(listOf(id), "l2")

        assertEquals(0, db.listDao().getById("l1")!!.items.size)
        assertEquals(1, db.listDao().getById("l2")!!.items.size)
    }

    @Test
    fun `removeCompleted deletes only done items`() = runTest {
        db.productDao().insert(ProductEntity("p1", "Milk", "lt"))
        db.listDao().insert(ShoppingListEntity("l1", "A"))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 1.0, done = true))
        db.listDao().insertItem(ShoppingListItemEntity(shoppingListId = "l1", productId = "p1", storeId = null, quantity = 2.0, done = false))

        db.listDao().removeCompleted("l1")

        assertEquals(1, db.listDao().getById("l1")!!.items.size)
    }
}
